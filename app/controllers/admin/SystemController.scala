package controllers.admin

import controllers.BaseController
import enums.RoleEnum
import play.api.data.Forms._
import play.api.data._
import play.api.libs.json.Json
import service.SystemSettingsService
import service.SystemSettingsService.{SystemSettings, Ldap}

/**
 * 系统配置
 *
 * @author of546
 */
object SystemController extends BaseController with SystemSettingsService {

  implicit val ldapWrites = Json.writes[Ldap]
  implicit val systemWrites = Json.writes[SystemSettings]

  val systemForm = Form(
    mapping(
      "ldapAuthentication" -> boolean,
      "ldap" -> optional(
        mapping(
          "host" -> nonEmptyText(maxLength = 1024),
          "port" -> optional(number),
          "bindDN" -> optional(text),
          "bindPassword" -> optional(text),
          "baseDN" -> nonEmptyText,
          "userNameAttribute" -> nonEmptyText,
          "additionalFilterCondition" -> optional(text),
          "fullNameAttribute" -> optional(text),
          "mailAttribute" -> optional(text),
          "tls" -> optional(boolean),
          "keystore" -> optional(text)
        )(Ldap.apply)(Ldap.unapply)
      )
    )(SystemSettings.apply)(SystemSettings.unapply)
  )

  def index() = AuthAction(RoleEnum.admin) { implicit request =>
    Ok(Json.toJson(loadSystemSettings()))
  }

  def update() = AuthAction(RoleEnum.admin) { implicit request =>
    systemForm.bindFromRequest.fold(
      formWithErrors => BadRequest(formWithErrors.errorsAsJson),
      _systemForm => {
        saveSystemSettings(_systemForm)
        Ok(_Success)
      }
    )
  }

}
