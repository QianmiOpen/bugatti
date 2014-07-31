package controllers.actor

import java.io.{File, FileWriter, PrintWriter}

import actor.ActorUtils
import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import enums.TaskEnum
import models.conf._
import models.task._
import play.api.Logger
import play.api.Play.current
import play.api.libs.concurrent.Akka
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.iteratee._
import play.api.libs.json._
import utils.DateFormatter._
import utils.{ConfHelp, TaskTools}

import scala.concurrent.duration._
import scala.sys.process._
import scalax.file.Path

/**
 * Created by jinwei on 20/6/14.
 */
object TaskProcess {

  implicit val taskQueueWrites = Json.writes[TaskQueue]

  implicit val timeout = Timeout(2 seconds)

  lazy val taskSystem = {
//    ActorSystem("MyProcessSystem")
    ActorUtils.system
  }

  lazy val socketActor = {
//        ActorSystem("mySocketSystem") actorOf Props[SocketActor]
    taskSystem actorOf Props[SocketActor]
    //    Akka.system.actorOf(Props[SocketActor])
  }

  def generateSchedule = {
    new WSSchedule().start(socketActor, TaskProcess.getAllStatus)
  }


  // 以envId:projectId作为key，区分任务执行actor
  var actorMap = {
    Map.empty[String, ActorRef]
  }

  //以envId:projectId作为key，区分任务状态
  var statusMap = Json.obj()

  def chooseTaskActor(envProject: String): ActorRef = {
    actorMap get envProject getOrElse {
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
    Logger.debug(s"TaskProcess WSSchedule ==> ${Json.prettyPrint(statusMap)}")
//    statusMap.keys.map{
//      key =>
//        statusMap = statusMap ++ Json.obj(s"${key}_1" -> (statusMap \ key))
//    }
          statusMap

  }

  def generateStatusJson(envId: Int, projectId: Int, currentNum: Int, totalNum: Int, sls: String, machine: String, status: Int, taskName: String) = {
    val key = s"${envId}_${projectId}"
    val json = Json.obj("currentNum" -> currentNum, "totalNum" -> totalNum, "sls" -> sls, "machine" -> machine, "status" -> status, "taskName" -> taskName, "task" -> Json.obj())
    Logger.info("68 =>" + json)
    generateJson(key, json)
  }

  def generateTaskStatusJson(envId: Int, projectId: Int, task: JsValue, taskName: String) = {
    val key = s"${envId}_${projectId}"
    val version = VersionHelper.findById((task \ "versionId").asOpt[Int].getOrElse(0))
    var taskObj = task
    version match {
      case Some(v) => {
        taskObj = task.as[JsObject] ++ Json.obj("version" -> v.vs)
      }
      case _ =>
    }
    generateJson(key, Json.obj("taskName" -> taskName, "task" -> taskObj, "status" -> task \ "status", "endTime" -> (task \ "endTime").toString))
  }

  def changeAllStatus(js: JsObject): JsValue = {
    statusMap = statusMap ++ js
    Logger.info("changeAllStatus ==>"+statusMap.toString())
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
    val taskQueueId = TaskQueueHelper.create(tq)
    //更新队列任务信息
    checkQueueNum(tq.envId, tq.projectId)
    //发送到指定 actor mailbox
    executeTasks(tq.envId, tq.projectId)
    taskQueueId
  }

  def generateConfFile(environmentId: Int, appName: String, taskId: Int, versionId: Int, fileName: String) = {
    val project = ProjectHelper.findByName(appName)
    val projectId = project.get.id.get

    val confSeq = ConfHelper.findByEnvId_ProjectId_VersionId(environmentId, projectId, versionId)
    val baseDir = s"${ConfHelp.confPath}/${taskId}"

//    val baseDir = s"${GitHelp.workDir.getAbsolutePath}/work/${appName}/${taskId}/files"
//    val file = new File(s"${baseDir}/.bugatti")
//    file.getParentFile.mkdirs()
//    file.createNewFile()
    val file = new File(s"${baseDir}/files")
    if(!file.exists()) {
      file.mkdirs()
    }
    if (confSeq.size > 0) {
      confSeq.foreach { xf =>
        val confContent = ConfContentHelper.findById(xf.id.get)
        val newFile = new File(s"${baseDir}/files/${xf.path}")
        newFile.getParentFile().mkdirs()
        implicit val codec = scalax.io.Codec.UTF8
        val f = Path(newFile)
        f.write(confContent.get.content)
      }
    }

    Logger.info("baseDir ==>" + baseDir)
    Logger.info("fileName ==>"+fileName)

    val baseDirPath = new File(new File(baseDir).getAbsolutePath)
    Process(Seq("tar", "zcf", s"${fileName}.tar.gz", "files"), baseDirPath).!!

    Process(Seq("md5sum", s"${fileName}.tar.gz"), baseDirPath) #> new File(s"${baseDirPath}/${fileName}.tar.gz.md5") !

    Seq("rm", "-r", s"${baseDir}/files").!!

  }



  def getFileName() = {
    val timestamp: Long = System.currentTimeMillis / 1000
    s"${timestamp}"
  }

  def checkQueueNum(envId: Int, projectId: Int) = {
    //1、获取队列中等待执行TaskWait的任务个数
    val waitNum = TaskQueueHelper.findQueueNum(envId, projectId)
    val list = TaskQueueHelper.findQueues(envId, projectId)
    Logger.info(s"waitNum ==> ${waitNum}")
    //2、更改任务状态
    generateQueueNumJson(envId, projectId, waitNum, list)
    //3、推送任务状态
    //TODO pushStatus()
  }

  def generateQueueNumJson(envId: Int, projectId: Int, num: Int, list: List[TaskQueue]){
    val key = s"${envId}_${projectId}"
    Logger.info("149=>"+num)
    generateJson(key, Json.obj("queueNum" -> num))
    val listJson: List[JsObject] = list.map{
      x =>
        var json = Json.toJson(x)
        json = json.as[JsObject] ++ Json.obj("taskTemplateName" -> TaskTemplateHelper.findById(x.taskTemplateId).name)
        json.as[JsObject]
    }
    generateJson(key, Json.obj("queues" -> listJson))
  }

  def generateJson(key: String, json: JsObject){
    Logger.info("generateJson ==>"+json)
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
  def pushStatus() {
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

  def main(args: Array[String]) {
    val seq = Seq("salt", "t-syndic", "state.sls", "job.updategfs", "--out-file=target/saltlogs/5/execute.log")
    seq lines
  }

}

/**
 * 一个任务一个actor
 */
class TaskProcess extends Actor {

  val baseLogPath = ConfHelp.logPath

  implicit val taskWrites = Json.writes[Task]

  def receive = {
    case ExecuteOneByOne(envId, projectId) => {
      //3.1、队列表中获取最先执行的任务；
      var taskQueue = TaskQueueHelper.findExecuteTask(envId, projectId)
      var taskName = ""
      while(taskQueue != None){
        taskName = TaskTemplateHelper.findById(taskQueue.get.taskTemplateId).name
        //3.2、insert到任务表 & 命令表；
        val taskId = TaskHelper.addByTaskQueue(taskQueue.get)
        //更新队列任务信息
        TaskProcess.checkQueueNum(taskQueue.get.envId, taskQueue.get.projectId)
        //3.3、依次执行命令(insert命令列表，依次执行，修改数据库状态，修改内存状态)；
        val params = TaskProcess.getAllParams
        val (commandList, paramsJson) = generateCommands(taskId, taskQueue.get, params)
//        val commandList: Seq[TaskCommand] = generateCommands(taskId, taskQueue, params)
        TaskCommandHelper.create(commandList)
        //3.4、检查命令执行日志，判断是否继续；
        //3.5、更改statusMap状态 & 推送任务状态；
        if(executeCommand(commandList, envId, projectId, taskId, taskName, paramsJson)){
          //任务执行成功
          TaskHelper.changeStatus(taskId, enums.TaskEnum.TaskSuccess)
        }
        else {
          //任务执行失败
          TaskHelper.changeStatus(taskId, enums.TaskEnum.TaskFailed)
        }
        //删除队列taskQueue相应记录
        TaskQueueHelper.delete(taskQueue.get)
        //更新statusMap中的queues信息
        TaskProcess.checkQueueNum(taskQueue.get.envId, taskQueue.get.projectId)
        //3.6、返回到3.1执行；
        taskQueue = TaskQueueHelper.findExecuteTask(envId, projectId)
      }
      if(taskName != ""){
        // 推送任务状态
        val taskStatus: JsValue = Json.toJson(TaskHelper.findLastStatusByProject(envId, projectId)(0))
        Logger.info(s"JsValue ==> ${taskStatus}")
        TaskProcess.generateTaskStatusJson(envId, projectId, taskStatus, taskName)
        //TODO        TaskProcess.pushStatus
      }
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

  def executeCommand(commandList: Seq[TaskCommand], envId: Int, projectId: Int, taskId: Int, taskName: String, params: JsValue): Boolean = {
    var result = true
    val totalNum = commandList.size
    val baseDir = s"${baseLogPath}/${taskId}"
    val path = s"${baseLogPath}/${taskId}/execute.log"
    val resultLogPath = s"${baseLogPath}/${taskId}/result.log"
    val logDir = new File(baseDir)
    val file = new File(resultLogPath)
    if (!logDir.exists) {
      logDir.mkdirs()
    }
    //    (Seq("touch", s"${path}") lines)

    if (totalNum == 0) {
      (Seq("echo", "[ERROR] 项目没有绑定机器！") #>> file lines)
      result = false
      return result
    }

    for (command <- commandList) {
      //修改内存状态
      val currentNum = command.orderNum
      TaskProcess.generateStatusJson(envId, projectId, currentNum, totalNum, command.sls, command.machine, 3, taskName)
      //修改数据库状态(task_command)
      TaskCommandHelper.update(command.taskId, command.orderNum, TaskEnum.TaskProcess)
      //推送状态
      //TODO TaskProcess.pushStatus

      //调用salt命令
      val outputCommand = s"--log-file=${path}"
      val cmd = command.command
      //      Logger.info(cmd)
      val commandSeq = command2Seq(cmd)
      //如果是copy conf file，先上传git
      if (command.command.startsWith("bugatti")) {
        commandSeq(1) match {
          case "copyfile" => {
            Logger.info("envId===> " + envId.toString)
            Logger.info((params \ "projectName").as[String])
            Logger.info(taskId.toString)
            Logger.info((params \ "versionId").as[Int].toString)
            TaskProcess.generateConfFile(envId, (params \ "projectName").as[String], taskId, ((params \ "versionId").as[Int]), (params \ "confFileName").as[String])
          }
        }
      }
      else {
        doCommand(commandSeq, path)
        //合并日志
        mergeLog(path, file, s"${cmd} ${outputCommand}", false)

        //查看日志 失败的命令再次执行一次
//        if (!checkLog(path)) {
//          Thread.sleep(2000)
//          doCommand(commandSeq, path)
//
//          //合并日志
//          mergeLog(path, file, s"${cmd} ${outputCommand}", true)
//          if (!checkLog(path)) {
//            result = false
//          }
//        }
        //更新数据库状态
        if (result) {
          TaskCommandHelper.update(command.taskId, command.orderNum, TaskEnum.TaskSuccess)
        } else {
          TaskCommandHelper.update(command.taskId, command.orderNum, TaskEnum.TaskFailed)
          return result
        }
      }

      //根据最后一次任务的状态判断整个任务是否成功
      result = true
    }
    Logger.info(result.toString)
    result
  }

  def doCommand(command: Seq[String], path: String)  {
    var jid = doCommandRetJid(command)

    val wf = new FileWriter(new File(path))
    var bWait = true
    for (i <- 1 to 600 if bWait) {
      Thread.sleep(1000)
      val lines = checkJobStates(jid)
      if (lines.size > 0) {
        if (lines.last.contains("is running as")){
          val runningJid = lines.last.replaceAll("^.* with jid ", "")
          var bWaitRunning = true
          for (j <- 1 to 600 if bWaitRunning) {
            Thread.sleep(1000)
            val runningRet = checkJobStates(runningJid)
            if (runningRet.size > 0 && runningRet.last.contains("Total")) {
              bWaitRunning = false
              jid = doCommandRetJid(command)
            }
          }
        }else{
          bWait = false
          lines.foreach { x =>
            Logger.info(s"salt-run: ${x}")
            wf.write(s"${x}\n")
          }
        }
      }
    }
    wf.flush()
    wf.close()
  }

  def doCommandRetJid(command: Seq[String]): String = {
    val commandSeq = command :+ "--async"
    val ret = commandSeq.lines.mkString(",")
    val jid = ret.replaceAll("Executed command with job ID: ", "")
    Logger.info(s"Execute ${commandSeq.toString};jobId: ${jid}")

    jid
  }

  def checkJobStates(jid: String) = {
    val saltRun = Seq("salt-run", "jobs.lookup_jid", jid)
    Logger.info(s"Execute ${saltRun}")
    saltRun.lines
  }

  def command2Seq(command: String): Seq[String] = {
    var retSeq = Seq.empty[String]
    var bAppend = false
    command.split(" ").foreach { c =>
      if (bAppend) {
        retSeq = retSeq.dropRight(1) :+ (retSeq.last + s" $c")
      } else {
        retSeq = retSeq :+ c
      }
      if (c.contains("'")) {
        bAppend = !bAppend
      }
    }
    retSeq
  }

  def mergeLog(path: String, file: File, cmd: String, again: Boolean) = {
    var executeAgain = ""
    if (again) {
      executeAgain = "[execute again] "
    }
    (Seq("echo", "=====================================华丽分割线=====================================") #>> file lines)
    (Seq("echo", s"${executeAgain} command: ${cmd}\n") #>> file lines)

    val executeLog = new File(path)
    //为何这个文件会被莫名的删掉？
    if (executeLog.exists()) {
      (Seq("cat", path) #>> file lines)
    }
    else {
      (Seq("echo", s"${path}不存在！") #>> file lines)
    }
  }

  def checkLog(path: String): Boolean = {
    var result = true
    val executeLog = new File(path)
    //为何这个文件会被莫名的删掉？
    if (executeLog.exists()) {
      val row = (s"tail -n3 ${path}" !!).split("\n")(0)
      if (row.split(":").length > 1) {
        val failedNum = row.split(":")(1).trim().toInt
        if (failedNum == 0) {
          result = true
        } else {
          result = false
        }
      } else {
        result = false
      }
    }
    else {
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
  def generateCommands(taskId: Int, taskQueue: TaskQueue, jsValue: JsValue): (Seq[TaskCommand], JsObject) = {
    //1、envId , projectId -> machines, nfsServer
    //    val machines: List[String] = List("t-minion")
    val seqMachines = EnvironmentProjectRelHelper.findByEnvId_ProjectId(taskQueue.envId, taskQueue.projectId)
    if (seqMachines.length == 0) {
      return (Seq.empty[TaskCommand], null)
    }
    val nfsServer = EnvironmentHelper.findById(taskQueue.envId).get.nfServer

    //2、projectId -> groupId, artifactId, projectName
    val projectName = ProjectHelper.findById(taskQueue.projectId).get.name

    //3、version -> version, repository
    val versionId = taskQueue.versionId
    var repository = "releases"
    if (TaskTools.isSnapshot(versionId.getOrElse(0))) {
      repository = "snapshots"
    }

    var versionName = ""
    versionId match {
      case Some(vid) => {
        versionName = VersionHelper.findById(vid).get.vs
      }
      case _ =>
    }

    val fileName = TaskProcess.getFileName()


    var paramsJson = Json.obj(
      "nfsServer" -> nfsServer
      , "version" -> versionName
      , "versionId" -> versionId.getOrElse[Int](0)
      , "repository" -> repository
      , "projectName" -> projectName
      , "envId" -> taskQueue.envId
      , "projectId" -> taskQueue.projectId
      , "taskId" -> taskId
      , "confFileName" -> fileName
    )

    //projectId -> groupId, artifactId, unpacked
    val attributesJson = AttributeHelper.findByProjectId(taskQueue.projectId).map {
      s =>
        paramsJson = paramsJson ++ Json.obj(s.name -> s.value)
    }

    val templateCommands = TaskTemplateStepHelper.findStepsByTemplateId(taskQueue.taskTemplateId).map { step =>
      //参数替换，更改sls命令
      fillSls(step, taskId, paramsJson)
    }

    //获取机器，遍历机器&模板命令
    var count = 0
    val seq = for {machine <- seqMachines
                   c <- templateCommands
    } yield {
      count += 1
      val command = c.command.replaceAll("\\{\\{machine\\}\\}", machine.name).replaceAll("\\{\\{syndic\\}\\}", machine.syndicName)
      c.copy(command = command).copy(orderNum = count).copy(machine = s"${machine.name}")
    }
    (seq, paramsJson)
  }

  /**
   * 填写命令参数
   * @param sls
   * @param taskId
   * @param paramsJson
   * @return
   */
  def fillSls(sls: TaskTemplateStep, taskId: Int, paramsJson: JsValue): TaskCommand = {
    val keys: Set[String] = paramsJson match {
      case JsObject(fields) => {
        fields.toMap.keySet
      }
      case _ => {
        Set.empty[String]
      }
    }
    //sls.sls需要被填充
    TaskCommand(None, taskId, replaceSls(sls, paramsJson, keys), "machine", sls.name, TaskEnum.TaskWait, sls.orderNum)
  }

  def replaceSls(sls: TaskTemplateStep, paramsJson: JsValue, keys: Set[String]): String = {
    var result = sls.sls
    keys.map {
      key =>
        result = result.replaceAll("\\{\\{" + key + "\\}\\}", TaskTools.trimQuotes((paramsJson \ key).toString))
    }
    result
  }
}

class WSSchedule{
  def start(socketActor: ActorRef, jsValue: JsValue): Cancellable = {
    Akka.system.scheduler.schedule(
      1 second,
      1 seconds,
      socketActor,
      jsValue
    )
  }
}

case class ExecuteOneByOne(envId: Int, projectId: Int)

case class ExecuteTasks(envId: Int, projectId: Int)

case class RemoveActor(envId: Int, projectId: Int)