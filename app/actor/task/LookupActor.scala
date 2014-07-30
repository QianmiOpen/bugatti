package actor.task

import java.io.File

import com.qianmi.bugatti.actors.{SpiritCommand, SaltCommand, SaltResult, TimeOut}
import enums.TaskEnum
import play.api.libs.json.Json
import utils.ConfHelp

import scala.sys.process._

/**
 * Created by jinwei on 17/7/14.
 */
import akka.actor._

import scala.concurrent.duration._

class LookupActor(path: String) extends Actor with ActorLogging{

  var _taskId = 0
  var _envId = 0
  var _projectId = 0
  var _order = 0
  var _versionId = Option.empty[Int]
  var _commandSeq = Seq.empty[String]

  val _baseLogPath = ConfHelp.logPath


  sendIdentifyRequest()

  def sendIdentifyRequest(): Unit = {
    context.actorSelection(path) ! Identify(path)
    import context.dispatcher
//    context.setReceiveTimeout(30.seconds)
    context.system.scheduler.scheduleOnce(3.seconds, self, ReceiveTimeout)
  }

  def receive = identifying

  def identifying: Actor.Receive = {
    case ActorIdentity(`path`, Some(actor)) =>
      context.watch(actor)
      context.become(active(actor))
    case ActorIdentity(`path`, None) => println(s"Remote actor not available: $path")
    case ReceiveTimeout              => sendIdentifyRequest()
    case _                           => println("Not ready yet")
  }

  def active(actor: ActorRef): Actor.Receive = {
    case lookupActorCommand: LookupActorCommand => {
      _taskId = lookupActorCommand.taskId
      _envId = lookupActorCommand.envId
      _projectId = lookupActorCommand.projectId
      _order = lookupActorCommand.order
      _versionId = lookupActorCommand.versionId
      _commandSeq = lookupActorCommand.commandSeq
      self ! SaltCommand(lookupActorCommand.commandSeq, 0, ".")
    }
//    case sc: SpiritCommand => {
//      actor ! sc
//    }
    case SaltCommand(commandSeq, 0, ".") => {
      actor ! SaltCommand(commandSeq)
    }

//    case executeCommand: ExecuteCommand => {
//      actor ! executeCommand
//    }
//
//    case terminateCommands: TerminateCommands => {
//      actor ! terminateCommands
//    }
    case SaltResult(result, excuteMicroseconds) => {
      log.info(s"result ==> ${result}")
      val jsonResult = Json.parse(result)
      val funType = (jsonResult \ "result" \ "fun").asOpt[String]
      funType match {
        case Some(fun) => {
          //1、写日志
          val baseDir = s"${_baseLogPath}/${_taskId}"
          val resultLogPath = s"${baseDir}/result.log"
          val logDir = new File(baseDir)
          if (!logDir.exists) {
            logDir.mkdirs()
          }
          val file = new File(resultLogPath)
          (Seq("echo", "=====================================华丽分割线=====================================") #>> file lines)
          (Seq("echo", s"command: ${_commandSeq.mkString(" ")}\n") #>> file lines)
          (Seq("echo", Json.prettyPrint(jsonResult)) #>> file lines)
          //2、判断是否成功
          val seqResult: Seq[Boolean] = (jsonResult \ "result" \ "return" \\ "result").map(js => js.as[Boolean])
          if(!seqResult.contains(false)){ //命令执行成功
            //3、调用commandActor
            getCommandActor() ! ExecuteCommand(_taskId, _envId, _projectId, _versionId, _order + 1)
            (Seq("echo", "命令执行成功") #>> file lines)
          } else { //命令执行失败
            getCommandActor() ! TerminateCommands(TaskEnum.TaskFailed)
            (Seq("echo", "命令执行失败") #>> file lines)
          }
        }
        case _ => { //如果不是state.sls 则直接返回成功
          getCommandActor() ! ExecuteCommand(_taskId, _envId, _projectId, _versionId, _order + 1)
        }
      }
    }
    case Terminated(`actor`) =>
      println("Actor terminated")
      sendIdentifyRequest()
      context.become(identifying)
    case ReceiveTimeout =>
    // ignore
    case saltTimeOut: TimeOut => {
      getCommandActor() ! TerminateCommands(TaskEnum.TaskFailed)

      val baseDir = s"${_baseLogPath}/${_taskId}"
      val resultLogPath = s"${baseDir}/result.log"
      val logDir = new File(baseDir)
      if (!logDir.exists) {
        logDir.mkdirs()
      }
      val file = new File(resultLogPath)
      (Seq("echo", "命令执行超时") #>> file lines)
    }
  }

  def getCommandActor() = {
    val path = s"/user/commandActor_${_envId}_${_projectId}"
    log.info(s"lookup context ==>${context.children}")
    log.info(s"lookup context ==>${context.system}")
    log.info(s"lookup context ==>${context.parent}")
//    val actor = context.actorSelection(path) ! Identify(path)
//    val actorPath = MyActor.system.child(s"commandActor_${_envId}_${_projectId}")
//    context.actorSelection(path)
    context.parent
  }

}

case class LookupActorCommand(commandSeq: Seq[String], taskId: Int, envId: Int, projectId: Int, versionId: Option[Int], order: Int)
