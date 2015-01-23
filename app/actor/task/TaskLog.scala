package actor.task

import java.io.File

import akka.util.Timeout
import play.api.Logger
import utils.{ConfHelp, Reader}

import scala.concurrent.duration._
import scala.language.postfixOps

/**
 * Created by jinwei on 5/8/14.
 */
object TaskLog{
  implicit val timeout = Timeout(2 seconds)

  val _baseLogPath = ConfHelp.logPath

  def fileGen(envId: Int, proId: Int, taskId: Int): File = {
    val filePath = s"${_baseLogPath}/${envId}/${proId}/${taskId}/result.log"
    new File(filePath)
  }

  def readHeader(envId: Int, proId: Int, taskId: Int, byteSize: Int): String = {
    val file = fileGen(envId, proId, taskId)
    val reader = new Reader()
    reader.reader(file, 0L, byteSize.toLong)._1
  }

  def readLog(envId: Int, proId: Int, taskId: Int): (String, String) = {
    val file = fileGen(envId, proId, taskId)
    if(file.exists()){
      val (from, fromMsg) = (file.length() - 1024L) match {
        case len if len > 0L =>
          (len, s"${len} bytes is hidden, show them ?")
        case len if len <= 0L =>
          (0L, "")
      }
      val reader = new Reader()
      val (logContent, len) = reader.reader(file, from, file.length())
      (fromMsg, logContent)
    }else{
      val msg = s"任务${taskId}日志不存在!"
      Logger.error(msg)
      ("", msg)
    }
  }

}