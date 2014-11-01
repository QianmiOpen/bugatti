package actor.task

import java.io.File
import java.util.regex.Pattern

import akka.actor._
import com.qianmi.bugatti.actors.TimeOut
import enums.TaskEnum
import models.conf._
import models.task.{TaskCommand, TaskTemplateStep}
import utils._

import scala.concurrent.duration._
import scala.sys.process._
import scalax.file.Path

/**
 * Created by jinwei on 21/8/14.
 */
class EngineActor(timeout: Int) extends Actor with ActorLogging {

  import context._

  val _reg = """\{\{ *[^}]+ *\}\}""".r

  val TimeOutSeconds = timeout seconds

  var timeOutSchedule: Cancellable = _

  var _lastReplaceKey = ""

  override def preStart(): Unit = {
    log.info(s"preStart is invoked!")
    timeOutSchedule = context.system.scheduler.scheduleOnce(TimeOutSeconds, self, TimeOut)
  }

  override def postStop(): Unit = {
    log.info(s"postStop is invoked!")
    if (timeOutSchedule != null) {
      timeOutSchedule.cancel()
    }
  }

  def receive = {
    case replaceCommand: ReplaceCommand => {
      val hostname = replaceCommand.hostname
      val taskId = replaceCommand.taskObj.taskId.toInt
      val engine = new ScriptEngineUtil(replaceCommand.taskObj, Some(hostname))

      var taskCommandSeq = Seq.empty[TaskCommand]
      var errors = Set.empty[String]

      replaceCommand.templateStep.foreach { templateStep =>
        var command = templateStep.sls
        getContentKeys(command).foreach { key =>
          _lastReplaceKey = key
          val (ret, value) = engine.eval(key)
          if (ret) {
            command = command.replaceAll(Pattern.quote(s"{{${key}}}"), value)
          } else {
            errors += s"${key}: ${value}"
            log.error(value)
          }
        }

        taskCommandSeq = taskCommandSeq :+ TaskCommand(None, taskId, command, hostname, templateStep.name, TaskEnum.TaskWait, templateStep.orderNum)
      }

      if (errors isEmpty) {
        sender ! SuccessReplaceCommand(taskCommandSeq)
      } else {
        sender ! ErrorReplaceCommand(errors.mkString("""\\\n"""))
      }

      context.stop(self)
    }

    case rc: ReplaceConfigure => {
      val fileName = s"${rc.taskObj.confFileName}_${rc.hostname}"
      val task = rc.taskObj
      val taskId = task.taskId.toInt

      val envId = task.env.id.toInt
      val projectId = task.id.toInt
      val versionId = task.version.get.id.toInt

      val confSeq = ConfHelper.findByEnvId_ProjectId_VersionId(envId, projectId, versionId) match {
        case seq: Seq[Conf] if seq.isEmpty =>
          VersionHelper.copyConfigs(envId, versionId)
          ConfHelper.findByEnvId_ProjectId_VersionId(envId, projectId, versionId)
        case seq: Seq[Conf] if !seq.isEmpty =>
          seq
      }
      val baseDir = s"${ConfHelp.confPath}/${taskId}"
      val baseFilesPath = new File(s"${baseDir}/files")

      if (!baseFilesPath.exists()) {
        baseFilesPath.mkdirs()
      }

      val engine = new ScriptEngineUtil(rc.taskObj, Some(rc.hostname))

      val (isSuccess, str) = replaceConfSeq(baseDir, confSeq, engine)

      val baseDirPath = new File(new File(baseDir).getAbsolutePath)
      Process(Seq("tar", "zcf", s"../${fileName}.tar.gz", "."), baseFilesPath).!!

      Process(Seq("md5sum", s"${fileName}.tar.gz"), baseDirPath) #> new File(s"${baseDirPath}/${fileName}.tar.gz.md5") !

      Seq("rm", "-r", s"${baseDir}/files").!!

      if (isSuccess) {
        sender ! SuccessReplaceConf(taskId, envId, projectId, Option(versionId))
      } else {
        sender ! ErrorReplaceConf(str)
      }
      context.stop(self)
    }

    case TimeOut => {
      context.parent ! TimeoutReplace(_lastReplaceKey)
      context.stop(self)
    }

    case _ =>
  }

  def replaceConfSeq(baseDir: String, confSeq: Seq[Conf], engine: ScriptEngineUtil): (Boolean, String) = {
    if (confSeq.size > 0) {
      confSeq.foreach { xf =>
        val confContent = ConfContentHelper.findById(xf.id.get)
        confContent match {
          case Some(conf) =>
            val (isSuccess, str) = fillConfFile(conf, engine)
            if (isSuccess) {
              val newFile = new File(s"${baseDir}/files/${xf.path}")
              newFile.getParentFile().mkdirs()
              implicit val codec = scalax.io.Codec.UTF8
              val f = Path(newFile)
              if (conf.octet) {
                f.write(confContent.get.content)
              } else {
                f.write(str)
              }
            } else {
              //替换失败，输出错误变量
              return (false, s"${str}变量未替换！")
            }
          case _ => {
            //error 有confId,但是没有confContent
            return (false, s"配置文件为空！")
          }
        }
      }
      (true, s"配置文件替换成功!")
    } else {
      (true, "没有任何配置文件!")
    }
  }

  def fillConfFile(conf: ConfContent, engine: ScriptEngineUtil): (Boolean, String) = {
    var errors = Set.empty[String]
    if (!conf.octet) {
      var content = new String(conf.content, "UTF8")
      getContentKeys(content).foreach {
        key =>
          _lastReplaceKey = key
          val (ret, value) = engine.eval(key)
          if (ret) {
            content = content.replaceAll(Pattern.quote(s"{{${key}}}"), value)
          } else {
            errors += s"${key}: ${value}"
            log.info(value)
          }
      }
      if (errors isEmpty) {
        (true, content)
      } else {
        (false, errors.mkString("""\\\n"""))
      }
    } else {
      return (true, "")
    }
  }

  def getContentKeys(content: String): Seq[String] = {
    var retSeq = Seq.empty[String]
    var num = 0
    var key = ""
    var bAppend = false
    var lastLeft= false
    var stopNum = 0
    content.foreach { c =>
      if (c == '{') {
        num += 1
        if (lastLeft) {
          bAppend = true
          stopNum = num
        }
        lastLeft = true
      } else {
        lastLeft = false
        if(!bAppend){
          num = 0
          stopNum = 0
        }
      }

      if (c == '}' && num > 0) {
        num -= 1
        if (num == stopNum - 1) {
          bAppend = false
          retSeq = retSeq :+ key.drop(1)
          key = ""
        }
      }

      if (bAppend) {
        key = s"$key$c"
      }
    }
    log.info(s"config keys ==> ${retSeq.toSet}")
    retSeq.toSet.toSeq
  }
}

case class ReplaceCommand(taskObj: ProjectTask_v, templateStep: Seq[TaskTemplateStep], hostname: String)

case class ReplaceConfigure(taskObj: ProjectTask_v, hostname: String)