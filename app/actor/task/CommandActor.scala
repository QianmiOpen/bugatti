package actor.task

import java.io.File

import akka.actor.Actor
import models.task.TaskCommand
import utils.SaltTools

/**
 * Created by jinwei on 14/7/14.
 */
object CommandActor {
  val baseLogPath = SaltTools.logPath
  //命令执行过程
  var envId_proejctIdCommands = Map.empty[String, (Seq[TaskCommand], Int)]
}

class CommandActor extends Actor {
  def receive = {
    case executeCommand(taskId, envId, projectId, order) => {
      CommandActor.envId_proejctIdCommands.get(s"${envId}_${projectId}") match {
        case Some((seq, num)) => {
          val command = seq(num)
          //TODO 推送状态
          //TODO 修改数据库状态




        }
        case _ =>
      }
    }
  }

  def executeSalt(taskId: Int, command: TaskCommand) = {
    val baseDir = s"${CommandActor.baseLogPath}/${taskId}"
    val executeLogPath = s"${baseDir}/execute.log"
    val resultLogPath = s"${baseDir}/result.log"
    val outputCommand = s"--log-file=${executeLogPath}"

    val logDir = new File(baseDir)
    if (!logDir.exists) {
      logDir.mkdirs()
    }
    val file = new File(resultLogPath)


  }
}

case class executeCommand(taskId: Int, envId: Int, projectId: Int, order: Int)
