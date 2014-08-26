package controllers.conf

import controllers.BaseController
import enums.{ModEnum, FuncEnum}
import models.conf._
import org.joda.time.DateTime
import utils.ControlUtil._
import play.api.Logger
import play.api.data._
import play.api.data.Forms._
import play.api.mvc._
import play.api.libs.json._

import scala.collection.mutable
import scala.io.Source

/**
 * 项目版本
 *
 * @author of546
 * @author of729
 */
object VersionController extends BaseController {

  implicit val versionWrites = Json.writes[Version]

  def msg(user: String, ip: String, msg: String, data: Version) =
    Json.obj("mod" -> ModEnum.version.toString, "user" -> user, "ip" -> ip, "msg" -> msg, "data" -> Json.toJson(data)).toString

  val versionForm = Form(
    mapping(
      "id" -> optional(number),
      "projectId" -> number,
      "vs" -> nonEmptyText,
      "updated" -> default(jodaDate("yyyy-MM-dd HH:mm:ss"), DateTime.now())
    )(Version.apply)(Version.unapply)
  )

  def nexusVersions(projectId: Int) = Action {
    // 1、根据projectId获取项目attribute中的groupId、artifactId
    val groupId = AttributeHelper.getValue(projectId, "t_groupId").map(_.replaceAll("\\.", "/"))
    val artifactId = AttributeHelper.getValue(projectId, "t_artifactId")
    Logger.info(s"groupId: ${groupId}, artifactId: ${artifactId}")
    // 2、查询release、snapshot版本
    val result = (groupId, artifactId) match {
      case (Some(gid), Some(aid)) => _makeVersion(gid, aid)
      case _ => List.empty[String]
    }
    // 3、拼接版本号，按照版本号逆序
    val resultReverse = result.sorted.reverse
    Logger.info(s"nexus return versions : [${resultReverse}]")
    Ok(Json.toJson(resultReverse))
  }

  def show(id: Int) = Action {
    Ok(Json.toJson(VersionHelper.findById(id)))
  }

  def index(projectId: Int, page: Int, pageSize: Int) = Action {
    Ok(Json.toJson(VersionHelper.all(projectId, page, pageSize)))
  }

  def count(projectId: Int) = Action {
    Ok(Json.toJson(VersionHelper.count(projectId)))
  }

  def all(projectId: Int, top: Int) = Action {
    Ok(Json.toJson(VersionHelper.all(projectId, top)))
  }

  def delete(id: Int) = AuthAction(FuncEnum.project) { implicit request =>
    VersionHelper.findById(id) match {
      case Some(version) =>
        if (!UserHelper.hasProjectSafe(version.projectId, request.user)) Forbidden
        else ConfHelper.findByVersionId(id).isEmpty match {
          case true =>
            ALogger.info(msg(request.user.jobNo, request.remoteAddress, "删除版本", version))
            Ok(Json.obj("r" -> Json.toJson(VersionHelper.delete(version))))
          case false =>
            Ok(Json.obj("r" -> "exist"))
        }
      case None =>
        Ok(Json.obj("r" -> "none"))
    }
  }

  def save = AuthAction(FuncEnum.project) { implicit request =>
    versionForm.bindFromRequest.fold(
      formWithErrors => BadRequest(Json.obj("r" -> formWithErrors.errorsAsJson)),
      versionForm => {
        if (!UserHelper.hasProjectSafe(versionForm.projectId, request.user)) Forbidden
        else VersionHelper.findByProjectId(versionForm.projectId).find(_.vs == versionForm.vs) match {
          case Some(_) =>
            Ok(Json.obj("r" -> "exist"))
          case None =>
            ALogger.info(msg(request.user.jobNo, request.remoteAddress, "新增版本", versionForm))
            Ok(Json.obj("r" ->Json.toJson(VersionHelper.create(versionForm))))
        }
      }
    )
  }

  def update(id: Int) = AuthAction(FuncEnum.project) { implicit request =>
    versionForm.bindFromRequest.fold(
      formWithErrors => BadRequest(Json.obj("r" -> formWithErrors.errorsAsJson)),
      versionForm => {
        if (!UserHelper.hasProjectSafe(versionForm.projectId, request.user)) Forbidden
        else VersionHelper.findByProjectId(versionForm.projectId)
          .filterNot(_.id == versionForm.id) // Some(id)
          .find(_.vs == versionForm.vs) match {
          case Some(_) =>
            Ok(Json.obj("r" -> "exist"))
          case None =>
            ALogger.info(msg(request.user.jobNo, request.remoteAddress, "修改版本", versionForm))
            Ok(Json.obj("r" -> Json.toJson(VersionHelper.update(id, versionForm))))
        }
      }
    )
  }

  lazy val NexusRepUrl = app.configuration.getString("nexus.rep_url").getOrElse("http://nexus.dev.ofpay.com/nexus/content/groups/public")
  def _makeVersion(groupId: String, artifactId: String): Seq[String] = {
    val list = new mutable.ListBuffer[String]
    val url = s"${NexusRepUrl}/${groupId}/${artifactId}"
    Logger.info(s"request version url = [${url}]")
    using(Source.fromURL(url)){ source =>
      val reg = """<a href=".+">([^/]+)/</a>""".r
      for (regMatch <- reg.findAllMatchIn(source.mkString)) {
        list += regMatch.group(1).toString
      }
    }
    Logger.info(s"request version result : [${list}]")
    list.toList
  }

}
