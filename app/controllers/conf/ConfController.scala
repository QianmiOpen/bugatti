package controllers.conf

import java.nio.file.Files

import exceptions.UniqueNameException
import utils.{TaskTools, FileUtil}
import utils.ControlUtil._
import enums.{ModEnum, RoleEnum}
import models.conf._
import org.joda.time.DateTime
import controllers.{BaseController}
import play.api.mvc.Action
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

  def show(id: Int) = AuthAction() {
    ConfHelper.findById(id) match {
      case Some(conf) =>
        Ok(Json.obj("conf" -> Json.toJson(conf), "confContent" -> ConfContentHelper.findById(id)))
      case None => NotFound
    }
  }

  def all(envId: Int, projectId: Int, versionId: Int) = AuthAction(RoleEnum.user) {
    Ok(Json.toJson(ConfHelper.findByEnvId_ProjectId_VersionId(envId, projectId, versionId)))
  }

  def delete(id: Int) = AuthAction() { implicit request =>
    ConfHelper.findById(id) match {
      case Some(conf) =>
        if (UserHelper.hasProjectInEnv(conf.projectId, conf.envId, request.user) ||
            UserHelper.hasEnv(conf.envId, request.user)
        ) {
          ALogger.info(msg(request.user.jobNo, request.remoteAddress, "删除配置文件", conf))
          Ok(Json.toJson(ConfHelper.delete(id)))
        } else Forbidden
      case None => NotFound
    }
  }

  def save = AuthAction() { implicit request =>
    confForm.bindFromRequest.fold(
      formWithErrors => BadRequest(formWithErrors.errorsAsJson),
      confForm => {
        if (UserHelper.hasProjectInEnv(confForm.projectId, confForm.envId, request.user) ||
            UserHelper.hasEnv(confForm.envId, request.user)
        ) {
          try {
            ALogger.info(msg(request.user.jobNo, request.remoteAddress, "新增配置文件", confForm.toConf))
            Ok(Json.toJson(ConfHelper.create(confForm.copy(jobNo = request.user.jobNo))))
          } catch {
            case un: UniqueNameException => Ok(_Exist)
          }
        } else Forbidden
      }
    )
  }

  def update(id: Int) = AuthAction() { implicit request =>
    confForm.bindFromRequest.fold(
      formWithErrors => BadRequest(formWithErrors.errorsAsJson),
      confForm => {
        if (UserHelper.hasProjectInEnv(confForm.projectId, confForm.envId, request.user) ||
            UserHelper.hasEnv(confForm.envId, request.user)
        ) {
          try {
            ALogger.info(msg(request.user.jobNo, request.remoteAddress, "修改配置文件", confForm.toConf))
            Ok(Json.toJson(ConfHelper.update(id, confForm.copy(jobNo = request.user.jobNo))))
          } catch {
            case un: UniqueNameException => Ok(_Exist)
          }
        } else Forbidden
      }
    )
  }

  val maxSizeExceeded = app.configuration.getInt("file.exceeded.max_size").getOrElse(10 * 1024 * 1024)
  val octetFileType = app.configuration.getString("file.extension.octet_list").map(_.split(",").toList).getOrElse(List("key"))

  def isOctet_?(filename: String, bytes: Array[Byte]) = defining(FileUtil.getContentType(filename, bytes)) { _contentType =>
    if (_contentType == "application/octet-stream") true
    else octetFileType.exists(_ == FileUtil.getExtension(filename))
  }

  def upload = AuthAction[TemporaryFile]() { implicit request =>
    val reqConfForm: Option[ConfForm] = confForm.bindFromRequest().fold(
      formWithErrors => None,
      _confForm => Some(_confForm)
    )
    request.body.asMultipartFormData.map { body =>
      reqConfForm.map { _confForm =>
        if (UserHelper.hasProjectInEnv(_confForm.projectId, _confForm.envId, request.user) ||
            UserHelper.hasEnv(_confForm.envId, request.user)
        ) {
          val result = body.files.filter(f => f.ref.file.length() < maxSizeExceeded).map { tempFile =>
            val filePath = _confForm.path
            val _path = if (filePath.last != '/') filePath + '/' else filePath
            val confFormat = _confForm.copy(jobNo = request.user.jobNo, name = Some(tempFile.filename), path = _path + tempFile.filename, content = "")

            var bytes = Files.readAllBytes(tempFile.ref.file.toPath)
            val isOctet = isOctet_?(tempFile.filename, bytes)
            if (!isOctet) {
              bytes = new String(bytes, "UTF-8").replaceAll("(\r\n)|(\n\r)|\r", "\n").getBytes("UTF-8")
            }
            Json.obj("fileName" -> tempFile.filename, "status" -> ConfHelper.create(confFormat.toConf, Some(ConfContent(None, isOctet, bytes))))
          }
          Ok(Json.toJson(result))
        } else Forbidden
      } getOrElse(BadRequest("ConfForm Errors:" + confForm.bindFromRequest.errorsAsJson))
    } getOrElse(BadRequest("multipartFormData error"))
  }

  def completer(envId: Int, projectId: Int, versionId: Int) = Action { implicit request =>
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

  def logs(confId: Int, page: Int, pageSize: Int) = AuthAction() {
    Ok(Json.toJson(ConfLogHelper.all(confId, page, pageSize)))
  }

  def logsCount(confId: Int) = AuthAction() {
    Ok(Json.toJson(ConfLogHelper.count(confId)))
  }

  implicit def recordWrite = new Writes[(Option[ConfLog], Option[ConfLogContent])] {
    def writes(logType: (Option[ConfLog], Option[ConfLogContent])) = {
      Json.obj("log"-> logType._1,"logContent" -> logType._2)
    }
  }
  def log(id: Int) = AuthAction() {
    val log = ConfLogHelper.findById(id)
    val logContent = ConfLogContentHelper.findById(id)
    Ok(Json.toJson(log, logContent))
  }

  // ===========================================================================
  // 一键拷贝, todo 无事务，后期改造为队列
  // 从目标拷贝
  // ===========================================================================
  implicit val conFormWrites = Json.writes[CopyForm]
  case class CopyForm(target_eid: Int, target_vid: Int, envId: Int, versionId: Int, projectId: Int, ovr: Boolean, copy: Boolean)
  val copyForm = Form(
    mapping(
      "target_eid" -> number,
      "target_vid" -> number,
      "envId" -> number, // 原环境编号
      "versionId" -> number, // 原版本编号
      "projectId" -> number, // 原项目编号
      "ovr" -> default(boolean, false),
      "copy" -> default(boolean, true)
    )(CopyForm.apply)(CopyForm.unapply)
  )
  def copy = AuthAction() { implicit request =>
    copyForm.bindFromRequest.fold(
      formWithErrors => BadRequest(formWithErrors.errorsAsJson),
      copyForm => {
        if (copyForm.target_eid == copyForm.envId && copyForm.target_vid == copyForm.versionId) Ok(_Exist)
        else if (
          UserHelper.hasProjectInEnv(copyForm.projectId, copyForm.envId, request.user) ||
          UserHelper.hasProjectInEnv(copyForm.projectId, copyForm.target_eid, request.user) ||
          UserHelper.hasEnv(copyForm.envId, request.user) ||
          UserHelper.hasEnv(copyForm.target_eid, request.user)
        ) {
          val targetConfs = ConfHelper.findByEnvId_ProjectId_VersionId(copyForm.target_eid, copyForm.projectId, copyForm.target_vid)
          val currConfs = ConfHelper.findByEnvId_ProjectId_VersionId(copyForm.envId, copyForm.projectId, copyForm.versionId)
          val confs = (copyForm.ovr, copyForm.copy) match {
            case (true, true) =>
              currConfs.filter(c => targetConfs.map(_.path).contains(c.path)).foreach(c => ConfHelper.delete(c))
              targetConfs
            case (false, true) =>
              targetConfs.filterNot(t => currConfs.map(_.path).contains(t.path))
            case (true, false) =>
              currConfs.filter(t => targetConfs.map(_.path).contains(t.path)).foreach( c => ConfHelper.delete(c)) // delete exist
              targetConfs
            case (false, false) =>
              currConfs.filterNot(t => targetConfs.map(_.path).contains(t.path))
          }
          // insert all
          confs.foreach { c =>
            val content = ConfContentHelper.findById(c.id.get)
            ConfHelper.create(c.copy(id = None, envId = copyForm.envId, versionId = copyForm.versionId), content)
          }
          val opMsg = if (copyForm.copy) "一键拷贝" else "生成模板"
          val _msg = Json.obj("mod" -> ModEnum.conf.toString, "user" -> request.user.jobNo,
            "ip" -> request.remoteAddress, "msg" -> opMsg, "data" -> Json.toJson(copyForm)).toString
          ALogger.info(_msg)
          Ok(_Success)
        } else Forbidden
      }
    )

  }

}
