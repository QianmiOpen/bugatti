package models.task

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.iteratee._
import play.api.libs.json.{JsObject, JsString, JsValue, Json}

import scala.concurrent.duration._
import utils.DateFormatter._

/**
 * Created by jinwei on 20/6/14.
 */
object TaskProcess {

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

  def generateStatusJson(envId: Int, projectId: Int, currentNum: Int, totalNum: Int, sls: String, status: Int, taskName: String): JsObject = {
    Json.obj(s"${envId}_${projectId}" -> Json.obj("currentNum" -> currentNum, "totalNum" -> totalNum, "sls" -> sls, "status" -> status, "taskName" -> taskName))
  }

  def generateTaskStatusJson(envId: Int, projectId: Int, task: JsValue, taskName: String): JsValue = {
    val status = statusMap \ s"${envId}_${projectId}"
    val result = Json.obj("currentNum" -> (status \ "currentNum").toString.toInt
      , "totalNum" -> (status \ "totalNum").toString.toInt
      , "sls" -> (status \ "sls").toString
      , "status" -> (task \ "status").toString.toInt
      , "endTime" -> (task \ "endTime").toString
      , "taskName" -> taskName
      , "task" -> task
    )
    changeAllStatus(Json.obj(s"${envId}_${projectId}" -> result))
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
    //发送到指定 actor mailbox
    executeTasks(tq.envId, tq.projectId)
    taskQueueId
  }

  //推送任务状态

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
        //3.3、依次执行命令(insert命令列表，依次执行，修改数据库状态，修改内存状态)；
        val params = TaskProcess.getAllParams
        val commandList: Seq[TaskCommand] = generateCommands(taskId, taskQueue.taskTemplateId, params)
        TaskCommandHelper.addCommands(commandList)
        //3.4、检查命令执行日志，判断是否继续；
        //3.5、更改statusMap状态 & 推送任务状态；
        if(executeCommand(commandList, envId, projectId, taskName)){
          //任务执行成功
          TaskHelper.changeStatus(taskId, 1)
        }
        else {
          //任务执行失败
          TaskHelper.changeStatus(taskId, 2)
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

  def executeCommand(commandList: Seq[TaskCommand], envId: Int, projectId: Int, taskName: String): Boolean = {
    val totalNum = commandList.size
    var result = true
    for(command <- commandList){
      //修改内存状态
      val currentNum = command.orderNum
      Logger.info("executeCommand currentNum==>" + currentNum)
      Logger.info("executeCommand totalNum==>" + totalNum)
      TaskProcess.changeAllStatus(TaskProcess.generateStatusJson(envId, projectId, currentNum, totalNum, command.command, 3, taskName))
      //修改数据库状态(task_command)
      TaskCommandHelper.updateStatusByOrder(command.taskId, command.orderNum, 3)
      //推送状态
      TaskProcess.pushStatus
      //调用salt命令
      //查看日志
      Thread.sleep(2000)
      //更新数据库状态
      TaskCommandHelper.updateStatusByOrder(command.taskId, command.orderNum, 1)
      //根据最后一次任务的状态判断整个任务是否成功
      result = true
    }
    Logger.info(result.toString)
    result
  }

  /**
   * 生成命令集合
   * @param taskId
   * @param taskTemplateId
   * @param jsValue
   * @return
   */
  def generateCommands(taskId: Int, taskTemplateId: Int, jsValue: JsValue): Seq[TaskCommand]={
    val templateCommands = TaskTemplateStepHelper.getStepsByTemplateId(taskTemplateId).map{ step =>
      //参数替换，更改sls命令
      fillSls(step, taskId, jsValue)
    }
    //获取机器，遍历机器&模板命令
    val machines: List[String] = List("t-minion1","t-minion2")
    var count = 0
    for{machine <- machines
      c <- templateCommands
    } yield {
      count += 1
      c.copy(command = s"${machine}:${c.command}").copy(orderNum = count)
    }
  }

  /**
   * 填写命令参数
   * @param sls
   * @param taskId
   * @param jsValue
   * @return
   */
  def fillSls(sls: TaskTemplateStep, taskId: Int, jsValue: JsValue): TaskCommand = {
    //sls.sls需要被填充
    TaskCommand(None, taskId, sls.sls, 0, sls.orderNum)
  }
}

/**
 * 处理所有的websocket
 */
//class SocketActor extends Actor {
//
//  val (out, channel) = Concurrent.broadcast[JsValue]
//
//  def receive = {
//    case JoinProcess(js) => {
//      sender ! ConnectedSocket(out)
//      notifyAllSocket(js)
//    }
//    case QuitProcess() => {
//      Logger.info("有一个客户端关闭了连接")
//    }
//    case AllTaskStatus() => {
//      notifyAllSocket(TaskProcess.getAllStatus)
//    }
//  }
//
//  def notifyAllSocket(js: JsValue) {
//    Logger.info(js.toString())
//    Thread.sleep(100)
//    channel.push(js)
//  }
//}
//
//case class JoinProcess(js: JsValue)
//
//case class AllTaskStatus()
//
//case class ConnectedSocket(out: Enumerator[JsValue])
//
//case class CannotConnect(msg: String)
//
//case class QuitProcess()

case class ExecuteOneByOne(envId: Int, projectId: Int)

case class ExecuteTasks(envId: Int, projectId: Int)

case class RemoveActor(envId: Int, projectId: Int)