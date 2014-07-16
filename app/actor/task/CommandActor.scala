package actor.task

import java.io.File

import akka.actor.{Props, Actor}
import enums.TaskEnum
import models.conf.VersionHelper
import models.task.{Task, TaskHelper, TaskCommand}
import play.api.Logger
import utils.SaltTools
import scala.sys.process._

/**
 * Created by jinwei on 14/7/14.
 */
object CommandActor {
  val baseLogPath = SaltTools.logPath
  //命令执行过程
  var envId_projectIdCommands = Map.empty[String, Seq[TaskCommand]]
}

class CommandActor extends Actor {
  def receive = {
    case InsertCommands(taskId, envId, projectId, versionId, commands) => {
      CommandActor.envId_projectIdCommands += s"${envId}_${projectId}" -> commands
      self ! ExecuteCommand(taskId, envId, projectId, versionId, 1)
    }
    case ExecuteCommand(taskId, envId, projectId, versionId, order) => {
      CommandActor.envId_projectIdCommands.get(s"${envId}_${projectId}") match {
        case Some(seq) => {
          if(order <= seq.length){
            val command = seq(order)
            //TODO 推送状态
            MyActor.superviseTaskActor ! ChangeCommandStatus(envId, projectId, order, command.sls, command.machine)
            //TODO 修改数据库状态 （暂时取消这个步骤，因为和日志有重合功能）

            executeSalt(taskId, command, envId, projectId, versionId, order)
          } else {
            //执行成功
            //TODO 修改数据库状态
            TaskHelper.changeStatus(taskId, TaskEnum.TaskSuccess)
            //TODO 推送状态
            val (task, version) = getTask_VS(taskId)
            MyActor.superviseTaskActor ! ChangeOverStatus(envId, projectId, TaskEnum.TaskSuccess, task.endTime.get, version)

          }
        }
        case _ => {
          //error:项目未绑定机器或者模板异常
          insertResultLog(taskId, "[error]项目未绑定机器或者模板异常")
          TaskHelper.changeStatus(taskId, TaskEnum.TaskFailed)
          val (task, version) = getTask_VS(taskId)
          MyActor.superviseTaskActor ! ChangeOverStatus(envId, projectId, TaskEnum.TaskFailed, task.endTime.get, version)
        }
      }
    }

    case CheckCommandLog(taskId, envId, projectId, versionId, order) => {
      CommandActor.envId_projectIdCommands.get(s"${envId}_${projectId}") match {
        case Some(seq) => {
          val command = seq(order)
          val baseDir = s"${CommandActor.baseLogPath}/${taskId}"
          val executeLogPath = s"${baseDir}/execute.log"
          val resultLogPath = s"${baseDir}/result.log"

          mergeLog(executeLogPath, resultLogPath, command.command)
          if(checkLog(executeLogPath)){
            self ! ExecuteCommand(taskId, envId, projectId, versionId, order + 1)
          } else {
            //失败
            //清理envId_proejctIdCommands
            CommandActor.envId_projectIdCommands -= s"${envId}_${projectId}"
            //TODO 推送状态
            //TODO 修改数据库 task
            TaskHelper.changeStatus(taskId, TaskEnum.TaskFailed)
            val (task, version) = getTask_VS(taskId)
            MyActor.superviseTaskActor ! ChangeOverStatus(envId, projectId, TaskEnum.TaskFailed, task.endTime.get,version)
          }
        }
        case _ =>
          //这种情况貌似不存在
      }
    }
  }

  def getTask_VS(taskId: Int): (Task, String) = {
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

  def checkLog(path: String): Boolean = {
    var result = true
    val executeLog = new File(path)
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

  def mergeLog(executeLogPath: String, resultLogPath: String, cmd: String) = {
    val resultLogFile = new File(resultLogPath)
    (Seq("echo", "=====================================华丽分割线=====================================") #>> resultLogFile lines)
    (Seq("echo", s"command: ${cmd}\n") #>> resultLogFile lines)

    val executeLog = new File(executeLogPath)
    //为何这个文件会被莫名的删掉？
      (Seq("cat", executeLogPath) #>> resultLogFile lines)
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
    val executeLogPath = s"${baseDir}/execute.log"
    val resultLogPath = s"${baseDir}/result.log"

    val logDir = new File(baseDir)
    if (!logDir.exists) {
      logDir.mkdirs()
    }
    val file = new File(resultLogPath)
    val outputCommand = s"--log-file=${executeLogPath}"
    val cmd = command.command
    val commandSeq = command2Seq(cmd)


    //TODO join jid
    if(cmd.startsWith("bugatti")){
      commandSeq(1) match {
        case "copyfile" => {
          val confActor = context.actorOf(Props[ConfActor], "confActor")
          confActor ! CopyConfFile(taskId, envId, projectId, versionId.get, order)
        }
      }
    } else {//正常的salt命令
      MyActor.jobActor ! JobStatus(commandSeq, taskId, envId, projectId, versionId, order)
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
case class CheckCommandLog(taskId: Int, envId: Int, projectId: Int,versionId: Option[Int], order: Int)