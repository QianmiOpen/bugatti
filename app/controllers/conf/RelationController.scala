package controllers.conf

import models.conf._
import org.apache.commons.net.util.SubnetUtils
import play.api.Logger
import play.api.data._
import play.api.data.Forms._
import play.api.mvc._
import play.api.libs.json._
/**
 * 关系
 */
object RelationController extends Controller {
  implicit val relationWrites = Json.writes[EnvironmentProjectRel]
  //  implicit val ipWrites = Json.writes[IP]
  //  implicit val relationFormWrites = Json.writes[EnvironmentProjectRelForm]
  implicit val writer = new Writes[(String, String)] {
    def writes(c: (String, String)): JsValue = {
      Json.obj("key" -> c._1,"value" -> c._2)
    }
  }

  val relationForm = Form(
    mapping(
      "id" -> optional(number),
      "envId" -> number,
      "projectId" -> number,
      "ips" -> seq(
        mapping(
          "ip" -> nonEmptyText,
          "name" -> nonEmptyText
        )(IP.apply)(IP.unapply)
      )
    )(EnvironmentProjectRelForm.apply)(EnvironmentProjectRelForm.unapply)
  )

  def index(envId: Option[Int], projectId: Option[Int], page: Int, pageSize: Int) = Action {
    val result = EnvironmentProjectRelHelper.all(envId, projectId, page, pageSize)
    Ok(Json.toJson(result))
  }

  def count(envId: Option[Int], projectId: Option[Int]) = Action {
    val result = EnvironmentProjectRelHelper.count(envId, projectId)
    Ok(Json.toJson(result))
  }

  def show(id: Int) = Action {
    Ok(Json.toJson(EnvironmentProjectRelHelper.findById(id)))
  }

//  def ips(envId: Int) = Action {
//    val env = EnvironmentHelper.findById(envId)
//    val ip_range = env.map(_.ipRange.map(_.split(";").toList).getOrElse(Seq.empty[String])).getOrElse(Seq.empty[String]) // 格式化
//    val rel_ips = EnvironmentProjectRelHelper.findIpsByEnvId(envId)
//    val filter_ip = SaltUtil.getMinionsJson.filter { // 正式环境可用ip集合
//      case (k, v) =>
//        ip_range.exists(new SubnetUtils(_).getInfo.isInRange(k)) && // 遍历每个ip范围形成一个独立区间，然后检测外部ip是否在该区间
//          !rel_ips.contains(k) // 已有关系中不存在
//    }
//
//    Ok(Json.toJson(filter_ip.toList))
//  }

  def save = Action{ implicit request =>
    relationForm.bindFromRequest.fold(
      formWithErrors => BadRequest(formWithErrors.errorsAsJson),
      relation =>
        Ok(Json.toJson(EnvironmentProjectRelHelper.add(relation)))
    )
  }

  def delete(id: Int) = Action {
    Ok(Json.toJson(EnvironmentProjectRelHelper.delete(id)))
  }

  def deletes(ids: String) = Action { implicit request =>
    val regular = """(^[0-9][0-9,]*)""".r
    ids match {
      case regular(_) =>
        Ok(Json.toJson(EnvironmentProjectRelHelper.deleteAll(ids.split(","))))
      case _ =>
        BadRequest
    }
  }

  implicit def recordWrite: Writes[Relation] = new Writes[Relation] {
    def writes(rel: Relation) = {
      Json.obj("relation"-> rel._1,"env" -> rel._2,"project" -> rel._3)
    }
  }

  type Relation = (EnvironmentProjectRel, String, String)

}