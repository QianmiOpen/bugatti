package controllers.conf

import controllers.BaseController
import enums.{StateEnum, ContainerTypeEnum, FuncEnum, ModEnum}
import models.conf._
import play.api.data.Forms._
import play.api.data._
import play.api.libs.json._
import play.api.mvc.Action

/**
 * 项目于环境关系配置
 * @author of546
 */
object RelationController extends BaseController {
  implicit val variableWrites = Json.writes[Variable]
  implicit val relationWrites = Json.writes[Host]

  val relationForm = Form(
    mapping(
      "envId" -> number,
      "projectId" -> number,
      "ids" -> seq(number)
    )(EnvRelForm.apply)(EnvRelForm.unapply)
  )

  val varRelForm = Form(
    mapping(
      "id" -> optional(number),
      "envId" -> optional(number),
      "projectId" -> optional(number),
      "areaId" -> optional(number),
      "syndicName" -> text,
      "spiritId" -> number,
      "name" -> text,
      "ip" -> text,
      "state" -> enums.form.enum(StateEnum),
      "containerType" -> enums.form.enum(ContainerTypeEnum),
      "hostIp" -> optional(text),
      "hostName" -> optional(text),
      "globalVariable" -> seq (
        mapping(
          "id" -> optional(number),
          "envId" -> optional(number),
          "projectId" -> optional(number),
          "name" -> text,
          "value" -> text
        )(Variable.apply)(Variable.unapply)
      )
    )(Host.apply)(Host.unapply)
  )

  def show(id: Int) = Action {
    Ok(Json.toJson(HostHelper.findById(id)))
  }

  def index(ip: Option[String], envId: Option[Int], projectId: Option[Int], sort: Option[String], direction: Option[String], page: Int, pageSize: Int) = AuthAction(FuncEnum.relation) {
    val result = HostHelper.all(
      ip.filterNot(_.isEmpty), envId, projectId, sort, direction, page, pageSize)
    Ok(Json.toJson(result))
  }

  def count(ip: Option[String], envId: Option[Int], projectId: Option[Int]) = AuthAction(FuncEnum.relation) {
    val result = HostHelper.count(ip.filterNot(_.isEmpty), envId, projectId)
    Ok(Json.toJson(result))
  }

  def ips(envId: Int) = AuthAction(FuncEnum.relation) {
    Ok(Json.toJson(HostHelper.findIpsByEnvId(envId)))
  }

  def update(id: Int) = AuthAction(FuncEnum.relation) { implicit request =>
    varRelForm.bindFromRequest.fold(
      formWithErrors => BadRequest(formWithErrors.errorsAsJson),
      relation => {
        Ok(Json.toJson(HostHelper.update(relation)))
      }
    )
  }

  implicit val relationFormWrites = Json.writes[EnvRelForm]

  def bind = AuthAction(FuncEnum.relation) { implicit request =>
    relationForm.bindFromRequest.fold(
      formWithErrors => BadRequest(formWithErrors.errorsAsJson),
      relation => {
        val msg = Json.obj("mod" -> ModEnum.relation.toString, "user" -> request.user.jobNo,
          "ip" -> request.remoteAddress, "msg" -> "绑定关系", "data" -> Json.toJson(relation)).toString
        ALogger.info(msg)
        val result = HostHelper.bind(relation)
        Ok(Json.toJson(result))
      }
    )
  }

  def unbind(id: Int) = AuthAction(FuncEnum.relation) { implicit request =>
    HostHelper.findById(id) match {
      case Some(relation) =>
        val msg = Json.obj("mod" -> ModEnum.relation.toString, "user" -> request.user.jobNo,
          "ip" -> request.remoteAddress, "msg" -> "解除关系", "data" -> Json.toJson(relation)).toString
        ALogger.info(msg)
        val result = HostHelper.unbind(relation)
        Ok(Json.toJson(result))
      case None => NotFound
    }
  }

  implicit val writer = new Writes[(String, Option[String])] {
    def writes(c: (String, Option[String])): JsValue = {
      Json.obj("ip" -> c._1, "host" -> c._2)
    }
  }
  def hosts(envId: Int, areaId: Int) = Action { implicit request =>
    Ok(Json.toJson(HostHelper.findUnbindByEnvId_AreaId(envId, areaId).map(r => (r.ip, r.hostName))))
  }

}