package controllers.admin

import controllers.BaseController
import enums._
import models.conf._
import play.api.data.Forms._
import play.api.data._
import play.api.libs.json._
import play.api.mvc.Action
import enums.ContainerTypeEnum.Container
import enums.StateEnum.State

/**
 * 项目于环境关系配置
 *
 * @author of546
 */
object RelationController extends BaseController {

  implicit val variableWrites = Json.writes[Variable]
  implicit val relationWrites = Json.writes[Host]
  implicit val relationFormWrites = Json.writes[EnvRelForm]

  case class Ip(a: Int, b: Int, c: Int, d: Int, e: Int)
  case class HostIp(id: Option[Int], envId: Option[Int], projectId: Option[Int], preProjectId: Option[Int], areaId: Option[Int],
                  syndicName: String, spiritId: Int, name: String, ip: Ip, state: State,
                  containerType: Container, hostIp: Option[String], hostName: Option[String],
                  globalVariable: Seq[Variable]) {
    val hosts = (ip.d to ip.e) map { i =>
      val _ip = ip.a + "." + ip.b + "." + ip.c + "." + i
      Host(id, envId, projectId, preProjectId, areaId, syndicName, spiritId, name = name.format(i), _ip, state,
        containerType, hostIp, hostName, globalVariable)
    }
  }

  val relationForm = Form(
    mapping(
      "envId" -> number,
      "projectId" -> number,
      "ids" -> seq(number)
    )(EnvRelForm.apply)(EnvRelForm.unapply)
  )

  val relForm = Form(
    mapping(
      "id" -> optional(number),
      "envId" -> optional(number),
      "projectId" -> optional(number),
      "preProjectId" -> optional(number),
      "areaId" -> optional(number),
      "syndicName" -> default(text, ""),
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
          "value" -> text,
          "level" -> enums.form.enum(LevelEnum)
        )(Variable.apply)(Variable.unapply)
      )
    )(Host.apply)(Host.unapply)
  )

  val relIpsForm = Form(
    mapping(
      "id" -> optional(number),
      "envId" -> optional(number),
      "projectId" -> optional(number),
      "preProjectId" -> optional(number),
      "areaId" -> optional(number),
      "syndicName" -> default(text, ""),
      "spiritId" -> number,
      "name" -> text,
      "ip" -> mapping(
        "a" -> number(min = 0, max = 255),
        "b" -> number(min = 0, max = 255),
        "c" -> number(min = 0, max = 255),
        "d" -> number(min = 0, max = 255),
        "e" -> number(min = 0, max = 255)
      )(Ip.apply)(Ip.unapply),
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
          "value" -> text,
          "level" -> enums.form.enum(LevelEnum)
        )(Variable.apply)(Variable.unapply)
      )
    )(HostIp.apply)(HostIp.unapply)
  )

  def show(id: Int) = Action {
    Ok(Json.toJson(HostHelper.findById(id)))
  }

  def index(ip: Option[String], envId: Option[Int], projectId: Option[Int], sort: Option[String], direction: Option[String], page: Int, pageSize: Int) = AuthAction() {
    val result = HostHelper.all(
      ip.filterNot(_.isEmpty), envId, projectId, sort, direction, page, pageSize)
    Ok(Json.toJson(result))
  }

  def count(ip: Option[String], envId: Option[Int], projectId: Option[Int]) = AuthAction() {
    val result = HostHelper.count(ip.filterNot(_.isEmpty), envId, projectId)
    Ok(Json.toJson(result))
  }

  def ips(envId: Int) = AuthAction() {
    Ok(Json.toJson(HostHelper.findIpsByEnvId(envId)))
  }

  implicit val writerSave = new Writes[(String, Int)] {
    def writes(c: (String, Int)): JsValue = {
      Json.obj("ip" -> c._1, "result" -> c._2)
    }
  }
  def saveBatch() = AuthAction(RoleEnum.admin) { implicit request =>
    relIpsForm.bindFromRequest.fold(
      formWithErrors => BadRequest(formWithErrors.errorsAsJson),
      relIps => {
        val result = relIps.hosts.map( host =>
          (host.ip, HostHelper.create_result(host))
        )
        val msg = Json.obj("mod" -> ModEnum.relation.toString, "user" -> request.user.jobNo,
          "ip" -> request.remoteAddress, "msg" -> "批量增加关系", "data" -> Json.toJson(result)).toString
        ALogger.info(msg)
        Ok(Json.toJson(result))
      }
    )
  }

  def save() = AuthAction(RoleEnum.admin) { implicit request =>
    relForm.bindFromRequest.fold(
      formWithErrors => BadRequest(formWithErrors.errorsAsJson),
      rel => {
        val result = HostHelper.create_result(rel)
        val msg = Json.obj("mod" -> ModEnum.relation.toString, "user" -> request.user.jobNo,
          "ip" -> request.remoteAddress, "msg" -> "增加关系", "data" -> Json.toJson((rel.ip, result))).toString
        ALogger.info(msg)
        Ok(Json.toJson(result))
      }
    )
  }

  def delete(id: Int) = AuthAction(RoleEnum.admin) { implicit request =>
    HostHelper.findById(id) match {
      case Some(rel) =>
        val msg = Json.obj("mod" -> ModEnum.relation.toString, "user" -> request.user.jobNo,
        "ip" -> request.remoteAddress, "msg" -> "删除关系", "data" -> Json.toJson(rel)).toString
        ALogger.info(msg)
        Ok(Json.toJson(HostHelper.delete(rel)))
      case _ => NotFound
    }
  }

  def update(id: Int) = AuthAction() { implicit request =>
    relForm.bindFromRequest.fold(
      formWithErrors => BadRequest(formWithErrors.errorsAsJson),
      relation => {
        val msg = Json.obj("mod" -> ModEnum.relation.toString, "user" -> request.user.jobNo,
          "ip" -> request.remoteAddress, "msg" -> "修改关系", "data" -> Json.toJson(relation)).toString
        ALogger.info(msg)
        Ok(Json.toJson(HostHelper.update(relation)))
      }
    )
  }

  def bind = AuthAction() { implicit request =>
    relationForm.bindFromRequest.fold(
      formWithErrors => BadRequest(formWithErrors.errorsAsJson),
      relation => {
        val msg = Json.obj("mod" -> ModEnum.relation.toString, "user" -> request.user.jobNo,
          "ip" -> request.remoteAddress, "msg" -> "绑定关系", "data" -> Json.toJson(relation)).toString
        ALogger.info(msg)
        Ok(Json.toJson(HostHelper.bind(relation)))
      }
    )
  }

  def unbind(id: Int) = AuthAction() { implicit request =>
    HostHelper.findById(id) match {
      case Some(relation) =>
        val msg = Json.obj("mod" -> ModEnum.relation.toString, "user" -> request.user.jobNo,
          "ip" -> request.remoteAddress, "msg" -> "解除关系", "data" -> Json.toJson(relation)).toString
        ALogger.info(msg)
        Ok(Json.toJson(HostHelper.unbind(relation)))
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