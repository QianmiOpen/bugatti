package controllers.conf

import controllers.BaseController
import enums.FuncEnum
import models.conf._
import org.joda.time.DateTime
import play.api.data._
import play.api.data.Forms._
import play.api.libs.json._

/**
 * 配置文件
 */
object ConfController extends BaseController {

  implicit val confWrites = Json.writes[Conf]
  implicit val contentWrites = Json.writes[ConfContent]

  val confForm = Form(
    mapping(
      "id" -> optional(number),
      "envId" -> number,
      "projectId" -> number,
      "versionId" -> number,
      "jobNo" -> ignored(""),
      "name" -> optional(text),
      "path" -> nonEmptyText,
      "content" -> default(text, ""),
      "remark" -> optional(text),
      "updated" -> default(jodaDate("yyyy-MM-dd hh:mm:ss"), DateTime.now())
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
        else Ok(Json.toJson(ConfHelper.delete(id)))
      case None =>
        NotFound
    }

  }

  def save = AuthAction(FuncEnum.project) { implicit request =>
    confForm.bindFromRequest.fold(
      formWithErrors => BadRequest(Json.obj("r" -> formWithErrors.errorsAsJson)),
      confForm => {
        if (!UserHelper.hasProjectInEnv(confForm.projectId, confForm.envId, request.user)) Forbidden
        else Ok(Json.obj("r" -> Json.toJson(ConfHelper.create(confForm.copy(jobNo = request.user.jobNo)))))
      }
    )
  }

  def update(id: Int) = AuthAction(FuncEnum.project) { implicit request =>
    confForm.bindFromRequest.fold(
      formWithErrors => BadRequest(Json.obj("r" -> formWithErrors.errorsAsJson)),
      confForm => {
        if (!UserHelper.hasProjectInEnv(confForm.projectId, confForm.envId, request.user)) Forbidden
        else Ok(Json.obj("r" -> Json.toJson(ConfHelper.update(id, confForm.copy(jobNo = request.user.jobNo)))))
      }
    )
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
            val confForm = ConfForm(None, copyForm.envId, c.projectId, copyForm.versionId, c.jobNo, Some(c.name), c.path, if (content != None) content.get.content else "", c.remark, c.updated)
            ConfHelper.create(confForm)
          }
          Ok(Json.obj("r" -> "ok"))
        }
      }
    )

  }

}
