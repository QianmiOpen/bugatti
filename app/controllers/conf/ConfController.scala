package controllers.conf

import controllers.{BaseController}
import enums.{ModEnum, FuncEnum}
import models.conf._
import org.joda.time.DateTime
import play.api.Logger
import play.api.data._
import play.api.data.Forms._
import play.api.libs.Files
import play.api.libs.Files.TemporaryFile
import play.api.libs.json._

/**
 * 配置文件
 */
object ConfController extends BaseController {

  implicit val confWrites = Json.writes[Conf]
  implicit val contentWrites = Json.writes[ConfContent]

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
      case None =>
        NotFound
    }
  }

  def all(envId: Int, versionId: Int) = AuthAction(FuncEnum.project) {
    Ok(Json.toJson(ConfHelper.findByEnvId_VersionId(envId, versionId)))
  }

  def delete(id: Int) = AuthAction(FuncEnum.project) { implicit request =>
    ConfHelper.findById(id) match {
      case Some(conf) =>
        if (!UserHelper.hasProjectInEnv(conf.projectId, conf.envId, request.user)) Forbidden
        else
          ALogger.info(msg(request.user.jobNo, request.remoteAddress, "删除配置文件", conf))
          Ok(Json.toJson(ConfHelper.delete(id)))
      case None =>
        NotFound
    }
  }

  def save = AuthAction(FuncEnum.project) { implicit request =>
    confForm.bindFromRequest.fold(
      formWithErrors => BadRequest(Json.obj("r" -> formWithErrors.errorsAsJson)),
      confForm => {
        if (!UserHelper.hasProjectInEnv(confForm.projectId, confForm.envId, request.user)) Forbidden
        else
          ALogger.info(msg(request.user.jobNo, request.remoteAddress, "新增配置文件", confForm.toConf))
          Ok(Json.obj("r" -> Json.toJson(ConfHelper.create(confForm.copy(jobNo = request.user.jobNo)))))
      }
    )
  }

  def update(id: Int) = AuthAction(FuncEnum.project) { implicit request =>
    confForm.bindFromRequest.fold(
      formWithErrors => BadRequest(Json.obj("r" -> formWithErrors.errorsAsJson)),
      confForm => {
        if (!UserHelper.hasProjectInEnv(confForm.projectId, confForm.envId, request.user)) Forbidden
        else
          ALogger.info(msg(request.user.jobNo, request.remoteAddress, "修改配置文件", confForm.toConf))
          Ok(Json.obj("r" -> Json.toJson(ConfHelper.update(id, confForm.copy(jobNo = request.user.jobNo)))))
      }
    )
  }

  val fileMaxSizeExceeded = 1 * 1024 * 1024
  val fileSuffixWhiteList = List("key", "conf", "properties", "ini", "xml", "txt")
  def _checkWhiteList(fileName: String) = {
    val suffix = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase
    fileSuffixWhiteList.contains(suffix)
  }

  def upload = AuthAction[TemporaryFile](FuncEnum.project) { implicit request =>
    val reqConfForm: Option[ConfForm] = confForm.bindFromRequest().fold(
      formWithErrors => None,
      _confForm => Some(_confForm)
    )
    request.body.asMultipartFormData.map { body =>
      reqConfForm.map { _confForm =>
        if (!UserHelper.hasProjectInEnv(_confForm.projectId, _confForm.envId, request.user)) Forbidden
        else { // todo security if contentType
          val result = body.files.filter(f => f.ref.file.length() < fileMaxSizeExceeded && _checkWhiteList(f.filename)).map { tempFile =>
            val fileContent = Files.readFile(tempFile.ref.file)
            val filePath = _confForm.path
            val _path = if (filePath.last != '/') filePath + '/' else filePath
            val confFormat = _confForm.copy(jobNo = request.user.jobNo, name = Some(tempFile.filename), path = _path + tempFile.filename, content = fileContent)
            Json.obj("fileName" -> tempFile.filename, "status" -> ConfHelper.create(confFormat))
          }
          Ok(Json.obj("r" -> result))
        }
      } getOrElse(BadRequest)
    } getOrElse(BadRequest)
  }

  // ===========================================================================
  // 配置文件历史记录
  // ===========================================================================
  implicit val confLogWrites = Json.writes[ConfLog]
  implicit val confLogContentWrites = Json.writes[ConfLogContent]

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
      formWithErrors => BadRequest(Json.obj("r" -> formWithErrors.errorsAsJson)),
      copyForm => {
        if (!UserHelper.hasProjectInEnv(copyForm.projectId, copyForm.envId, request.user)) Forbidden
        else if (copyForm.target_eid == copyForm.envId && copyForm.target_vid == copyForm.versionId) Ok(Json.obj("r" -> "exist"))
        else {
          val targetConfs = ConfHelper.findByEnvId_VersionId(copyForm.target_eid, copyForm.target_vid)
          val currConfs = ConfHelper.findByEnvId_VersionId(copyForm.envId, copyForm.versionId)
          val confs = copyForm.ovr match {
            case true =>
              targetConfs.filter(t => currConfs.map(_.path).contains(t.path)) foreach( c => ConfHelper.delete(c)) // delete exist
              targetConfs // return targets
            case false =>
              targetConfs.filterNot(t => currConfs.map(_.path).contains(t.path))
          }
          // insert all
          confs.foreach { c =>
            val content = ConfContentHelper.findById(c.id.get)
            val confForm = ConfForm(None, copyForm.envId, c.projectId, copyForm.versionId, c.jobNo, Some(c.name), c.path, c.fileType, if (content != None) content.get.content else "", c.remark, c.updated)
            ConfHelper.create(confForm)
          }
          val _msg = Json.obj("mod" -> ModEnum.conf.toString, "user" -> request.remoteAddress,
            "ip" -> request.remoteAddress, "msg" -> "一键拷贝", "data" -> Json.toJson(copyForm)).toString
          ALogger.info(_msg)
          Ok(Json.obj("r" -> "ok"))
        }
      }
    )

  }

}
