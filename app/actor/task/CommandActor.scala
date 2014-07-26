package actor.task

import java.io.File

import akka.actor.{Actor, Props}
import com.qianmi.bugatti.actors.SaltResult
import enums.TaskEnum
import enums.TaskEnum.TaskStatus
import models.conf.VersionHelper
import models.task.{Task, TaskCommand, TaskHelper}
import play.api.Logger
import play.api.libs.json.JsObject
import utils.ConfHelp

import scala.concurrent.duration._
import scala.sys.process._

/**
 * Created by jinwei on 14/7/14.
 */
object CommandActor {
  val baseLogPath = ConfHelp.logPath
  //命令执行过程
  var envId_projectIdCommands = Map.empty[String, Seq[TaskCommand]]
}

class CommandActor extends Actor {
  var _commands = Seq.empty[TaskCommand]
  var _taskId = 0
  var _envId = 0
  var _projectId = 0
  var _order = 0
  var _versionId = Option.empty[Int]

  def receive = {
    case SaltResult(result, excuteMicroseconds) => {
      println(s"result command ==> ${result}")
      Logger.info(s"result ==> ${result}")
    }
    case insertCommands: InsertCommands => {
      _taskId = insertCommands.taskId
      _envId = insertCommands.envId
      _projectId = insertCommands.projectId
      _versionId = insertCommands.versionId

      if(insertCommands.commandList.length == 0){
        noCommands(_taskId, _envId, _projectId, insertCommands.json)
        closeSelf
      } else {
        _commands = insertCommands.commandList
        //      CommandActor.envId_projectIdCommands += s"${envId}_${projectId}" -> commands
        self ! ExecuteCommand(_taskId, _envId, _projectId, _versionId, 1)
      }
    }
    case executeCommand: ExecuteCommand => {
      _order = executeCommand.order
      if(_order <= _commands.length){
        val command = _commands(_order - 1)
        //TODO 推送状态
        MyActor.superviseTaskActor ! ChangeCommandStatus(_envId, _projectId, _order, command.sls, command.machine)
        //TODO 修改数据库状态 （暂时取消这个步骤，因为和日志有重合功能）

        executeSalt(_taskId, command, _envId, _projectId, _versionId, _order)

      } else {
        terminateCommand(TaskEnum.TaskSuccess)
      }
    }

    case tcs: TerminateCommands => {
      terminateCommand(tcs.status)
    }
  }

  def terminateCommand(status: TaskStatus) = {
    TaskHelper.changeStatus(_taskId, status)
    val (task, version) = getTask_VS(_taskId)
    MyActor.superviseTaskActor ! ChangeOverStatus(_envId, _projectId, status, task.endTime.get, version)

    closeLookup
    closeSelf
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
    MyActor.superviseTaskActor ! ChangeOverStatus(envId, projectId, TaskEnum.TaskFailed, task.endTime.get, version)
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
    val baseDir = s"${CommandActor.baseLogPath}/${taskId}"
    val resultLogPath = s"${baseDir}/result.log"
    val logDir = new File(baseDir)
    if (!logDir.exists) {
      logDir.mkdirs()
    }
    val resultLogFile = new File(resultLogPath)
    (Seq("echo", message) #>> resultLogFile lines)
  }

  def executeSalt(taskId: Int, command: TaskCommand, envId: Int, projectId: Int, versionId: Option[Int], order: Int) = {
    val baseDir = s"${CommandActor.baseLogPath}/${taskId}"
    val resultLogPath = s"${baseDir}/result.log"

    val logDir = new File(baseDir)
    if (!logDir.exists) {
      logDir.mkdirs()
    }
    val file = new File(resultLogPath)
    val cmd = command.command
    val commandSeq = command2Seq(cmd)


    //TODO join jid
    if(cmd.startsWith("bugatti")){
      commandSeq(1) match {
        case "copyfile" => {
          val confActor = context.actorOf(Props[ConfActor], s"confActor_${envId}_${projectId}_${order}")
          confActor ! CopyConfFile(taskId, envId, projectId, versionId.get, order)
        }
      }
    } else {//正常的salt命令
      //1、根据syndic获取ip
      val key = s"${_envId}_${_projectId}"
      Logger.debug(s"commnadActor key ==> ${key}")
      Logger.debug(s"commnadActor eps ==> ${MyActor.envId_projectId_syndic.get(key)}")
      Logger.debug(s"commnadActor eps ==> ${MyActor.syndic_ip.get(MyActor.envId_projectId_syndic.get(key).getOrElse("0_0")).getOrElse("0.0.0.0")}")
      val syndicIp = MyActor.syndic_ip.get(MyActor.envId_projectId_syndic.get(s"${_envId}_${_projectId}").getOrElse("0_0")).getOrElse("0.0.0.0")

      val remotePath = s"akka.tcp://Spirit@${syndicIp}:2552/user/SpiritCommands"
      Logger.info("test111")
      import actor.task.MyActor.system.dispatcher
      val lookupActor = context.actorOf(Props(classOf[LookupActor], remotePath), s"lookupActor_${envId}_${projectId}_${order}")
      Logger.info("test222")
      //3、触发远程命令
      MyActor.system.scheduler.scheduleOnce(1.second) {
        lookupActor ! LookupActorCommand(commandSeq, taskId, envId, projectId, versionId, order)
        Logger.info("test333")
      }
    }


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
}

case class InsertCommands(taskId: Int, envId: Int, projectId: Int, versionId: Option[Int], commandList: Seq[TaskCommand], json: JsObject)
case class ExecuteCommand(taskId: Int, envId: Int, projectId: Int,versionId: Option[Int], order: Int)
case class TerminateCommands(status: TaskStatus)