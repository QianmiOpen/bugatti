package actor.task

import java.io.File
import javax.script.{ScriptException, ScriptEngine, ScriptEngineManager}

import akka.actor.{Cancellable, Actor, ActorLogging}
import com.qianmi.bugatti.actors.TimeOut
import enums.TaskEnum
import models.conf.{Conf, ConfContent, ConfContentHelper, ConfHelper}
import models.task.{TaskCommand, TaskTemplateStep}
import play.api.libs.json.Json
import utils._
import scala.concurrent.duration._
import scalax.file.Path
import scala.sys.process._

import utils.TaskTools._

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
    timeOutSchedule = context.system.scheduler.scheduleOnce(TimeOutSeconds, self, TimeOut)
  }

  override def postStop(): Unit = {
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
        _reg.findAllIn(command).foreach { key =>
          _lastReplaceKey = key
          val realkey = key.replaceAll("\\{\\{", "").replaceAll("\\}\\}", "")
          val (ret, value) = engine.eval(realkey)
          if (ret) {
            command = command.replaceAll(key, value)
          } else {
            errors += key
            log.info(value)
          }
        }

        taskCommandSeq = taskCommandSeq :+ TaskCommand(None, taskId, command, hostname, templateStep.name, TaskEnum.TaskWait, templateStep.orderNum)
      }

      if (errors isEmpty) {
        sender ! SuccessReplaceCommand(taskCommandSeq)
      } else {
        sender ! ErrorReplaceCommand(errors)
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

      val confSeq = ConfHelper.findByEnvId_ProjectId_VersionId(envId, projectId, versionId)
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
      //    Process(Seq("md5", s"${fileName}.tar.gz"), baseDirPath) #> new File(s"${baseDirPath}/${fileName}.tar.gz.md5") !

      Seq("rm", "-r", s"${baseDir}/files").!!

      if (isSuccess) {
        sender ! SuccessReplaceConf(taskId, envId, projectId, Option(versionId))
      } else {
        sender ! ErrorReplaceConf(str)
      }
      context.stop(self)
    }

    case TimeOut => {
      sender ! TimeoutReplace(_lastReplaceKey)
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
      _reg.findAllIn(content).foreach {
        key =>
          _lastReplaceKey = key
          val realkey = key.replaceAll("\\{\\{", "").replaceAll("\\}\\}", "")
          val (ret, value) = engine.eval(realkey)
          if (ret) {
            content = content.replaceAll(_lastReplaceKey, value)
          } else {
            errors += key
            log.info(value)
          }
      }
      if (errors isEmpty) {
        (true, content)
      } else {
        (false, errors.mkString(","))
      }
    } else {
      return (true, "")
    }
  }
}

case class ReplaceCommand(taskObj: ProjectTask_v, templateStep: Seq[TaskTemplateStep], hostname: String)

case class ReplaceConfigure(taskObj: ProjectTask_v, hostname: String)
