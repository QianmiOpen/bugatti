package controllers.conf

import actor.task.{RefreshSyndic, MyActor}
import controllers.BaseController
import enums.{ModEnum, FuncEnum}
import models.conf._
import play.api.data._
import play.api.data.Forms._
import play.api.libs.json._
/**
 * 项目于环境关系配置
 * @author of546
 */
object RelationController extends BaseController {
  implicit val relationWrites = Json.writes[EnvironmentProjectRel]

  val relationForm = Form(
    mapping(
      "envId" -> number,
      "projectId" -> number,
      "ids" -> seq(number)
    )(EnvRelForm.apply)(EnvRelForm.unapply)
  )

  def index(envId: Option[Int], projectId: Option[Int], sort: Option[String], direction: Option[String], page: Int, pageSize: Int) = AuthAction(FuncEnum.relation) {
    val result = EnvironmentProjectRelHelper.all(envId, projectId, sort, direction, page, pageSize)
    Ok(Json.toJson(result))
  }

  def count(envId: Option[Int], projectId: Option[Int]) = AuthAction(FuncEnum.relation) {
    val result = EnvironmentProjectRelHelper.count(envId, projectId)
    Ok(Json.toJson(result))
  }

  def ips(envId: Int) = AuthAction(FuncEnum.relation) {
    Ok(Json.toJson(EnvironmentProjectRelHelper.findIpsByEnvId(envId)))
  }

  implicit val relationFormWrites = Json.writes[EnvRelForm]

  def bind = AuthAction(FuncEnum.relation) { implicit request =>
    relationForm.bindFromRequest.fold(
      formWithErrors => BadRequest(formWithErrors.errorsAsJson),
      relation => {
        val msg = Json.obj("mod" -> ModEnum.relation.toString, "user" -> request.user.jobNo,
          "ip" -> request.remoteAddress, "msg" -> "绑定关系", "data" -> Json.toJson(relation)).toString
        ALogger.info(msg)
        val result = EnvironmentProjectRelHelper.bind(relation)
        //刷新缓存
        MyActor.superviseTaskActor ! RefreshSyndic()
        Ok(Json.toJson(result))
      }
    )
  }

  def unbind(id: Int) = AuthAction(FuncEnum.relation) { implicit request =>
    EnvironmentProjectRelHelper.findById(id) match {
      case Some(relation) =>
        val msg = Json.obj("mod" -> ModEnum.relation.toString, "user" -> request.user.jobNo,
          "ip" -> request.remoteAddress, "msg" -> "解除关系", "data" -> Json.toJson(relation)).toString
        ALogger.info(msg)
        val result = EnvironmentProjectRelHelper.unbind(relation)
        //刷新缓存
        MyActor.superviseTaskActor ! RefreshSyndic()
        Ok(Json.toJson(result))
      case None =>
        NotFound
    }
  }

}