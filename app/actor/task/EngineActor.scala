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
class EngineActor(timeout: Int) extends Actor with ActorLogging{

  import context._

  val _reg = """\{\{ *[^}]+ *\}\}""".r

  var _key = ""
  var _errors = Set.empty[String]

  var _seq = Seq.empty[TaskCommand]

  var timeOutSchedule: Cancellable = _

  val TimeOutSeconds = timeout seconds

  var _fileName = ""
  var _taskId = 0
  var _envId = 0
  var _projectId = 0
  var _versionId = 0

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
      val taskObj = replaceCommand.taskObj
      val templateStep = replaceCommand.templateStep
      val hostname = replaceCommand.cluster
      val engine = createEngine(taskObj, hostname)

      templateStep.foreach {
        t =>
          var command = t.sls
          _reg.findAllIn(command).foreach{
            key =>
              _key = key
              val realkey = key.replaceAll("\\{\\{", "").replaceAll("\\}\\}", "")
              try{
                val value = engine.eval(realkey).toString()
                command = command.replaceAll(_key, value)
              }catch {
                case e: Exception => _errors += _key
              }
          }
          _seq :+ TaskCommand(None, replaceCommand.taskId, command, hostname, t.name, TaskEnum.TaskWait, t.orderNum)
      }
      if(_errors isEmpty){
        sender ! SuccessReplaceCommand(_seq)
        timeOutSchedule.cancel()
      }else {
        sender ! ErrorReplaceCommand(_errors)
        timeOutSchedule.cancel()
      }
      context.stop(self)
    }

    case rc: ReplaceConfigure => {
      _fileName = s"${rc.taskObj.confFileName}_${rc.cluster}"
      _taskId = rc.taskObj.taskId.toInt
      _envId = rc.envId
      _projectId = rc.projectId
      _versionId = rc.versionId

      val confSeq = ConfHelper.findByEnvId_ProjectId_VersionId(rc.envId, rc.projectId, rc.versionId)
      val baseDir = s"${ConfHelp.confPath}/${_taskId}"
      val baseFilesPath = new File(s"${baseDir}/files")
      if (!baseFilesPath.exists()) {
        baseFilesPath.mkdirs()
      }
      val e = createEngine(rc.taskObj, rc.cluster)

      val (isSuccess, str) = replaceConfSeq(baseDir, confSeq, e)

      val baseDirPath = new File(new File(baseDir).getAbsolutePath)
      Process(Seq("tar", "zcf", s"../${_fileName}.tar.gz", "."), baseFilesPath).!!

      Process(Seq("md5sum", s"${_fileName}.tar.gz"), baseDirPath) #> new File(s"${baseDirPath}/${_fileName}.tar.gz.md5") !
      //    Process(Seq("md5", s"${fileName}.tar.gz"), baseDirPath) #> new File(s"${baseDirPath}/${fileName}.tar.gz.md5") !

      Seq("rm", "-r", s"${baseDir}/files").!!

      if(isSuccess){
        sender ! SuccessReplaceConf(_taskId, _envId, _projectId, Option(_versionId))
      }else {
        sender ! ErrorReplaceConf(str)
      }
      context.stop(self)
    }

    case TimeOut => {
      sender ! TimeoutReplace(_key)
      context.stop(self)
    }

    case _ =>
  }

  def createEngine(taskObj: Task_v, hostname: String): ScriptEngine = {
    val engine = new ScriptEngineManager().getEngineByName("js")

    engine.eval(s"var __t__ = ${Json.toJson(taskObj).toString}")

    log.info(engine.eval("JSON.stringify(__t__)").toString)

    engine.eval("for (__attr in __t__) {this[__attr] = __t__[__attr];}")
    engine.eval("var alias = {};")

    try {
      taskObj.alias.foreach{case (key, value) => engine.eval(s"alias.${key} = ${value}")}
    } catch {
      case e: ScriptException => log.error(e.toString)
    }

    engine.eval(s"var current = ${Json.toJson(TaskTools.generateCurrent(hostname, taskObj))}")
    engine.eval(s"var confFileName = ${taskObj.confFileName}_${hostname}")

    engine
  }

  def replaceConfSeq(baseDir: String, confSeq: Seq[Conf], e: ScriptEngine): (Boolean, String) = {
    if (confSeq.size > 0) {
      confSeq.foreach { xf =>
        val confContent = ConfContentHelper.findById(xf.id.get)
        confContent match {
          case Some(conf) =>
            val (isSuccess, str) = fillConfFile(conf, e)
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
    }else{
      (true, "没有任何配置文件!")
    }
  }

  def fillConfFile(conf: ConfContent, e: ScriptEngine): (Boolean, String) = {
    if(!conf.octet){
      var content = new String(conf.content, "UTF8")
      _reg.findAllIn(content).foreach{
        key =>
          _key = key
          val realkey = key.replaceAll("\\{\\{", "").replaceAll("\\}\\}", "")
          try{
            val value = e.eval(realkey).toString()
            content = content.replaceAll(_key, value)
          }catch {
            case e: Exception => _errors += _key
          }
      }
      if(_errors isEmpty){
        (true, content)
      }else {
        (false, _errors.mkString(","))
      }
    }else{
      return (true, "")
    }
  }
}

case class ReplaceCommand(taskId: Int, taskObj: Task_v, templateStep: Seq[TaskTemplateStep], cluster: String)
case class ReplaceConfigure(envId: Int, projectId: Int, versionId: Int, taskObj: Task_v, cluster: String)
