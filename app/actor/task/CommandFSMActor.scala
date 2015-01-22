package actor.task

import java.io.File
import java.nio.file.{StandardOpenOption, Paths, Files}
import java.nio.charset.StandardCharsets

import actor.ActorUtils
import actor.salt.{ConnectStoped, RemoteSpirit}
import akka.actor.SupervisorStrategy.Escalate
import akka.actor._
import actor.task.CommandFSMActor._
import com.qianmi.bugatti.actors._
import enums.TaskEnum
import enums.TaskEnum._
import models.conf.VersionHelper
import models.task.{Task, TaskHelper, TaskCommand}
import play.api.libs.json.{JsError, JsSuccess, Json, JsObject}
import utils.{ScriptEngineUtil, ConfHelp, ProjectTask_v}
import scala.sys.process._
import scala.language.postfixOps
import scala.concurrent.duration._

/**
 * Created by jinwei on 10/1/15.
 */
object CommandFSMActor{
  sealed trait State
  case object Init extends State
  case object Failure extends State
  case object Executing extends State
  case object Finish extends State
  case object Stopping extends State
}

case class TaskInfo(taskId: Int, envId: Int, projectId: Int, versionId: Option[Int], clusterName: Option[String])
case class CommandStatus(commands: Seq[TaskCommand], taskDoif: Seq[String], commandSeq: Seq[String], order: Int, taskObj: ProjectTask_v, jid: String, taskInfo: TaskInfo, json: JsObject, engine: ScriptEngineUtil, status: TaskStatus)

class CommandFSMActor extends LoggingFSM[State, CommandStatus] {

  override val supervisorStrategy = OneForOneStrategy() {
    case e: Exception =>
      log.error(s"${self} catch exception: ${e.getMessage} ${e.getStackTraceString}")
      commandOver(_taskInfo.taskId, s" ${e.getMessage} ${e.getStackTraceString}")
      Escalate
  }

//  var _taskId = 0
  var _taskInfo = TaskInfo(0, 0, 0, None, None)
  val _baseLogPath = ConfHelp.logPath

  startWith(Init, CommandStatus(Seq.empty[TaskCommand], Seq.empty[String], Seq.empty[String], 0, null, "", null, null, null, TaskEnum.TaskProcess))

  when(Init, stateTimeout = 10 second){
    case Event(insert: Insert, data: CommandStatus) =>
//      _taskId = insert.taskId
      _taskInfo = TaskInfo(insert.taskId, insert.envId, insert.projectId, insert.versionId, insert.cluster)
      val engine = new ScriptEngineUtil(insert.taskObj, _taskInfo.clusterName)
      val taskObj = engine.setCHost
      val commandStatus = data.copy(commands = insert.commandList, taskDoif = insert.taskDoif, taskObj = taskObj, taskInfo = _taskInfo, json = insert.json, engine = engine)

      if (commandStatus.commands.isEmpty) {
        goto(Failure) using commandStatus.copy(status = TaskEnum.TaskFailed)
      }else {
        goto(Executing) using commandStatus
      }

    case Event(st: StopTask, data: CommandStatus) => {
      commandOver(_taskInfo.taskId, s"用户选择停止任务")
      goto(Stopping) using data
    }

    case Event(StateTimeout, data: CommandStatus) =>
      commandOver(_taskInfo.taskId, s"任务号:${_taskInfo.taskId} Init执行超时 actor is ${self}")
      goto(Finish) using data.copy(status = TaskEnum.TaskFailed)
  }

  when(Executing, stateTimeout = 180 second){
    case Event(ec: Execute, data: CommandStatus) =>
      log.info(s"executing taskId:${_taskInfo.taskId}, envId:${_taskInfo.envId}, projectId:${_taskInfo.projectId}, order:${data.order}")
      val engine = data.engine
      if(data.order < data.commands.length){
        val taskInfo = data.taskInfo
        val command = data.commands(data.order)
        val doif = data.taskDoif(data.order)
        log.info(s"doif is $doif")
        val (flag, result) = engine.eval(doif)
        if(doif == "" || (flag && result == "true")){
          commandOver(taskInfo.taskId, s"命令(${command.sls}):${command.command}")
          MyActor.superviseTaskActor ! ChangeCommandStatus(taskInfo.envId, taskInfo.projectId, data.order, command.sls, command.machine, taskInfo.clusterName)
          executeSalt(taskInfo.taskId, command, taskInfo.envId, taskInfo.projectId, taskInfo.versionId, data.order, data.json, data.taskObj)
        }else {
          commandOver(taskInfo.taskId, s"${command}跳过执行,原因:${doif}")
          //更新taskCommand状态
          context.parent ! UpdateCommandStatus(taskInfo.taskId, data.order, TaskEnum.TaskPass)
          //执行下一个任务
          self ! Execute()
        }
        stay using data.copy(order = data.order + 1, jid = "")
      }else {
        goto(Finish) using data.copy(status = TaskEnum.TaskSuccess)
      }
    case Event(ssr: SaltStatusResult, data: CommandStatus) => {
      if(!ssr.canPing){
        commandOver(data.taskInfo.taskId, "远程ip ping不通!")
        goto(Finish) using data.copy(status = TaskEnum.TaskFailed)
      }else if(!ssr.canSPing){
        commandOver(data.taskInfo.taskId, "远程salt ping不通!")
        goto(Finish) using data.copy(status = TaskEnum.TaskFailed)
      }else {
        val taskInfo = data.taskInfo
        log.info(s"mminfo is ${ssr.mmInfo}")
        if(ssr.mmInfo.isEmpty){
          commandOver(data.taskInfo.taskId, "grains为空")
          goto(Finish) using data.copy(status = TaskEnum.TaskFailed)
        }else {
          val taskObj = data.taskObj.copy(grains = Json.parse(ssr.mmInfo).as[JsObject])
          log.info(Json.prettyPrint(taskObj.grains))
          commandOver(taskInfo.taskId, Json.prettyPrint(taskObj.grains))
          self ! Execute()
          stay using data.copy(taskObj = taskObj)
        }
      }
    }
    case Event(sjb: SaltJobBegin, data: CommandStatus) => {
      val msg = s"任务:${sjb.jid}开始, envId:${_taskInfo.envId}, proId:${_taskInfo.projectId}, 执行时间:${sjb.excuteMicroseconds}"
      log.info(msg)
      commandOver(data.taskInfo.taskId, msg)
      stay using data.copy(jid = sjb.jid)
    }
    case Event(sje: SaltJobError, data: CommandStatus) => {
      log.info(s"任务:${data.jid} has receive SaltJobError")
      val msg = s"任务:${data.jid}执行失败,${sje.msg},执行时间:${sje.excuteMicroseconds}"
      log.error(msg)
      commandOver(data.taskInfo.taskId, msg)
      context.parent ! UpdateCommandStatus(data.taskInfo.taskId, data.order, TaskEnum.TaskFailed)
      goto(Finish) using data.copy(status = TaskEnum.TaskFailed)
    }
    case Event(sr: SaltJobOk, data: CommandStatus) =>{
      val msg = s"任务:${data.jid} has receive SaltJobOk, envId:${_taskInfo.envId}, proId:${_taskInfo.projectId}"
      log.info(msg)
      commandOver(data.taskInfo.taskId, msg)
      val srResult = sr.result
      val executeTime = sr.excuteMicroseconds
      val jsonResult = Json.parse(srResult)
      log.info(s"jsonResult is ${Json.prettyPrint(jsonResult)}")
      jsonResult.validate[JsObject] match {
        case s: JsSuccess[JsObject] => {
          val jResult = s.get
          val funType = (jResult \ "result" \ "fun").asOpt[String]
          funType match {
            case Some(fun) => {
              val baseDir = s"${`_baseLogPath`}/${data.taskInfo.taskId}"
              val resultLogPath = s"${baseDir}/result.log"
              val file = new File(resultLogPath)
              val exec_command = s"执行时间：${executeTime} ms\n${Json.prettyPrint(jResult).replaceAll( """\\n""", "\r\n").replaceAll("""\\t""", "\t")}"
              Files.write(file.toPath, exec_command.getBytes(StandardCharsets.UTF_8), Seq(StandardOpenOption.APPEND, StandardOpenOption.SYNC):_*)
              val seqResult: Seq[Boolean] = (jResult \ "result" \ "return" \\ "result").map(js =>
                try{
                  js.as[Boolean]
                }catch{
                  case e: Exception =>
                    false
                }
              )
              val exeResult: Seq[Boolean] = (jResult \ "result" \\ "success").map(js => js.as[Boolean])
              if (!seqResult.contains(false) && !exeResult.contains(false)) {
                //命令执行成功
                //3、调用commandActor
                context.parent ! UpdateCommandStatus(data.taskInfo.taskId, data.order, TaskEnum.TaskSuccess)
                (Seq("echo", "命令执行成功") #>> file lines)
                self ! Execute()
                (Seq("echo", "=====================================华丽分割线=====================================") #>> file lines)
                stay
              } else {
                //命令执行失败
                context.parent ! UpdateCommandStatus(data.taskInfo.taskId, data.order, TaskEnum.TaskFailed)
                (Seq("echo", "命令执行失败") #>> file lines)
                (Seq("echo", "=====================================华丽分割线=====================================") #>> file lines)
                goto(Finish) using data.copy(status = TaskEnum.TaskFailed)
              }
            }
            case _ => {
              context.parent ! UpdateCommandStatus(data.taskInfo.taskId, data.order, TaskEnum.TaskSuccess)
              //如果不是state.sls 则直接返回成功
              self ! Execute()
              stay
            }
          }
        }
        case e: JsError =>{
          log.warning(s"Errors: ${JsError.toFlatJson(e)}")
          stay
        }
      }
    }
    case Event(saltTimeOut: SaltTimeOut, data: CommandStatus) => {
      commandOver(data.taskInfo.taskId, "远程任务执行超时!")
      goto(Finish) using data.copy(status = TaskEnum.TaskFailed)
    }

    case Event(ccf: ConfCopyFailed, data: CommandStatus) => {
      commandOver(data.taskInfo.taskId, ccf.str)
      goto(Finish) using data.copy(status = TaskEnum.TaskFailed)
    }

    case Event(st: StopTask, data: CommandStatus) => {
      commandOver(data.taskInfo.taskId, s"用户选择停止任务")
      goto(Stopping) using data
    }

    case Event(StateTimeout, data: CommandStatus) =>
      log.info(s"任务${data.jid} has receive StateTimeout")
      commandOver(data.taskInfo.taskId, s"任务号:${data.taskInfo.taskId} Executing执行超时")
      goto(Finish) using data.copy(status = TaskEnum.TaskFailed)

  }

  when(Stopping, stateTimeout = 5 second){
    case Event(sjb: SaltJobBegin, data: CommandStatus) => {
      val jid = sjb.jid
      log.info(s"jid is $jid")
      ActorUtils.spirits ! RemoteSpirit(data.taskObj.cHost.get.spiritId, SaltJobStop(jid))
      stay using data.copy(jid = jid)
    }
    case Event(sjs: SaltJobStoped, data: CommandStatus) => {
      commandOver(data.taskInfo.taskId, s"任务号${data.jid}已被停止")
      goto(Finish) using data.copy(status = TaskEnum.TaskFailed)
    }

    case Event(StateTimeout, data: CommandStatus) =>
      commandOver(data.taskInfo.taskId, s"任务号:${data.taskInfo.taskId} Stopping执行超时")
      goto(Finish) using data.copy(status = TaskEnum.TaskFailed)
  }

  when(Failure){FSM.NullFunction}

  when(Finish)(FSM.NullFunction)

  whenUnhandled {
    case Event(ConnectStoped, s) =>
      log.error("connectException request {} in state {}/{}", ConnectStoped, stateName, s)
      commandOver(s.taskInfo.taskId, s"远程spirit异常")
      goto(Finish) using s.copy(status = TaskEnum.TaskFailed)
    case Event(e, s) =>
      log.warning("received unhandled request {} in state {}/{}", e, stateName, s)
      stay
  }

  onTransition {
    case Init -> Executing => {
      self ! Execute()
    }
    case Init -> Failure => {
      val taskInfo = _taskInfo
      val taskId = taskInfo.taskId
      insertResultLog(taskId, s"[error] ${nextStateData.json \ "error"}")
      TaskHelper.changeStatus(taskId, TaskEnum.TaskFailed)
      val (task, version) = getTask_VS(taskId)
      MyActor.superviseTaskActor ! ChangeOverStatus(taskInfo.envId, taskInfo.projectId, TaskEnum.TaskFailed, task.endTime.get, version, taskInfo.clusterName)
    }

    case (Init | Executing | Stopping) -> (Finish | Stopping) => {
      val taskInfo = _taskInfo
      TaskHelper.changeStatus(taskInfo.taskId, nextStateData.status)
      val (task, version) = getTask_VS(taskInfo.taskId)
//      MyActor.superviseTaskActor ! ChangeOverStatus(taskInfo.envId, taskInfo.projectId, nextStateData.status, task.endTime.get, version, taskInfo.clusterName)
      context.parent ! ChangeOverStatus(taskInfo.envId, taskInfo.projectId, nextStateData.status, task.endTime.get, version, taskInfo.clusterName)
    }

    case Executing -> Stopping => {
      if(nextStateData.jid != ""){
        ActorUtils.spirits ! RemoteSpirit(nextStateData.taskObj.cHost.get.spiritId, SaltJobStop(nextStateData.jid))
      }else {
        commandOver(nextStateData.taskInfo.taskId, s"jid为空,等待jid返回...")
      }
    }
  }

  def insertResultLog(taskId: Int, message: String) = {
    val baseDir = s"${_baseLogPath}/${taskId}"
    val resultLogPath = s"${baseDir}/result.log"
    val logDir = new File(baseDir)
    if (!logDir.exists) {
      logDir.mkdirs()
    }
    val resultLogFile = new File(resultLogPath)
    (Seq("echo", message) #>> resultLogFile lines)
  }

  def commandOver(taskId: Int, msg: String) = {
    if(taskId != 0){
      val baseDir = s"${_baseLogPath}/${taskId}"
      val resultLogPath = s"${baseDir}/result.log"
      val logDir = new File(baseDir)
      if (!logDir.exists) {
        logDir.mkdirs()
      }
      val file = new File(resultLogPath)
      (Seq("echo", s"${msg}") #>> file lines)
    } else {
      log.error(s"taskId is 0 and msg is ${msg}")
    }
  }

  def getTask_VS(taskId: Int): (Task, String) = {
    //TODO refactor
    val task = TaskHelper.findById(taskId)
    var version = ""
    task.versionId match {
      case Some(vid) => {
        version = VersionHelper.findById(vid).get.vs
      }
      case _ =>
    }
    (task, version)
  }

  def executeSalt(taskId: Int, command: TaskCommand, envId: Int, projectId: Int, versionId: Option[Int], order: Int, returnJson: JsObject, taskObj: ProjectTask_v) = {
    val baseDir = s"${_baseLogPath}/${taskId}"
    val resultLogPath = s"${baseDir}/result.log"

    val logDir = new File(baseDir)
    if (!logDir.exists) {
      logDir.mkdirs()
    }
    val file = new File(resultLogPath)
    val cmd = command.command
    val hostname = command.machine
    val commandSeq = CommandActor.command2Seq(cmd)
    log.info(s"executeSalt cmd:${cmd}")
    if (cmd.startsWith("bugatti")) {
      commandSeq(1) match {
        case "copyfile" => {
          val actorName = s"confActor_${envId}_${projectId}_${order}"
          val confActor = context.child(actorName).getOrElse(
            context.actorOf(Props[ConfActor], actorName)
          )
          confActor ! CopyConfFile(taskId, envId, projectId, versionId.get, order, returnJson, hostname, taskObj)
        }
        case "hostStatus" => {
          ActorUtils.spirits ! RemoteSpirit(taskObj.cHost.get.spiritId, SaltStatus(hostname, taskObj.cHost.get.ip))
        }
      }
    } else {
      //正常的salt命令
      ActorUtils.spirits ! RemoteSpirit(taskObj.cHost.get.spiritId, SaltCommand(commandSeq))
    }
  }

  initialize()
}

case class Insert(taskId: Int, envId: Int, projectId: Int, versionId: Option[Int], commandList: Seq[TaskCommand], json: JsObject, taskObj: ProjectTask_v, cluster: Option[String], taskDoif: Seq[String])
case class Execute()