package actor.task

import java.io.File

import akka.actor.Actor
import models.conf.{ConfContentHelper, ConfHelper}
import play.api.libs.json.{Json, JsObject}
import utils.ConfHelp
import scala.sys.process._
import scalax.file.Path

/**
 * Created by jinwei on 14/7/14.
 */
class ConfActor extends Actor{
  var _json = Json.obj()
  def receive = {
    case CopyConfFile(taskId, envId, projectId, versionId, order, json) => {
      _json = json
      generateConfFile(taskId, envId, projectId, versionId)
      sender ! ExecuteCommand(taskId, envId, projectId, Option(versionId), order + 1)
      context.stop(self)
    }
  }

  def generateConfFile(taskId: Int, envId: Int, projectId: Int, versionId: Int) = {
    val fileName = (_json \ "confFileName").as[String]
    val confSeq = ConfHelper.findByEnvId_ProjectId_VersionId(envId, projectId, versionId)
    val baseDir = s"${ConfHelp.confPath}/${taskId}"
    val baseFilesPath = new File(s"${baseDir}/files")
    if(!baseFilesPath.exists()) {
      baseFilesPath.mkdirs()
    }
    if (confSeq.size > 0) {
      confSeq.foreach { xf =>
        val confContent = ConfContentHelper.findById(xf.id.get)
        val newFile = new File(s"${baseDir}/files/${xf.path}")
        newFile.getParentFile().mkdirs()
        implicit val codec = scalax.io.Codec.UTF8
        val f = Path(newFile)
        f.write(confContent.get.content)
      }
    }

    val baseDirPath = new File(new File(baseDir).getAbsolutePath)
    Process(Seq("tar", "zcf", s"../${fileName}.tar.gz", "."), baseFilesPath).!!

    Process(Seq("md5sum", s"${fileName}.tar.gz"), baseDirPath) #> new File(s"${baseDirPath}/${fileName}.tar.gz.md5") !

    Seq("rm", "-r", s"${baseDir}/files").!!

  }
}

case class CopyConfFile(taskId: Int, envId: Int, projectId: Int, versionId: Int, order: Int, json: JsObject)
