package actor.task

import java.io.File

import akka.actor.SupervisorStrategy.Escalate
import akka.actor._
import com.qianmi.bugatti.actors._
import enums.TaskEnum
import enums.TaskEnum.TaskStatus
import models.conf.VersionHelper
import models.task.{TaskQueue, Task, TaskCommand, TaskHelper}
import play.api.libs.json.{JsError, JsSuccess, Json, JsObject}
import utils.{ScriptEngineUtil, ProjectTask_v, ConfHelp}

import scala.concurrent.duration._
import scala.sys.process._
import scalax.file.Path
import scalax.io.StandardOpenOption._

/**
 * Created by jinwei on 14/7/14.
 */
object CommandActor{

  def command2Seq(command: String): Seq[String] = {
    var retSeq = Seq.empty[String]
    var bAppend = false
    command.split(" ").foreach { c =>
      if (bAppend) {
        retSeq = retSeq.dropRight(1) :+ (retSeq.last + s" $c")
      } else {
        retSeq = retSeq :+ c
      }
      if (c.contains("'") && !c.endsWith("'")) {
        bAppend = !bAppend
      }
    }

    retSeq.map(_.replace("'",""))
  }
}

class CommandActor extends Actor with ActorLogging {

  override val supervisorStrategy = OneForOneStrategy() {
    case e: Exception =>
      log.error(s"${self} catch ${sender} exception: ${e.getStackTrace}")
      Escalate
  }

  var _commands = Seq.empty[TaskCommand]
  var _taskDoif = Seq.empty[String]
  var _taskId = 0
  var _envId = 0
  var _projectId = 0
  var _order = 0
  var _versionId = Option.empty[Int]
  var _returnJson = Json.obj()

  val _baseLogPath = ConfHelp.logPath
  var _commandSeq = Seq.empty[String]

  var _taskObj: ProjectTask_v = null

  var _clusterName = Option.empty[String]
  var _jid = ""

  def receive = {
    case insertCommands: InsertCommands => {
      _taskId = insertCommands.taskId
      _envId = insertCommands.envId
      _projectId = insertCommands.projectId
      _versionId = insertCommands.versionId
      _returnJson = insertCommands.json
      _taskObj = insertCommands.taskObj
      _clusterName = insertCommands.cluster
      _taskDoif = insertCommands.taskDoif

      if (insertCommands.commandList.length == 0) {
        noCommands(_taskId, _envId, _projectId, insertCommands.json)
        closeSelf
      } else {
        _commands = insertCommands.commandList
        self ! ExecuteCommand(_taskId, _envId, _projectId, _versionId, 0)
      }
    }
    case executeCommand: ExecuteCommand => {
      val engine = new ScriptEngineUtil(_taskObj, _clusterName)
      _taskObj = engine.setCHost
      _order = executeCommand.order
      if (_order < _commands.length) {
        val command = _commands(_order)
        //TODO 增加判断do...if...
        val doif = _taskDoif(_order)
        val (flag, result) = engine.eval(doif)
        if(doif == "" || (flag && result == "true")){
          commandOver(s"命令:${command.command}(${command.sls})")
          MyActor.superviseTaskActor ! ChangeCommandStatus(_envId, _projectId, _order, command.sls, command.machine, _clusterName)
          executeSalt(_taskId, command, _envId, _projectId, _versionId, _order)
        }else {
          commandOver(s"${command}跳过执行,原因:${doif}")
          //更新taskCommand状态
          context.parent ! UpdateCommandStatus(_taskId, _order, TaskEnum.TaskPass)
          //执行下一个任务
          self ! ExecuteCommand(_taskId, _envId, _projectId, _versionId, _order + 1)
        }

//        MyActor.superviseTaskActor ! ChangeCommandStatus(_envId, _projectId, _order, command.sls, command.machine, _clusterName)
//        executeSalt(_taskId, command, _envId, _projectId, _versionId, _order)
      } else {
        terminateCommand(TaskEnum.TaskSuccess)
      }
    }

    case tcs: TerminateCommands => {
      terminateCommand(tcs.status)
    }

    case ssr: SaltStatusResult => {
      if(!ssr.canPing){
        self ! TerminateCommands(TaskEnum.TaskFailed, _envId, _projectId, _clusterName)
        commandOver("远程ip ping不通!")
      }else if(!ssr.canSPing){
        self ! TerminateCommands(TaskEnum.TaskFailed, _envId, _projectId, _clusterName)
        commandOver("远程salt ping不通!")
      }else{
        //重构taskObj的grains
        _taskObj = _taskObj.copy(grains = Json.parse(ssr.mmInfo).as[JsObject])
        self ! ExecuteCommand(_taskId, _envId, _projectId, _versionId, _order + 1)
      }
    }

    case sjb: SaltJobBegin => {
      _jid = sjb.jid
      val msg = s"任务:${_jid}开始,执行时间:${sjb.excuteMicroseconds}"
      log.debug(msg)
      commandOver(msg)
    }

    case sje: SaltJobError => {
      val msg = s"任务:${_jid}执行失败,${sje.msg},执行时间:${sje.excuteMicroseconds}"
      log.error(msg)
      commandOver(msg)
      context.parent ! UpdateCommandStatus(_taskId, _order, TaskEnum.TaskFailed)
      self ! TerminateCommands(TaskEnum.TaskFailed, _envId, _projectId, _clusterName)
    }

    case sr: SaltJobOk => {
      val srResult = sr.result
      val executeTime = sr.excuteMicroseconds
      log.debug(s"result.size ==> ${srResult.size}")
      val jsonResult = Json.parse(srResult)
      jsonResult.validate[Seq[JsObject]] match {
        case s: JsSuccess[Seq[JsObject]] => {
          s.get.foreach { jResult =>
            val funType = (jResult \ "result" \ "fun").asOpt[String]
            funType match {
              case Some(fun) => {
                //1、写日志
                val baseDir = s"${`_baseLogPath`}/${`_taskId`}"
                val resultLogPath = s"${baseDir}/result.log"
                log.debug(s"commandActor resultLogPath ==> ${resultLogPath}")
                val logDir = new File(baseDir)
                if (!logDir.exists) {
                  logDir.mkdirs()
                }
                val file = new File(resultLogPath)
                implicit val codec = scalax.io.Codec.UTF8
                val f = Path(file).outputStream(WriteAppend: _*)
                f.write(s"command: ${_commandSeq.mkString(" ")} 执行时间：${executeTime} ms\n${Json.prettyPrint(jResult).replaceAll( """\\n""", "\r\n").replaceAll("""\\t""", "\t")}")
                //                (Seq("echo", s"command: ${_commandSeq.mkString(" ")} 执行时间：${executeTime} ms\n") #>> file lines)
                //                (Seq("echo", Json.prettyPrint(jResult).replaceAll( """\\n""", "\r\n")) #>> file lines)
                //2、判断是否成功
                val seqResult: Seq[Boolean] = (jResult \ "result" \ "return" \\ "result").map(js => js.as[Boolean])
                val exeResult: Seq[Boolean] = (jResult \ "result" \\ "success").map(js => js.as[Boolean])
                if (!seqResult.contains(false) && !exeResult.contains(false)) {
                  //命令执行成功
                  //3、调用commandActor
                  context.parent ! UpdateCommandStatus(_taskId, _order, TaskEnum.TaskSuccess)
                  (Seq("echo", "命令执行成功") #>> file lines)
                  self ! ExecuteCommand(_taskId, _envId, _projectId, _versionId, _order + 1)
                } else {
                  //命令执行失败
                  context.parent ! UpdateCommandStatus(_taskId, _order, TaskEnum.TaskFailed)
                  (Seq("echo", "命令执行失败") #>> file lines)
                  self ! TerminateCommands(TaskEnum.TaskFailed, _envId, _projectId, _clusterName)
                }
                (Seq("echo", "=====================================华丽分割线=====================================") #>> file lines)
              }
              case _ => {
                context.parent ! UpdateCommandStatus(_taskId, _order, TaskEnum.TaskSuccess)
                //如果不是state.sls 则直接返回成功
                self ! ExecuteCommand(_taskId, _envId, _projectId, _versionId, _order + 1)
              }
            }
          }
        }

        case e: JsError => log.warning(s"Errors: ${JsError.toFlatJson(e)}")
      }
    }

    case saltTimeOut: SaltTimeOut => {
      self ! TerminateCommands(TaskEnum.TaskFailed, _envId, _projectId, _clusterName)
      commandOver("远程任务执行超时!")
    }

    case IdentityNone() => {
      log.error(s"远程spirit异常!")
      self ! TerminateCommands(TaskEnum.TaskFailed, _envId, _projectId, _clusterName)
      commandOver("远程spirit异常!")
    }
    case ccf: ConfCopyFailed => {
      self ! TerminateCommands(TaskEnum.TaskFailed, _envId, _projectId, _clusterName)
      commandOver(ccf.str)
    }

  }

  def commandOver(msg: String) = {
    val baseDir = s"${_baseLogPath}/${_taskId}"
    val resultLogPath = s"${baseDir}/result.log"
    val logDir = new File(baseDir)
    if (!logDir.exists) {
      logDir.mkdirs()
    }
    val file = new File(resultLogPath)
    (Seq("echo", s"${msg}") #>> file lines)
  }

  def terminateCommand(status: TaskStatus) = {
    TaskHelper.changeStatus(_taskId, status)
    val (task, version) = getTask_VS(_taskId)
    MyActor.superviseTaskActor ! ChangeOverStatus(_envId, _projectId, status, task.endTime.get, version, _clusterName)

    closeLookup
//    closeSelf
  }

  def closeLookup = {
    //TODO  关闭sender
    context.stop(sender)
  }

  def closeSelf = {
    context.stop(self) //直接关闭
  }

  def noCommands(taskId: Int, envId: Int, projectId: Int, json: JsObject) = {
    //error:项目未绑定机器或者模板异常
    insertResultLog(taskId, s"[error] ${json \ "error"}")
    TaskHelper.changeStatus(taskId, TaskEnum.TaskFailed)
    val (task, version) = getTask_VS(taskId)
    MyActor.superviseTaskActor ! ChangeOverStatus(envId, projectId, TaskEnum.TaskFailed, task.endTime.get, version, _clusterName)
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

  def executeSalt(taskId: Int, command: TaskCommand, envId: Int, projectId: Int, versionId: Option[Int], order: Int) = {
    val baseDir = s"${_baseLogPath}/${taskId}"
    val resultLogPath = s"${baseDir}/result.log"

    val logDir = new File(baseDir)
    if (!logDir.exists) {
      logDir.mkdirs()
    }
    val file = new File(resultLogPath)
    val cmd = command.command
    val hostname = command.machine
    _commandSeq = CommandActor.command2Seq(cmd)
    log.info(s"executeSalt ==> ${cmd}")


    //TODO join jid
    log.info(s"executeSalt cmd:${cmd}")
    if (cmd.startsWith("bugatti")) {
      _commandSeq(1) match {
        case "copyfile" => {
          val confActor = context.actorOf(Props[ConfActor], s"confActor_${envId}_${projectId}_${order}")
          confActor ! CopyConfFile(taskId, envId, projectId, versionId.get, order, _returnJson, hostname, _taskObj)
        }
        case "hostStatus" => {
          val remoteActor = getRemoteActor(envId, projectId, order)
          log.info(s"remoteActor ==> ${remoteActor}")
          remoteActor ! SaltStatus(hostname, _taskObj.cHost.get.ip)
        }
      }
    } else {
      //正常的salt命令
      callRemote(envId, projectId, order, _commandSeq)
    }
  }

  def getRemoteActor(envId: Int, projectId: Int, order: Int): ActorRef ={
    //1、根据syndic获取ip
    log.info(s"hostname => ${_clusterName}")
    log.info(s"taskObj => ${_taskObj}")
    val syndicIp: String = _taskObj.hosts.filter(_.name == _clusterName.get).map(_.proxyIp).headOption.getOrElse("0.0.0.0")
    log.debug(s"commnadActor syndicIp ==> ${syndicIp}")

    val remotePath = s"akka.tcp://Spirit@${syndicIp}:2552/user/SpiritCommands"
    log.info(s"remotePath ==> ${remotePath}")
    context.actorOf(Props(classOf[LookupActor], remotePath), s"lookupActor_${envId}_${projectId}_${_taskId}_${order}")
  }

  def callRemote(envId: Int, projectId: Int, order: Int, commandSeq: Seq[String]): Unit ={
    val lookupActor = getRemoteActor(envId, projectId, order)
    //3、触发远程命令
    import context._
    context.system.scheduler.scheduleOnce(1.second) {
      lookupActor ! SaltCommand(commandSeq)
    }
  }
}

case class InsertCommands(taskId: Int, envId: Int, projectId: Int, versionId: Option[Int], commandList: Seq[TaskCommand], json: JsObject, taskObj: ProjectTask_v, cluster: Option[String], taskDoif: Seq[String])

case class ExecuteCommand(taskId: Int, envId: Int, projectId: Int, versionId: Option[Int], order: Int)

case class TerminateCommands(status: TaskStatus, envId: Int, projectId: Int, clusterName: Option[String])

case class ConfCopyFailed(str: String)