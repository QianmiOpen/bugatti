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
import scala.collection.mutable
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
  //socketActor
  val socketActor = system.actorOf(Props[SocketActor], "socketActor")

  //容器：管理eid_pid状态
  var envId_projectIdStatus = Map.empty[String, TaskStatus]

  var statusMap = Json.obj()

  // envId_projectId -> syndic_name
  private var envId_projectId_syndic = mutable.Map.empty[String, String]
  // syndic_name -> ip
  private var syndic_ip = mutable.Map.empty[String, String]

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

  def forceTerminate(envId: Int, projectId: Int): Unit ={
    superviseTaskActor ! ForceTerminate(envId, projectId)
  }

  //在global被初始化
  def generateSchedule() = {
    new WSSchedule().start(socketActor, "notify")
  }

  def refreshSyndic() = {
    superviseTaskActor ! RefreshSyndic()
  }

  def get_envId_projectId_syndic(): mutable.Map[String, String] = {
    envId_projectId_syndic.clone().asInstanceOf[mutable.Map[String, String]]
  }

  def get_syndic_ip(): mutable.Map[String, String] = {
    syndic_ip.clone().asInstanceOf[mutable.Map[String, String]]
  }
}

class MyActor extends Actor with ActorLogging {
  import context._
  def receive = {
    case CreateNewTaskActor(envId, projectId) => {
      // 增加任务队列数量
      val key = s"${envId}_${projectId}"
      incQueueNum(key, 1)
      self ! NextTaskQueue(envId, projectId)
    }
    case NextTaskQueue(envId, projectId) => {
      val key = s"${envId}_${projectId}"
      log.info(s"envId_projectIdStatus before ==> ${MyActor.envId_projectIdStatus}")
      if(!MyActor.envId_projectIdStatus.keySet.contains(key)){
        MyActor.envId_projectIdStatus += key -> TaskEnum.TaskProcess
        log.info(s"envId_projectIdStatus after ==> ${MyActor.envId_projectIdStatus}")
        context.child(s"taskExecute_${key}").getOrElse(
          actorOf(Props[TaskExecute], s"taskExecute_${key}")
        ) ! NextTaskQueue(envId, projectId)
      }
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
//      val taskExecute = actorSelection(s"/user/superviseActor/taskExecute_${key}")
      //      val taskExecute = actorOf(Props[TaskExecute], s"taskExecute_${key}")

      context.child(s"taskExecute_${key}").getOrElse(
        actorOf(Props[TaskExecute], s"taskExecute_${key}")
      ) ! RemoveTaskQueue()

    }

    case ForceTerminate(envId, projectId) => {
      val key = s"${envId}_${projectId}"
      context.child(s"taskExecute_${key}").getOrElse(
        actorOf(Props[TaskExecute], s"taskExecute_${key}")
      ) ! TerminateCommands(TaskEnum.TaskFailed)
    }

    case RemoveStatus(envId, projectId) => {
      removeStatus(envId, projectId)
    }

    case RefreshSyndic() => {
      refreshSyndic()
    }
  }

  def refreshSyndic() = {
    EnvironmentProjectRelHelper.allNotEmpty.foreach{
      r =>
        MyActor.envId_projectId_syndic += s"${r.envId.get}_${r.projectId.get}" -> r.syndicName
    }
    Logger.debug(s"envId_projectId_syndic ==> ${MyActor.envId_projectId_syndic}")
    AreaHelper.allInfo.foreach {
      a =>
        MyActor.syndic_ip += s"${a.syndicName}" -> a.syndicIp
    }
    Logger.debug(s"syndic_ip ==> ${MyActor.syndic_ip}")
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

  def removeStatus(envId: Int, projectId: Int) = {
    val key = s"${envId}_${projectId}"
    val queueNum = (MyActor.statusMap \ key \ "queueNum").asOpt[Int]
    queueNum match {
      case Some(qm) => {
        if(qm == 0){
          MyActor.statusMap = MyActor.statusMap - key
          MyActor.socketActor ! FindLastStatus(key)
        }
        else {
          MyActor.statusMap = MyActor.statusMap - key
          changeStatus(Json.obj(key -> Json.obj("queueNum" -> queueNum)))
        }
        MyActor.envId_projectIdStatus = MyActor.envId_projectIdStatus - key
        self ! NextTaskQueue(envId, projectId)
      }
      case _ => {
        MyActor.statusMap = MyActor.statusMap - key
        MyActor.envId_projectIdStatus = MyActor.envId_projectIdStatus - key
      }
    }
  }

}

case class CreateNewTaskActor(envId: Int, projectId: Int)
case class NextTaskQueue(envId: Int, projectId: Int)

case class ChangeTaskStatus(taskQueue: TaskQueue, taskName: String, queues: Seq[JsObject], currentNum: Int, totalNum: Int)
case class ChangeCommandStatus(envId: Int, projectId: Int, currentNum: Int, commandName: String, machine: String)
case class ChangeOverStatus(envId: Int, projectId: Int, taskStatus: TaskStatus, endTime: DateTime, version: String)
case class RemoveStatus(envId: Int, projectId: Int)
case class FindLastStatus(key: String)
case class RefreshSyndic()

case class ForceTerminate(envId: Int, projectId: Int)

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