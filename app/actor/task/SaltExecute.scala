package actor.task

import java.io.{File, FileWriter}

import akka.actor.Actor
import play.api.Logger
import utils.ConfHelp

import scala.sys.process._

/**
 * Created by jinwei on 13/7/14.
 */
class SaltExecute extends Actor {
  def receive = {
    case SaltCheck(commandSeq, taskId, envId, projectId, versionId, order) => {
      val baseLogPath = ConfHelp.logPath
      val path = s"${baseLogPath}/${taskId}/execute.log"

      if(doCommand(commandSeq, path)){
        MyActor.commandActor ! CheckCommandLog(taskId, envId, projectId, versionId, order)
      }else {
        MyActor.jobActor ! JobStatus(commandSeq, taskId, envId, projectId, versionId, order)
      }
    }
  }

  def doCommand(command: Seq[String], path: String): Boolean = {
    var result = false
    var jid = doCommandRetJid(command)
    val wf = new FileWriter(new File(path))
    var bWait = true
    for (i <- 1 to 600 if bWait) {
      Thread.sleep(1000)
      val lines = checkJobStates(jid)
      if (lines.size > 0) {
        if (lines.last.contains("is running as")){
          val runningJid = lines.last.replaceAll("^.* with jid ", "")
          var bWaitRunning = true
          for (j <- 1 to 600 if bWaitRunning) {
            Thread.sleep(1000)
            val runningRet = checkJobStates(runningJid)
            if (runningRet.size > 0 && runningRet.last.contains("Total")) {
              bWaitRunning = false
              jid = doCommandRetJid(command)
            }
          }
          result = false
        }else{
          bWait = false
          lines.foreach { x =>
            Logger.info(s"salt-run: ${x}")
            wf.write(s"${x}\n")
          }
          result = true
        }
      } else {
        result = false
      }
    }
    wf.flush()
    wf.close()
    result
  }

  def doCommandRetJid(command: Seq[String]): String = {
    val commandSeq = command :+ "--async"
    val ret = commandSeq.lines.mkString(",")
    val jid = ret.replaceAll("Executed command with job ID: ", "")
    Logger.info(s"Execute ${commandSeq.toString};jobId: ${jid}")
    jid
  }

  def checkJobStates(jid: String) = {
    val saltRun = Seq("salt-run", "jobs.lookup_jid", jid)
    Logger.info(s"Execute ${saltRun}")
    saltRun lines
  }

}
case class SaltCheck(commandSeq: Seq[String], taskId: Int, envId: Int, projectId: Int, versionId: Option[Int], order: Int)