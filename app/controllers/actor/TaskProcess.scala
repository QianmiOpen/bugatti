package controllers.actor

import java.io.{PrintWriter, File}

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import enums.TaskEnum
import models.conf._
import models.task._
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.iteratee._
import play.api.libs.json._
import utils.DateFormatter._
import utils.{GitHelp, TaskTools}

import scala.concurrent.duration._
import scala.sys.process._

/**
 * Created by jinwei on 20/6/14.
 */
object TaskProcess {

  implicit val taskQueueWrites = Json.writes[TaskQueue]

  implicit val timeout = Timeout(2 seconds)

  lazy val taskSystem = {
    ActorSystem("MyProcessSystem")
  }

  lazy val socketActor = {
//    ActorSystem("mySocketSystem") actorOf Props[SocketActor]
    taskSystem actorOf Props[SocketActor]
//    Akka.system.actorOf(Props[SocketActor])
  }

  // 以envId:projectId作为key，区分任务执行actor
  var actorMap = {
    Map.empty[String, ActorRef]
  }

  //以envId:projectId作为key，区分任务状态
  var statusMap = Json.obj()

  def chooseTaskActor(envProject: String): ActorRef = {
    actorMap get envProject getOrElse{
      val actor = taskSystem actorOf Props[TaskProcess]
      actorMap += envProject -> actor
      actor
    }
  }

  /**
   * 获得所有actor的执行状态
   * @return
   */
  def getAllStatus: JsValue = {
    Logger.info(Json.prettyPrint(statusMap))
    statusMap
  }

  def generateStatusJson(envId: Int, projectId: Int, currentNum: Int, totalNum: Int, sls: String, status: Int, taskName: String) = {
    val key = s"${envId}_${projectId}"
    val json = Json.obj("currentNum" -> currentNum, "totalNum" -> totalNum, "sls" -> sls, "status" -> status, "taskName" -> taskName)
    generateJson(key, json)
  }

  def generateTaskStatusJson(envId: Int, projectId: Int, task: JsValue, taskName: String) = {
    val key = s"${envId}_${projectId}"
    generateJson(key, Json.obj("taskName" -> taskName, "task" -> task, "status" -> task \ "status", "endTime" -> (task \ "endTime").toString))
  }

  def changeAllStatus(js: JsObject): JsValue = {
    statusMap = statusMap ++ js
    Logger.info(statusMap.toString())
    statusMap
  }

  def removeStatus(key: String): JsValue = {
    statusMap = statusMap - key
    statusMap
  }

  def getAllParams: JsValue = {
    null
  }

  //新建任务,insert队列表
  def createNewTask(tq: TaskQueue): Int = {
    val taskQueueId = TaskQueueHelper.add(tq)
    //更新队列任务信息
    checkQueueNum(tq)
    //发送到指定 actor mailbox
    executeTasks(tq.envId, tq.projectId)
    taskQueueId
  }

  def copyConfFileAddToGit(environmentId: Int, appName: String, taskId: Int, versionId: Int) = {
    val project = ProjectHelper.findByName(appName)
    val projectId = project.get.id.get

    val confSeq = ConfHelper.findByEid_Pid_Vid(environmentId, projectId, versionId)
    if (confSeq.size > 0) {
      val baseDir = s"${GitHelp.workDir.getAbsolutePath}/work/${appName}/${taskId}/files"
      val file = new File(baseDir)
      file.mkdirs()
      confSeq.foreach { xf =>
        val confContent = ConfContentHelper.findById(xf.id.get)
        val newFile = new File(s"${baseDir}/${xf.path}")
        newFile.getParentFile().mkdirs()
        val io = new PrintWriter(newFile)
        io.write(confContent.get.content)
        io.close()
      }

      GitHelp.push(s"push ${appName} job, id is ${taskId}")
    }
  }

  def checkQueueNum(tq: TaskQueue) = {
    //1、获取队列中等待执行TaskWait的任务个数
    val waitNum = TaskQueueHelper.findQueueNum(tq)
    val list = TaskQueueHelper.findQueues(tq)
    Logger.info(s"waitNum ==> ${waitNum}")
    //2、更改任务状态
    generateQueueNumJson(tq, waitNum, list)
    //3、推送任务状态
    pushStatus()
  }

  def generateQueueNumJson(tq: TaskQueue, num: Int, list: List[TaskQueue]){
    val key = s"${tq.envId}_${tq.projectId}"
    generateJson(key, Json.obj("queueNum" -> num))
    generateJson(key, Json.obj("queues" -> Json.toJson(list)))
  }

  def generateJson(key: String, json: JsObject){
    val status = (statusMap \ key).asOpt[JsObject]
    if(status != None){
      val result = status.get ++ json
      changeAllStatus(Json.obj(key -> result))
      Logger.info(s"status ==> ${result.toString()}")
    }
    else{
      changeAllStatus(Json.obj(key -> json))
      Logger.info(s"status ==> ${json}")
    }
  }

  def join(): scala.concurrent.Future[(Iteratee[JsValue,_],Enumerator[JsValue])] = {
    val js: JsValue = getAllStatus
    (socketActor ? JoinProcess(js)).map{
      case ConnectedSocket(out) => {
        val in = Iteratee.foreach[JsValue]{ event =>
          //这个是为了client主动调用
          socketActor ! AllTaskStatus()
        }.map{ _ =>
          socketActor ! QuitProcess()
        }
        (in, out)
      }
      case CannotConnect(error) =>{
        val iteratee = Done[JsValue,Unit]((),Input.EOF)
        // Send an error and close the socket
        val enumerator =  Enumerator[JsValue](JsObject(Seq("error" -> JsString(error)))).andThen(Enumerator.enumInput(Input.EOF))

        (iteratee,enumerator)
      }
    }
  }

  /**
   * 推送任务状态
   */
  def pushStatus(){
    Logger.info("pushStatus")
    socketActor ! AllTaskStatus()
  }

  def executeTasks(envId: Int, projectId: Int) = {
    //2、新建一个actor用来调用命令，该actor不记录到actorMap
    val actor = taskSystem actorOf Props[TaskProcess]
    //3、actor开始执行任务流程
    actor ! ExecuteTasks(envId, projectId)
  }

  def removeActor(envId: Int, projectId: Int) {
    actorMap -= s"${envId}_${projectId}"
  }

}

/**
 * 一个任务一个actor
 */
class TaskProcess extends Actor {

  val baseLogPath = "/Users/jinwei/bugatti/saltlogs"

  implicit val taskWrites = Json.writes[Task]

  def receive = {
    case ExecuteOneByOne(envId, projectId) => {
      //3.1、队列表中获取最先执行的任务；
      var taskQueue = TaskQueueHelper.findExecuteTask(envId, projectId)
      var taskName = ""
      while(taskQueue != null){
        taskName = TaskTemplateHelper.getById(taskQueue.taskTemplateId).name
        //3.2、insert到任务表 & 命令表；
        val taskId = TaskHelper.addByTaskQueue(taskQueue)
        //更新队列任务信息
        TaskProcess.checkQueueNum(taskQueue)
        //3.3、依次执行命令(insert命令列表，依次执行，修改数据库状态，修改内存状态)；
        val params = TaskProcess.getAllParams
        val commandList: Seq[TaskCommand] = generateCommands(taskId, taskQueue, params)
        Logger.info(commandList.toString)
        TaskCommandHelper.addCommands(commandList)
        //3.4、检查命令执行日志，判断是否继续；
        //3.5、更改statusMap状态 & 推送任务状态；
        if(executeCommand(commandList, envId, projectId, taskId, taskName)){
          //任务执行成功
          TaskHelper.changeStatus(taskId, enums.TaskEnum.TaskSuccess)
        }
        else {
          //任务执行失败
          TaskHelper.changeStatus(taskId, enums.TaskEnum.TaskFailed)
        }
        //删除队列taskQueue相应记录
        TaskQueueHelper.remove(taskQueue)
        //3.6、返回到3.1执行；
        taskQueue = TaskQueueHelper.findExecuteTask(envId, projectId)
      }
      // 推送任务状态
      val taskStatus: JsValue = Json.toJson(TaskHelper.findLastStatusByProject(envId, projectId)(0))
      Logger.info(s"JsValue ==> ${taskStatus}")
      TaskProcess.generateTaskStatusJson(envId, projectId, taskStatus, taskName)
      TaskProcess.pushStatus
      //no task no actor
      self ! RemoveActor(envId, projectId)
    }
    case ExecuteTasks(envId, projectId) => {
      val actor = TaskProcess.chooseTaskActor(s"${envId}_${projectId}")
      actor ! ExecuteOneByOne(envId, projectId)
    }
    case RemoveActor(envId, projectId) => {
      TaskProcess.removeActor(envId, projectId)
    }
  }

  def executeCommand(commandList: Seq[TaskCommand], envId: Int, projectId: Int, taskId: Int, taskName: String): Boolean = {
    val totalNum = commandList.size
    var result = true
    val baseDir = s"${baseLogPath}/${taskId}"
    val path = s"${baseLogPath}/${taskId}/execute.log"
    val resultLogPath = s"${baseLogPath}/${taskId}/result.log"
    val logDir = new File(baseDir)
    if(!logDir.exists){
      logDir.mkdirs()
    }

    for(command <- commandList){
      //修改内存状态
      val currentNum = command.orderNum
      TaskProcess.generateStatusJson(envId, projectId, currentNum, totalNum, command.command, 3, taskName)
      //修改数据库状态(task_command)
      TaskCommandHelper.updateStatusByOrder(command.taskId, command.orderNum, TaskEnum.TaskProcess)
      //推送状态
      TaskProcess.pushStatus
      //调用salt命令
      val cmd = command.command + s" -v --out-file=${path}"
      Logger.info(cmd)
      (cmd !!)
//      """salt t-minion state.sls webapp.deploy pillar={webapp:{"groupId":"com.ofpay","artifactId":"cardserverimpl","version":"1.6.3-RELEASE","repository":"releases"}} -v --out-file=/Users/jinwei/bugatti/saltlogs/install.log""" !!

      //合并日志
      val dir = new File(baseDir)
      val file = new File(resultLogPath)
      mergeLog(path, file, cmd, false)

      //查看日志 失败的命令再次执行一次
      if(!checkLog(path)){
        (cmd !!)
        //合并日志
        mergeLog(path, file, cmd, true)
        if(!checkLog(path)){
          result = false
        }
      }
      //更新数据库状态
      if(result){
        TaskCommandHelper.updateStatusByOrder(command.taskId, command.orderNum, TaskEnum.TaskSuccess)
      } else {
        TaskCommandHelper.updateStatusByOrder(command.taskId, command.orderNum, TaskEnum.TaskFailed)
        return result
      }
      //根据最后一次任务的状态判断整个任务是否成功
      result = true
    }
    Logger.info(result.toString)
    result
  }

  def mergeLog(path: String, file: File, cmd: String, again: Boolean) = {
    var executeAgain = ""
    if(again){
      executeAgain = "[execute again] "
    }

    Seq("echo", "=====================================华丽分割线=====================================") #>> file lines

    Seq("echo", s"${executeAgain} command: ${cmd}\n") #>> file lines

    Seq("cat", path) #>> file lines
  }

  def checkLog(path: String): Boolean = {
    var result = true
    val row = (s"tail -n3 ${path}" !!).split("\n")(0)
    if(row.split(":").length>1){
      val failedNum = row.split(":")(1).trim().toInt
      if(failedNum == 0){
        result = true
      }else {
        result = false
      }
    }else {
      result = false
    }
    result
  }

  /**
   * 生成命令集合
   * @param taskId
   * @param taskQueue
   * @param jsValue
   * @return
   */
  def generateCommands(taskId: Int, taskQueue: TaskQueue, jsValue: JsValue): Seq[TaskCommand]={
    //1、envId , projectId -> machines, nfsServer
    val machines: List[String] = List("t-minion")
    val nfsServer = EnvironmentHelper.findById(taskQueue.envId).get.nfServer

    //2、projectId -> groupId, artifactId
    val groupId = AttributeHelper.getValue(taskQueue.projectId, "groupId")
    val artifactId = AttributeHelper.getValue(taskQueue.projectId, "artifactId")

    //3、version -> version, repository
    val version = taskQueue.version
    var repository = "releases"
    if(TaskTools.isSnapshot(version)){
      repository = "snapshots"
    }

    val paramsJson = Json.obj(
      "nfsServer" -> nfsServer
      ,"groupId" -> groupId
      ,"artifactId" -> artifactId
      ,"version" -> version
      ,"repository" -> repository
    )


    val templateCommands = TaskTemplateStepHelper.getStepsByTemplateId(taskQueue.taskTemplateId).map{ step =>
      //参数替换，更改sls命令
      fillSls(step, taskId, paramsJson)
    }

    //获取机器，遍历机器&模板命令
    var count = 0
    for{machine <- machines
      c <- templateCommands
    } yield {
      count += 1
      val command = c.command.replaceAll("\\{\\{machine\\}\\}", machine)
      c.copy(command = command).copy(orderNum = count).copy(machine = s"${machine}")
    }
  }

  /**
   * 填写命令参数
   * @param sls
   * @param taskId
   * @param paramsJson
   * @return
   */
  def fillSls(sls: TaskTemplateStep, taskId: Int, paramsJson: JsValue): TaskCommand = {
    val keys: Set[String] = paramsJson match{
      case JsObject(fields) =>{
        fields.toMap.keySet
      }
      case _ =>{
        Set.empty[String]
      }
    }
    //sls.sls需要被填充
    TaskCommand(None, taskId, replaceSls(sls, paramsJson, keys), "machine", "sls", TaskEnum.TaskWait, sls.orderNum)
  }

  def replaceSls(sls: TaskTemplateStep, paramsJson: JsValue, keys: Set[String]): String = {
    var result = sls.sls
    keys.map{
      key =>
        result = result.replaceAll("\\{\\{" + key + "\\}\\}", (paramsJson \ key).toString)
    }
    result
  }
}


case class ExecuteOneByOne(envId: Int, projectId: Int)

case class ExecuteTasks(envId: Int, projectId: Int)

case class RemoveActor(envId: Int, projectId: Int)