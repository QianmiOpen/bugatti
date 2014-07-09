package controllers.conf

import controllers.BaseController
import controllers.conf.ConfController._
import enums.FuncEnum
import models.conf._
import org.apache.commons.net.util.SubnetUtils
import play.api.Logger
import play.api.data._
import play.api.data.Forms._
import play.api.mvc._
import play.api.libs.json._
/**
 * 项目于环境关系配置
 * @author of546
 */
object RelationController extends BaseController {
  implicit val relationWrites = Json.writes[EnvironmentProjectRel]
  //  implicit val ipWrites = Json.writes[IP]
  //  implicit val relationFormWrites = Json.writes[EnvironmentProjectRelForm]
//  implicit val writer = new Writes[(String, String)] {
//    def writes(c: (String, String)): JsValue = {
//      Json.obj("key" -> c._1,"value" -> c._2)
//    }
//  }

  val relationForm = Form(
    mapping(
      "envId" -> number,
      "projectId" -> number,
      "ids" -> seq(number)
    )(EnvRelForm.apply)(EnvRelForm.unapply)
  )

  def index(envId: Option[Int], projectId: Option[Int], page: Int, pageSize: Int) = AuthAction(FuncEnum.relation) {
    val result = EnvironmentProjectRelHelper.all(envId, projectId, page, pageSize)
    Ok(Json.toJson(result))
  }

  def count(envId: Option[Int], projectId: Option[Int]) = AuthAction(FuncEnum.relation) {
    val result = EnvironmentProjectRelHelper.count(envId, projectId)
    Ok(Json.toJson(result))
  }

  def ips(envId: Int) = AuthAction(FuncEnum.relation) {
//    val env = EnvironmentHelper.findById(envId)
//    val ip_range = env.map(_.ipRange.map(_.split(";").toList).getOrElse(Seq.empty[String])).getOrElse(Seq.empty[String]) // 格式化
//    val rel_ips = EnvironmentProjectRelHelper.findIpsByEnvId(envId)
//    val filter_ip = SaltUtil.getMinionsJson.filter { // 正式环境可用ip集合
//      case (k, v) =>
//        ip_range.exists(new SubnetUtils(_).getInfo.isInRange(k)) && // 遍历每个ip范围形成一个独立区间，然后检测外部ip是否在该区间
//          !rel_ips.contains(k) // 已有关系中不存在
//    }

//    Ok(Json.toJson(filter_ip.toList))

    Ok(Json.toJson(EnvironmentProjectRelHelper.findIpsByEnvId(envId)))
  }

  def bind = AuthAction(FuncEnum.relation) { implicit request =>
    relationForm.bindFromRequest.fold(
      formWithErrors => BadRequest(formWithErrors.errorsAsJson),
      relation =>
        Ok(Json.toJson(EnvironmentProjectRelHelper.updateProjectId(relation)))
    )
  }

  def unbind(id: Int) = AuthAction(FuncEnum.relation) {
    Ok(Json.toJson(EnvironmentProjectRelHelper.unbind(id)))
  }

//  implicit def recordWrite: Writes[Relation] = new Writes[Relation] {
//    def writes(rel: Relation) = {
//      Json.obj("relation"-> rel._1,"env" -> rel._2,"project" -> rel._3)
//    }
//  }
//
//  type Relation = (EnvironmentProjectRel, String, String)

}