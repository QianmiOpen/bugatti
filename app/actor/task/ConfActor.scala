package actor.task

import java.io.File

import akka.actor.Actor
import models.conf.{ConfContent, ConfContentHelper, ConfHelper}
import play.api.libs.json.{Json, JsObject}
import utils.{SaltTools, TaskTools, ConfHelp}
import scala.sys.process._
import scalax.file.Path

/**
 * Created by jinwei on 14/7/14.
 */
class ConfActor extends Actor{
  var _json = Json.obj()
  val _reg = """\{\{ *[^}]+ *\}\}""".r
  def receive = {
    case CopyConfFile(taskId, envId, projectId, versionId, order, json) => {
      _json = json
      val (isSuccess, str) = generateConfFile(taskId, envId, projectId, versionId)
      if(isSuccess) {
        sender ! ExecuteCommand(taskId, envId, projectId, Option(versionId), order + 1)
      }
      else {
        sender ! ConfCopyFailed(str)
      }
      context.stop(self)
    }
  }

  def generateConfFile(taskId: Int, envId: Int, projectId: Int, versionId: Int): (Boolean, String) = {
    val fileName = (_json \ "confFileName").as[String]
    val confSeq = ConfHelper.findByEnvId_ProjectId_VersionId(envId, projectId, versionId)
    val baseDir = s"${ConfHelp.confPath}/${taskId}"
    val baseFilesPath = new File(s"${baseDir}/files")
    if (!baseFilesPath.exists()) {
      baseFilesPath.mkdirs()
    }
    if (confSeq.size > 0) {
      confSeq.foreach { xf =>
        val confContent = ConfContentHelper.findById(xf.id.get)
        confContent match {
          case Some(conf) =>
            val (isSuccess, str) = fillConfFile(conf)
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
            return (false, s"没有找到配置文件！")
          }
        }

      }
    }

    val baseDirPath = new File(new File(baseDir).getAbsolutePath)
    Process(Seq("tar", "zcf", s"../${fileName}.tar.gz", "."), baseFilesPath).!!

    Process(Seq("md5sum", s"${fileName}.tar.gz"), baseDirPath) #> new File(s"${baseDirPath}/${fileName}.tar.gz.md5") !
//    Process(Seq("md5", s"${fileName}.tar.gz"), baseDirPath) #> new File(s"${baseDirPath}/${fileName}.tar.gz.md5") !

    Seq("rm", "-r", s"${baseDir}/files").!!

    (true, "执行成功")
  }

    def fillConfFile(conf: ConfContent): (Boolean, String) = {
      if(!conf.octet){
        var content = new String(conf.content, "UTF8")
        val seq = SaltTools.findErrorConf(_json, content)
        if(seq.length > 0){
          return (false, seq.mkString(","))
        }
        else {
          _reg.findAllIn(content).foreach{
            key =>
              val realKey = key.replaceAll("\\{\\{", "").replaceAll("\\}\\}", "")
              content = content.replaceAll(s"\\{\\{${realKey}\\}\\}", TaskTools.trimQuotes((_json \ realKey).toString))
          }
          return (true, content)
        }
      }else{
        return (true, "")
      }
    }
}

case class CopyConfFile(taskId: Int, envId: Int, projectId: Int, versionId: Int, order: Int, json: JsObject)
