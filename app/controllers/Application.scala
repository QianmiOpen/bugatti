package controllers

import enums.RoleEnum
import models.conf._
import org.joda.time.DateTime
import play.api._
import play.api.mvc._
import play.api.cache._
import play.api.libs.json._
import org.pac4j.play.scala.ScalaController

import views._

object Application extends ScalaController with Security {

  lazy val siteDomain = app.configuration.getString("site.domain").getOrElse("ofpay.com")

  def index = Action { implicit request =>
    Ok(html.index(siteDomain))
  }

  lazy val CacheExpiration = app.configuration.getInt("cache.expiration").getOrElse(60 /* seconds */ * 15 /* minutes */)

  implicit class ResultWithToken(result: Result) {

    def withToken(token: (String, String)): Result = {
      Cache.set(token._1, token._2, CacheExpiration)
      result.withCookies(Cookie(AuthTokenCookieKey, token._1, None, domain = Some(siteDomain), httpOnly = false))
    }

    def discardingToken(token: String): Result = {
      Cache.remove(token)
      result.discardingCookies(DiscardingCookie(name = AuthTokenCookieKey, domain = Some(siteDomain)))
    }
  }

  def login = RequiresAuthentication("CasClient") { profile =>
    Action { implicit request =>
      UserHelper.findByJobNo(profile.getId) match {
        case Some(user) if user.locked =>
          Locked(html.template.ldap_callback_locked.render(siteDomain))
        case Some(user) if user.role == RoleEnum.admin =>
          UserHelper.update(user.jobNo, user.copy(lastIp = Some(request.remoteAddress), lastVisit = Some(DateTime.now)))
          val token = java.util.UUID.randomUUID().toString
          Ok(html.template.ldap_callback.render(siteDomain)).withToken(token -> user.jobNo)
        case Some(user) if user.role == RoleEnum.user =>
          PermissionHelper.findByJobNo(user.jobNo) match {
            case Some(p) =>
              UserHelper.update(user.jobNo, user.copy(lastIp = Some(request.remoteAddress), lastVisit = Some(DateTime.now)))
              val token = java.util.UUID.randomUUID().toString
              Ok(html.template.ldap_callback.render(siteDomain)).withToken(token -> user.jobNo)
            case None =>
              Forbidden(html.template.ldap_callback_forbidden.render(siteDomain))
          }
        case None =>
          NotFound(html.template.ldap_callback_none.render(siteDomain))
      }
    }
  }

  def logout = Action { implicit request =>
    request.headers.get(AuthTokenHeader) map { token =>
      Ok.discardingToken(token)
    } getOrElse BadRequest (Json.obj("r" -> "No Token"))
  }

  def ping = HasToken() { token => jobNo => implicit request =>
    UserHelper.findByJobNo(jobNo) map { user =>
      val ps = PermissionHelper.findByJobNo(jobNo) match {
        case Some(p) =>
          p.functions
        case None =>
          Seq.empty
      }
      Ok(Json.obj("jobNo" -> jobNo, "role" -> user.role, "permissions" -> ps)).withToken(token -> jobNo)
    } getOrElse NotFound (Json.obj("r" -> "User Not Found"))
  }


  def javascriptRoutes = Action { implicit request =>
    import controllers._
    Ok(
      Routes.javascriptRouter("PlayRoutes")(
        // user
        conf.routes.javascript.UserController.index,
        conf.routes.javascript.UserController.count,
        conf.routes.javascript.UserController.show,
        conf.routes.javascript.UserController.permissions,
        conf.routes.javascript.UserController.save,
        conf.routes.javascript.UserController.update,
        conf.routes.javascript.UserController.delete,

        // area
        conf.routes.javascript.AreaController.all,
        conf.routes.javascript.AreaController.get,
        conf.routes.javascript.AreaController.save,
        conf.routes.javascript.AreaController.update,
        conf.routes.javascript.AreaController.delete,
        conf.routes.javascript.AreaController.refresh,

        // env
        conf.routes.javascript.EnvController.index,
        conf.routes.javascript.EnvController.all,
        conf.routes.javascript.EnvController.count,
        conf.routes.javascript.EnvController.show,
        conf.routes.javascript.EnvController.save,
        conf.routes.javascript.EnvController.update,
        conf.routes.javascript.EnvController.delete,
        conf.routes.javascript.EnvController.showAuth,

        // project
        conf.routes.javascript.ProjectController.show,
        conf.routes.javascript.ProjectController.all,
        conf.routes.javascript.ProjectController.save,
        conf.routes.javascript.ProjectController.index,
        conf.routes.javascript.ProjectController.count,
        conf.routes.javascript.ProjectController.update,
        conf.routes.javascript.ProjectController.delete,
        conf.routes.javascript.ProjectController.atts,
        conf.routes.javascript.ProjectController.showAuth,
        // project member
        conf.routes.javascript.ProjectController.member,
        conf.routes.javascript.ProjectController.members,
        conf.routes.javascript.ProjectController.saveMember,
        conf.routes.javascript.ProjectController.updateMember,

        // template
        conf.routes.javascript.TemplateController.all,
        conf.routes.javascript.TemplateController.save,
        conf.routes.javascript.TemplateController.delete,
        conf.routes.javascript.TemplateController.show,
        conf.routes.javascript.TemplateController.update,
        // template item
        conf.routes.javascript.TemplateController.items,

        // version
        conf.routes.javascript.VersionController.index,
        conf.routes.javascript.VersionController.count,
        conf.routes.javascript.VersionController.show,
        conf.routes.javascript.VersionController.delete,
        conf.routes.javascript.VersionController.save,
        conf.routes.javascript.VersionController.update,
        conf.routes.javascript.VersionController.all,

        // conf
        conf.routes.javascript.ConfController.all,
        conf.routes.javascript.ConfController.show,
        conf.routes.javascript.ConfController.save,
        conf.routes.javascript.ConfController.delete,
        conf.routes.javascript.ConfController.update,
        conf.routes.javascript.ConfController.copy,

        // conf logs
        conf.routes.javascript.ConfController.logs,
        conf.routes.javascript.ConfController.logsCount,
        conf.routes.javascript.ConfController.log,

        // relation
        conf.routes.javascript.RelationController.index,
        conf.routes.javascript.RelationController.count,
        conf.routes.javascript.RelationController.ips,
        conf.routes.javascript.RelationController.bind,
        conf.routes.javascript.RelationController.unbind,

        //task
        task.routes.javascript.TaskController.findLastTaskStatus,
        task.routes.javascript.TaskController.joinProcess,
        task.routes.javascript.TaskController.getVersions,
        task.routes.javascript.TaskController.getNexusVersions,
        task.routes.javascript.TaskController.createNewTaskQueue,
        task.routes.javascript.TaskController.getTemplates,
        task.routes.javascript.TaskController.taskLog,
        task.routes.javascript.TaskController.taskLogFirst,
        task.routes.javascript.TaskController.removeTaskQueue
      )
    ).as(JAVASCRIPT)
  }

}