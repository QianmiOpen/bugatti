package actor.task

import actor.ActorUtils
import akka.actor.SupervisorStrategy.Escalate
import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import enums.TaskEnum
import enums.TaskEnum.TaskStatus
import models.conf.{AreaHelper, EnvironmentProjectRelHelper}
import models.task.{TaskTemplateHelper, TaskCommand, TaskQueueHelper, TaskQueue}
import org.joda.time.DateTime
import play.api.Logger
import play.api.libs.concurrent.Akka
import play.api.libs.iteratee.{Input, Done, Enumerator, Iteratee}
import play.api.libs.json.{JsString, JsObject, Json, JsValue}
import scala.collection.mutable
import scala.concurrent.duration._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.Play.current

/**
 * Created by jinwei on 13/7/14.
 */
object MyActor {

  implicit val timeout = Timeout(2 seconds)

  final val projectKey = "ALL"

  val system = ActorUtils.system
  //管理taskQueue中，在同一时间只有一个eid_pid的任务在执行
  val superviseTaskActor = system.actorOf(Props[MyActor], "superviseActor")
  //socketActor
  val socketActor = system.actorOf(Props[SocketActor], "socketActor")

  //容器：管理eid_pid状态
  var envId_projectIdStatus = Map.empty[String, Seq[String]]

  var statusMap = Json.obj()

  /**
   * 新建一个任务需要到actor的队列中处理
   */
  def createNewTask(envId: Int, projectId: Int, clusterName: Option[String]) = {
    superviseTaskActor ! CreateNewTaskActor(envId, projectId, clusterName)
  }

  def join(): scala.concurrent.Future[(Iteratee[JsValue,_],Enumerator[JsValue])] = {

    val js: JsValue = MyActor.statusMap
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

  def forceTerminate(envId: Int, projectId: Int, clusterName: Option[String]): Unit ={
    superviseTaskActor ! ForceTerminate(envId, projectId, clusterName)
  }

  //在global被初始化
  def generateSchedule() = {
    new WSSchedule().start(socketActor, "notify")
  }

}

class MyActor extends Actor with ActorLogging {

  implicit val taskQueueWrites = Json.writes[TaskQueue]

  override val supervisorStrategy = OneForOneStrategy() {
    case e: Exception =>
      log.error(s"${self} catch ${sender} exception: ${e.getStackTrace}")
      Escalate
  }

  import context._
  def receive = {
    case CreateNewTaskActor(envId, projectId, cluster) => {
      // 增加任务队列数量
      val key = taskKey(envId, projectId, cluster)
      incQueueNum(key, 1)
      self ! NextTaskQueue(envId, projectId, cluster)
    }
    case NextTaskQueue(envId, projectId, cluster) => {
      val key = s"${envId}_${projectId}"
      val tKey = taskKey(envId, projectId, cluster)
      val clusterName = genClusterName(cluster)
      if(isTaskAvailable(key, clusterName)){
        addKeyStatus(key, clusterName)
        context.child(s"taskExecute_${tKey}").getOrElse(
          actorOf(Props[TaskExecute], s"taskExecute_${tKey}")
        ) ! NextTaskQueue(envId, projectId, cluster)
      }else {
        self ! ChangeQueues(envId, projectId, cluster)
      }
    }

    case cq: ChangeQueues => {
      val tKey = taskKey(cq.envId, cq.projectId, cq.clusterName)
      //修改队列
      val queues = TaskQueueHelper.findQueues(cq.envId, cq.projectId, cq.clusterName)
      val _queuesJson = queues.map{
        x =>
          var json = Json.toJson(x)
          //增加模板名称
          json = json.as[JsObject] ++ Json.obj("taskTemplateName" -> TaskTemplateHelper.findById(x.taskTemplateId).name)
          json.as[JsObject]
      }
      changeStatus(mergerStatus(tKey, Json.obj("queues" -> _queuesJson)))
      val size = _queuesJson.size
      changeStatus(mergerStatus(tKey, Json.obj("queueNum" -> (if(size - 1 > 0){size - 1}else{0}) )))
    }

    case ChangeTaskStatus(tq, taskName, queuesJson, currentNum, totalNum, cluster) => {
//      val key = s"${tq.envId}_${tq.projectId}"
      val key = taskKey(tq.envId, tq.projectId, cluster)
      //1、修改queueNum
      changeStatus(mergerStatus(key, Json.obj("queueNum" -> queuesJson.size)))
      incQueueNum(key, -1)
      //2、更新queueList
      changeStatus(mergerStatus(key, Json.obj("queues" -> queuesJson)))
      //3、task状态 -> 正在执行
      changeStatus(mergerStatus(key, Json.obj("status" -> Json.toJson(TaskEnum.TaskProcess))))
      //4、totalNum
      changeStatus(mergerStatus(key, Json.obj("totalNum" -> totalNum)))
      //5、currentNum
      changeStatus(mergerStatus(key, Json.obj("currentNum" -> currentNum)))
      //6、taskName
      changeStatus(mergerStatus(key, Json.obj("taskName" -> taskName)))
    }
    case ChangeCommandStatus(envId, projectId, order, sls, machine, cluster) => {
//      val key = s"${envId}_${projectId}"
      val key = taskKey(envId, projectId, cluster)
      //1、currentNum
      changeStatus(mergerStatus(key, Json.obj("currentNum" -> order)))
      //2、command.commandName
      //3、comamnd.machine
      val json = Json.obj("command" -> Json.obj("sls" -> sls, "machine" -> machine))
      changeStatus(mergerStatus(key, json))
    }
    case ChangeOverStatus(envId, projectId, status, endTime, version, cluster) => {
//      val key = s"${envId}_${projectId}"
      val key = taskKey(envId, projectId, cluster)
      //1、taskStatus
      changeStatus(mergerStatus(key, Json.obj("taskStatus" -> Json.toJson(status))))
      //2、endTime
      changeStatus(mergerStatus(key, Json.obj("endTime" -> Json.toJson(endTime))))
      //3、version
      changeStatus(mergerStatus(key, Json.obj("version" -> version)))

      // 删除所有queues的状态
      changeStatus(mergerStatus(key, Json.obj("queues" -> Seq.empty[JsObject])))

      //TODO NextTaskQueue
      //复用taskExecute
      //      val taskExecute = actorSelection(s"/user/mySystem/superviseActor/taskExecute_${key}")
//      val taskExecute = actorSelection(s"/user/superviseActor/taskExecute_${key}")
      //      val taskExecute = actorOf(Props[TaskExecute], s"taskExecute_${key}")

      context.child(s"taskExecute_${key}").getOrElse(
        actorOf(Props[TaskExecute], s"taskExecute_${key}")
      ) ! RemoveTaskQueue(envId, projectId, cluster)

    }

    case ForceTerminate(envId, projectId, clusterName) => {
//      val key = s"${envId}_${projectId}"
      val key = taskKey(envId, projectId, clusterName)
      context.child(s"taskExecute_${key}").getOrElse(
        actorOf(Props[TaskExecute], s"taskExecute_${key}")
      ) ! TerminateCommands(TaskEnum.TaskFailed, envId, projectId, clusterName)
    }

    case RemoveStatus(envId, projectId, cluster) => {
      removeStatus(envId, projectId, cluster)
    }
  }

  def mergerStatus(key: String, js: JsObject): JsObject = {
    Json.obj(key -> (MyActor.statusMap \ key).as[JsObject].deepMerge(js))
  }

  def incQueueNum(key: String, num: Int) = {
    (MyActor.statusMap \ key).asOpt[JsObject] match {
      case Some(m) => {
        val queueNum = (m \ "queueNum").as[Int] + num
        changeStatus(Json.obj(key -> m.deepMerge(Json.obj("queueNum" -> queueNum))))
      }
      case _ => {
        changeStatus(Json.obj(key -> Json.obj("queueNum" -> 1)))
      }
    }
  }

  def changeStatus(js: JsObject): JsValue = {
    MyActor.statusMap = MyActor.statusMap ++ js
    log.info("changeStatus ==>"+MyActor.statusMap.toString())
    MyActor.statusMap
  }

  def removeStatus(envId: Int, projectId: Int, clusterName: Option[String]) = {
    val key = s"${envId}_${projectId}"
    val tKey = taskKey(envId, projectId, clusterName)
    val cName = genClusterName(clusterName)
    val queueNum = (MyActor.statusMap \ tKey \ "queueNum").asOpt[Int]

    queueNum match {
      case Some(qm) => {
        if(qm == 0){
          MyActor.statusMap = MyActor.statusMap - tKey
          MyActor.socketActor ! FindLastStatus(tKey)
        }
        else {
          MyActor.statusMap = MyActor.statusMap - tKey
          changeStatus(Json.obj(tKey -> Json.obj("queueNum" -> queueNum)))
        }
//        MyActor.envId_projectIdStatus = MyActor.envId_projectIdStatus - key
        removeKeyStatus(key, cName)
        self ! NextTaskQueue(envId, projectId, clusterName)
      }
      case _ => {
        MyActor.statusMap = MyActor.statusMap - tKey
//        MyActor.envId_projectIdStatus = MyActor.envId_projectIdStatus - key
        removeKeyStatus(key, cName)
      }
    }
  }

  /**
   * 判断同一个项目下的任务是否可以触发执行
   * @param key
   * @param name
   * @return
   */
  def isTaskAvailable(key: String, name: String): Boolean ={
    MyActor.envId_projectIdStatus.get(key) match {
      case Some(seq) =>
        if(seq.contains(MyActor.projectKey) || name == MyActor.projectKey){
          false
        }else {
          !seq.contains(name)
        }
      case _ =>
        true
    }
  }

  /**
   * 记录该项目中正在执行的任务key
   * @param key
   * @param name
   */
  def addKeyStatus(key: String, name: String): Unit ={
    MyActor.envId_projectIdStatus.get(key) match {
      case Some(seq) =>
        MyActor.envId_projectIdStatus += key -> (seq :+ name)
      case _ =>
        MyActor.envId_projectIdStatus += key -> Seq(name)
    }
  }

  /**
   * 删除该项目中已经执行完成的key
   * @param key
   * @param name
   */
  def removeKeyStatus(key: String, name: String): Unit ={
    MyActor.envId_projectIdStatus.get(key) match {
      case Some(seq) =>
        MyActor.envId_projectIdStatus += key -> (seq.filterNot(_ == name))
      case _ =>
    }
  }

  /**
   * 生成正确的taskKey（区分项目和机器级别的key）
   * @param envId
   * @param projectId
   * @param cluster
   * @return
   */
  def taskKey(envId: Int, projectId: Int, cluster: Option[String]): String ={
    cluster match{
      case Some(c) =>
        s"${envId}_${projectId}_${c}"
      case _ =>
        s"${envId}_${projectId}"
    }
  }

  /**
   * 转换机器负载的名称，如果是项目级别，则返回projectKey
   * @param cluster
   * @return
   */
  def genClusterName(cluster: Option[String]): String ={
    cluster match {
      case Some(s) => s
      case _ => MyActor.projectKey
    }
  }


}

case class CreateNewTaskActor(envId: Int, projectId: Int, clusterName: Option[String])
case class NextTaskQueue(envId: Int, projectId: Int, clusterName: Option[String])
case class ChangeQueues(envId: Int, projectId: Int, clusterName: Option[String])

case class ChangeTaskStatus(taskQueue: TaskQueue, taskName: String, queues: Seq[JsObject], currentNum: Int, totalNum: Int, cluster: Option[String])
case class ChangeCommandStatus(envId: Int, projectId: Int, currentNum: Int, commandName: String, machine: String, cluster: Option[String])
case class ChangeOverStatus(envId: Int, projectId: Int, taskStatus: TaskStatus, endTime: DateTime, version: String, cluster: Option[String])
case class RemoveStatus(envId: Int, projectId: Int, clusterName: Option[String])
case class FindLastStatus(key: String)

case class ForceTerminate(envId: Int, projectId: Int, clusterName: Option[String])

class WSSchedule{
  def start(socketActor: ActorRef, notify: String): Cancellable = {
    Akka.system.scheduler.schedule(
      1 second,
      0.5 second,
      socketActor,
      notify
    )
  }
}