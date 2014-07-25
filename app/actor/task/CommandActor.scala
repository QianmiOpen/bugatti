package actor.task

import java.io.File

import akka.actor.{Actor, Props}
import com.qianmi.bugatti.actors.SaltResult
import enums.TaskEnum
import enums.TaskEnum.TaskStatus
import models.conf.VersionHelper
import models.task.{Task, TaskCommand, TaskHelper}
import play.api.Logger
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
        noCommands(_taskId, _envId, _projectId)
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
        //执行成功
        //TODO 修改数据库状态
//        TaskHelper.changeStatus(_taskId, TaskEnum.TaskSuccess)
//        //TODO 推送状态
//        val (task, version) = getTask_VS(_taskId)
//        MyActor.superviseTaskActor ! ChangeOverStatus(_envId, _projectId, TaskEnum.TaskSuccess, task.endTime.get, version)
//
//        closeLookup
//        closeSelf
      }

//      CommandActor.envId_projectIdCommands.get(s"${envId}_${projectId}") match {
//        case Some(seq) => {
//          if(order < seq.length){
//            val command = seq(order - 1)
//            //TODO 推送状态
//            MyActor.superviseTaskActor ! ChangeCommandStatus(envId, projectId, order, command.sls, command.machine)
//            //TODO 修改数据库状态 （暂时取消这个步骤，因为和日志有重合功能）
//
//            executeSalt(taskId, command, envId, projectId, versionId, order)
//          } else {
//            //执行成功
//            //TODO 修改数据库状态
//            TaskHelper.changeStatus(taskId, TaskEnum.TaskSuccess)
//            //TODO 推送状态
//            val (task, version) = getTask_VS(taskId)
//            MyActor.superviseTaskActor ! ChangeOverStatus(envId, projectId, TaskEnum.TaskSuccess, task.endTime.get, version)
//
//          }
//        }
//        case _ => {
//          //error:项目未绑定机器或者模板异常
//          insertResultLog(taskId, "[error]项目未绑定机器或者模板异常")
//          TaskHelper.changeStatus(taskId, TaskEnum.TaskFailed)
//          val (task, version) = getTask_VS(taskId)
//          MyActor.superviseTaskActor ! ChangeOverStatus(envId, projectId, TaskEnum.TaskFailed, task.endTime.get, version)
//        }
//      }
    }

    case tcs: TerminateCommands => {
      terminateCommand(tcs.status)
    }

//    case CheckCommandLog(taskId, envId, projectId, versionId, order) => {
//      //修改为远程调用
//
//      CommandActor.envId_projectIdCommands.get(s"${envId}_${projectId}") match {
//        case Some(seq) => {
//          val command = seq(order)
//          val baseDir = s"${CommandActor.baseLogPath}/${taskId}"
//          val executeLogPath = s"${baseDir}/execute.log"
//          val resultLogPath = s"${baseDir}/result.log"
//
//          mergeLog(executeLogPath, resultLogPath, command.command)
//          if(checkLog(executeLogPath)){
//            self ! ExecuteCommand(taskId, envId, projectId, versionId, order + 1)
//          } else {
//            //失败
//            //清理envId_proejctIdCommands
//            CommandActor.envId_projectIdCommands -= s"${envId}_${projectId}"
//            //TODO 推送状态
//            //TODO 修改数据库 task
//            TaskHelper.changeStatus(taskId, TaskEnum.TaskFailed)
//            val (task, version) = getTask_VS(taskId)
//            MyActor.superviseTaskActor ! ChangeOverStatus(envId, projectId, TaskEnum.TaskFailed, task.endTime.get,version)
//          }
//        }
//        case _ =>
//        //这种情况貌似不存在
//      }
//    }
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

  def noCommands(taskId: Int, envId: Int, projectId: Int) = {
    //error:项目未绑定机器或者模板异常
    insertResultLog(taskId, "[error]项目未绑定机器或者模板异常")
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

//  def checkLog(path: String): Boolean = {
//    var result = true
//    val executeLog = new File(path)
//    if (executeLog.exists()) {
//      val row = (s"tail -n3 ${path}" !!).split("\n")(0)
//      if (row.split(":").length > 1) {
//        val failedNum = row.split(":")(1).trim().toInt
//        if (failedNum == 0) {
//          result = true
//        } else {
//          result = false
//        }
//      } else {
//        result = false
//      }
//    }
//    else {
//      result = false
//    }
//    result
//  }

//  def mergeLog(executeLogPath: String, resultLogPath: String, cmd: String) = {
//    val resultLogFile = new File(resultLogPath)
//    (Seq("echo", "=====================================华丽分割线=====================================") #>> resultLogFile lines)
//    (Seq("echo", s"command: ${cmd}\n") #>> resultLogFile lines)
//
//    val executeLog = new File(executeLogPath)
//    //为何这个文件会被莫名的删掉？
//    (Seq("cat", executeLogPath) #>> resultLogFile lines)
//  }

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
//    val executeLogPath = s"${baseDir}/execute.log"
    val resultLogPath = s"${baseDir}/result.log"

    val logDir = new File(baseDir)
    if (!logDir.exists) {
      logDir.mkdirs()
    }
    val file = new File(resultLogPath)
//    val outputCommand = s"--log-file=${executeLogPath}"
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
      //      MyActor.jobActor ! JobStatus(commandSeq, taskId, envId, projectId, versionId, order)

      // 修改为远程调用
      // 组装remotePath e.g."akka.tcp://CalculatorSystem@127.0.0.1:2552/user/calculator"
      //1、根据syndic获取ip
//      val remotePath = "akka.tcp://Spirit@192.168.59.3:2552/user/SpiritCommands"
      val key = s"${_envId}_${_projectId}"
      Logger.debug(s"commnadActor key ==> ${key}")
      Logger.debug(s"commnadActor eps ==> ${MyActor.envId_projectId_syndic.get(key)}")
      Logger.debug(s"commnadActor eps ==> ${MyActor.syndic_ip.get(MyActor.envId_projectId_syndic.get(key).getOrElse("0_0")).getOrElse("0.0.0.0")}")
      val syndicIp = MyActor.syndic_ip.get(MyActor.envId_projectId_syndic.get(s"${_envId}_${_projectId}").getOrElse("0_0")).getOrElse("0.0.0.0")

      val remotePath = s"akka.tcp://Spirit@${syndicIp}:2552/user/SpiritCommands"
      //      val remotePath = "akka.tcp://Spirit@0.0.0.0:2552/user/SpiritCommands"
//      val system = ActorSystem("LookupSystem", ConfigFactory.load("remotelookup"))
      Logger.info("test111")
      import actor.task.MyActor.system.dispatcher
      //      val remotePath = "akka.tcp://CalculatorSystem@127.0.0.1:2552/user/calculator"
      //2、使用myActor创建lookupActor
      //      val lookupActor = MyActor.system.actorOf(Props(classOf[LookupActor], remotePath), s"lookupActor_${envId}_${projectId}_${order}")
      //      import system.dispatcher
//      val lookupActor = MyActor.system.actorOf(Props(classOf[LookupActor], remotePath), s"lookupActor_${envId}_${projectId}_${order}")
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

case class InsertCommands(taskId: Int, envId: Int, projectId: Int, versionId: Option[Int], commandList: Seq[TaskCommand])
case class ExecuteCommand(taskId: Int, envId: Int, projectId: Int,versionId: Option[Int], order: Int)
case class TerminateCommands(status: TaskStatus)
//case class CheckCommandLog(taskId: Int, envId: Int, projectId: Int,versionId: Option[Int], order: Int)
