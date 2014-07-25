package actor.task

import actor.ActorUtils
import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import enums.TaskEnum
import enums.TaskEnum.TaskStatus
import models.conf.{AreaHelper, EnvironmentProjectRelHelper}
import models.task.{TaskCommand, TaskQueueHelper, TaskQueue}
import org.joda.time.DateTime
import play.api.Logger
import play.api.libs.concurrent.Akka
import play.api.libs.iteratee.{Input, Done, Enumerator, Iteratee}
import play.api.libs.json.{JsString, JsObject, Json, JsValue}
import scala.concurrent.duration._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.Play.current

/**
 * Created by jinwei on 13/7/14.
 */
object MyActor {

  implicit val timeout = Timeout(2 seconds)

  val system = ActorUtils.system
  //管理taskQueue中，在同一时间只有一个eid_pid的任务在执行
  val superviseTaskActor = system.actorOf(Props[MyActor], "superviseActor")
  //check salt执行结果
//  val jobActor = system.actorOf(Props[CheckJob], "checkJob")
  //taskCommand 执行过程
//  val commandActor = system.actorOf(Props[CommandActor], "commandActor")
  //socketActor
  val socketActor = system.actorOf(Props[SocketActor], "socketActor")

  //容器：管理eid_pid状态
  var envId_projectIdStatus = Map.empty[String, TaskStatus]

  var statusMap = Json.obj()

  // envId_projectId -> syndic_name
  var envId_projectId_syndic = Map.empty[String, String]
  // syndic_name -> ip
  var syndic_ip = Map.empty[String, String]

  //test
//  generateSchedule()

  /**
   * 新建一个任务需要到actor的队列中处理
   */
  def createNewTask(envId: Int, projectId: Int) = {
    superviseTaskActor ! CreateNewTaskActor(envId, projectId)
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
  //在global被初始化
  def generateSchedule() = {
    new WSSchedule().start(socketActor, "notify")
  }

  def refreshSyndic() = {
    EnvironmentProjectRelHelper.allNotEmpty.foreach{
      r =>
        envId_projectId_syndic += s"${r.envId.get}_${r.projectId.get}" -> r.syndicName
    }
    Logger.debug(s"envId_projectId_syndic ==> ${envId_projectId_syndic}")
    AreaHelper.allInfo.foreach {
      a =>
        syndic_ip += s"${a.syndicName}" -> a.syndicIp
    }
    Logger.debug(s"syndic_ip ==> ${syndic_ip}")
  }
}

class MyActor extends Actor{
  import context._
  def receive = {
    case CreateNewTaskActor(envId, projectId) => {
      // 增加任务队列数量
      val key = s"${envId}_${projectId}"

      Logger.info("key ==> "+key)


      if(!MyActor.envId_projectIdStatus.keySet.contains(key)){
        val taskExecute = actorOf(Props[TaskExecute], s"taskExecute_${key}")
        MyActor.envId_projectIdStatus += key -> TaskEnum.TaskProcess
        incQueueNum(key, 1)
        taskExecute ! TaskGenerateCommand(envId, projectId)
      }
    }
    case NextTaskQueue(envId, projectId) => {
      val key = s"${envId}_${projectId}"
//      MyActor.envId_projectIdStatus = MyActor.envId_projectIdStatus - key
      context.child(s"taskExecute_${key}").getOrElse(
        actorOf(Props[TaskExecute], s"taskExecute_${key}")
      ) ! NextTaskQueue(envId, projectId)
    }
    case ChangeTaskStatus(tq, taskName, queuesJson, currentNum, totalNum) => {
      val key = s"${tq.envId}_${tq.projectId}"
      //1、修改queueNum
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
    case ChangeCommandStatus(envId, projectId, order, sls, machine) => {
      val key = s"${envId}_${projectId}"
      //1、currentNum
      changeStatus(mergerStatus(key, Json.obj("currentNum" -> order)))
      //2、command.commandName
      //3、comamnd.machine
      val json = Json.obj("command" -> Json.obj("sls" -> sls, "machine" -> machine))
      changeStatus(mergerStatus(key, json))
    }
    case ChangeOverStatus(envId, projectId, status, endTime, version) => {
      val key = s"${envId}_${projectId}"
      //1、taskStatus
      changeStatus(mergerStatus(key, Json.obj("taskStatus" -> Json.toJson(status))))
      //2、endTime
      changeStatus(mergerStatus(key, Json.obj("endTime" -> Json.toJson(endTime))))
      //3、version
      changeStatus(mergerStatus(key, Json.obj("version" -> version)))

      //TODO NextTaskQueue
      //复用taskExecute
      //      val taskExecute = actorSelection(s"/user/mySystem/superviseActor/taskExecute_${key}")
      val taskExecute = actorSelection(s"/user/superviseActor/taskExecute_${key}")
      //      val taskExecute = actorOf(Props[TaskExecute], s"taskExecute_${key}")

      taskExecute ! RemoveTaskQueue()
    }
    case RemoveStatus(envId, projectId) => {
      val key = s"${envId}_${projectId}"
      removeStatus(key)
      MyActor.envId_projectIdStatus = MyActor.envId_projectIdStatus - key
      self ! NextTaskQueue(envId, projectId)
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
    Logger.info("changeStatus ==>"+MyActor.statusMap.toString())
    MyActor.statusMap
  }

  def removeStatus(key: String) = {
    MyActor.statusMap = MyActor.statusMap - key
    MyActor.socketActor ! FindLastStatus(key)
  }

}

case class CreateNewTaskActor(envId: Int, projectId: Int)
case class NextTaskQueue(envId: Int, projectId: Int)

case class ChangeTaskStatus(taskQueue: TaskQueue, taskName: String, queues: Seq[JsObject], currentNum: Int, totalNum: Int)
case class ChangeCommandStatus(envId: Int, projectId: Int, currentNum: Int, commandName: String, machine: String)
case class ChangeOverStatus(envId: Int, projectId: Int, taskStatus: TaskStatus, endTime: DateTime, version: String)
case class RemoveStatus(envId: Int, projectId: Int)
case class FindLastStatus(key: String)

class WSSchedule{
  def start(socketActor: ActorRef, notify: String): Cancellable = {
    Akka.system.scheduler.schedule(
      1 second,
      1 second,
      socketActor,
      notify
    )
  }
}