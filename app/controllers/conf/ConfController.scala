package controllers.conf

import exceptions.UniqueNameException
import utils.{TaskTools, FileUtil}
import utils.ControlUtil._
import controllers.{BaseController}
import enums.{ModEnum, FuncEnum}
import models.conf._
import org.joda.time.DateTime
import play.api.data._
import play.api.data.Forms._
import play.api.libs.Files.TemporaryFile
import play.api.libs.json._

import scala.io.Source

/**
 * 配置文件
 */
object ConfController extends BaseController {

  implicit val confWrites = Json.writes[Conf]

  implicit val writer = new Writes[ConfContent] {
    def writes(c: ConfContent): JsValue = {
      implicit val codec = scala.io.Codec.UTF8
      val cs = if (c.octet) "" else Source.fromBytes(c.content).getLines().mkString("\n")
      Json.obj("id" -> c.id, "octet" -> c.octet, "content" -> cs)
    }
  }

  def msg(user: String, ip: String, msg: String, data: Conf) =
    Json.obj("mod" -> ModEnum.conf.toString, "user" -> user, "ip" -> ip, "msg" -> msg, "data" -> Json.toJson(data)).toString

  // error?
//  implicit val func = FuncEnum.project

  val confForm = Form(
    mapping(
      "id" -> optional(number),
      "envId" -> number,
      "projectId" -> number,
      "versionId" -> number,
      "jobNo" -> ignored(""),
      "name" -> optional(text),
      "path" -> nonEmptyText(maxLength = 255),
      "fileType" -> optional(text),
      "content" -> default(text, ""),
      "remark" -> optional(text),
      "updated" -> default(jodaDate("yyyy-MM-dd HH:mm:ss"), DateTime.now())
    )(ConfForm.apply)(ConfForm.unapply)
  )

  def show(id: Int) = AuthAction(FuncEnum.project) {
    ConfHelper.findById(id) match {
      case Some(conf) =>
        Ok(Json.obj("conf" -> Json.toJson(conf), "confContent" -> ConfContentHelper.findById(id)))
      case None => NotFound
    }
  }

  def all(envId: Int, versionId: Int) = AuthAction(FuncEnum.project) {
    Ok(Json.toJson(ConfHelper.findByEnvId_VersionId(envId, versionId)))
  }

  def delete(id: Int) = AuthAction(FuncEnum.project) { implicit request =>
    ConfHelper.findById(id) match {
      case Some(conf) =>
        if (!UserHelper.hasProjectInEnv(conf.projectId, conf.envId, request.user)) Forbidden
        else {
          ALogger.info(msg(request.user.jobNo, request.remoteAddress, "删除配置文件", conf))
          Ok(Json.toJson(ConfHelper.delete(id)))
        }
      case None => NotFound
    }
  }

  def save = AuthAction(FuncEnum.project) { implicit request =>
    confForm.bindFromRequest.fold(
      formWithErrors => BadRequest(formWithErrors.errorsAsJson),
      confForm => {
        if (!UserHelper.hasProjectInEnv(confForm.projectId, confForm.envId, request.user)) Forbidden
        else {
          try {
            ALogger.info(msg(request.user.jobNo, request.remoteAddress, "新增配置文件", confForm.toConf))
            Ok(Json.toJson(ConfHelper.create(confForm.copy(jobNo = request.user.jobNo))))
          } catch {
            case un: UniqueNameException => Ok(_Exist)
          }
        }
      }
    )
  }

  def update(id: Int) = AuthAction(FuncEnum.project) { implicit request =>
    confForm.bindFromRequest.fold(
      formWithErrors => BadRequest(formWithErrors.errorsAsJson),
      confForm => {
        if (!UserHelper.hasProjectInEnv(confForm.projectId, confForm.envId, request.user)) Forbidden
        else {
          try {
            ALogger.info(msg(request.user.jobNo, request.remoteAddress, "修改配置文件", confForm.toConf))
            Ok(Json.toJson(ConfHelper.update(id, confForm.copy(jobNo = request.user.jobNo))))
          } catch {
            case un: UniqueNameException => Ok(_Exist)
          }
        }
      }
    )
  }

  val maxSizeExceeded = app.configuration.getInt("file.exceeded.max_size").getOrElse(10 * 1024 * 1024)
  val octetFileType = app.configuration.getString("file.extension.octet_list").map(_.split(",").toList).getOrElse(List("key"))

  def isOctet_?(filename: String, bytes: Array[Byte]) = defining(FileUtil.getContentType(filename, bytes)) { _contentType =>
    if (_contentType == "application/octet-stream") true
    else octetFileType.exists(_ == FileUtil.getExtension(filename))
  }

  def upload = AuthAction[TemporaryFile](FuncEnum.project) { implicit request =>
    val reqConfForm: Option[ConfForm] = confForm.bindFromRequest().fold(
      formWithErrors => None,
      _confForm => Some(_confForm)
    )
    request.body.asMultipartFormData.map { body =>
      reqConfForm.map { _confForm =>
        if (!UserHelper.hasProjectInEnv(_confForm.projectId, _confForm.envId, request.user)) Forbidden
        else {

          val result = body.files.filter(f => f.ref.file.length() < maxSizeExceeded).map { tempFile =>
            val filePath = _confForm.path
            val _path = if (filePath.last != '/') filePath + '/' else filePath
            val confFormat = _confForm.copy(jobNo = request.user.jobNo, name = Some(tempFile.filename), path = _path + tempFile.filename, content = "")

            val bytes = scalax.io.Resource.fromFile(tempFile.ref.file).byteArray
            val isOctet = isOctet_?(tempFile.filename, bytes)
            Json.obj("fileName" -> tempFile.filename, "status" -> ConfHelper.create(confFormat.toConf, Some(ConfContent(None, isOctet, bytes))))
          }
          Ok(Json.toJson(result))

        }
      } getOrElse(BadRequest)
    } getOrElse(BadRequest)
  }

  def completer(envId: Int, projectId: Int, versionId: Int) = AuthAction(FuncEnum.project) { implicit request =>
    Ok(Json.parse(TaskTools.generateCodeCompleter(envId, projectId, versionId)))
  }

  // ===========================================================================
  // 配置文件历史记录
  // ===========================================================================
  implicit val confLogWrites = Json.writes[ConfLog]
  implicit val confLogContentWrites = new Writes[ConfLogContent] {
    def writes(c: ConfLogContent): JsValue = {
      implicit val codec = scala.io.Codec.UTF8
      val cs = if (c.octet) "" else Source.fromBytes(c.content).getLines().mkString("\n")
      Json.obj("id" -> c.id, "octet" -> c.octet, "content" -> cs)
    }
  }

  def logs(confId: Int, page: Int, pageSize: Int) = AuthAction(FuncEnum.project) {
    Ok(Json.toJson(ConfLogHelper.all(confId, page, pageSize)))
  }

  def logsCount(confId: Int) = AuthAction(FuncEnum.project) {
    Ok(Json.toJson(ConfLogHelper.count(confId)))
  }

  implicit def recordWrite = new Writes[(Option[ConfLog], Option[ConfLogContent])] {
    def writes(logType: (Option[ConfLog], Option[ConfLogContent])) = {
      Json.obj("log"-> logType._1,"logContent" -> logType._2)
    }
  }
  def log(id: Int) = AuthAction(FuncEnum.project) {
    val log = ConfLogHelper.findById(id)
    val logContent = ConfLogContentHelper.findById(id)
    Ok(Json.toJson(log, logContent))
  }

  // ===========================================================================
  // 一键拷贝, todo 无事务，后期改造为队列
  // ===========================================================================
  implicit val conFormWrites = Json.writes[CopyForm]
  case class CopyForm(target_eid: Int, target_vid: Int, envId: Int, versionId: Int, projectId: Int, ovr: Boolean)
  val copyForm = Form(
    mapping(
      "target_eid" -> number,
      "target_vid" -> number,
      "envId" -> number, // 原环境编号
      "versionId" -> number, // 原版本编号
      "projectId" -> number, // 原项目编号
      "ovr" -> default(boolean, false)
    )(CopyForm.apply)(CopyForm.unapply)
  )
  def copy = AuthAction(FuncEnum.project) { implicit request =>
    copyForm.bindFromRequest.fold(
      formWithErrors => BadRequest(formWithErrors.errorsAsJson),
      copyForm => {
        if (!UserHelper.hasProjectInEnv(copyForm.projectId, copyForm.envId, request.user)) Forbidden
        else if (copyForm.target_eid == copyForm.envId && copyForm.target_vid == copyForm.versionId) Ok(_Exist)
        else {
          val targetConfs = ConfHelper.findByEnvId_VersionId(copyForm.target_eid, copyForm.target_vid)
          val currConfs = ConfHelper.findByEnvId_VersionId(copyForm.envId, copyForm.versionId)
          val confs = copyForm.ovr match {
            case true =>
              targetConfs.filter(t => currConfs.map(_.path).contains(t.path)).foreach( c => ConfHelper.delete(c)) // delete exist
              targetConfs.filterNot(t => currConfs.map(_.path).contains(t.path)) // return targets
            case false =>
              targetConfs.filterNot(t => currConfs.map(_.path).contains(t.path))
          }
          // insert all
          confs.foreach { c =>
            val content = ConfContentHelper.findById(c.id.get)
            ConfHelper.create(c.copy(id = None, envId = copyForm.envId, versionId = copyForm.versionId), content)
          }
          val _msg = Json.obj("mod" -> ModEnum.conf.toString, "user" -> request.user.jobNo,
            "ip" -> request.remoteAddress, "msg" -> "一键拷贝", "data" -> Json.toJson(copyForm)).toString
          ALogger.info(_msg)
          Ok(_Success)
        }
      }
    )

  }

}
