package controllers

import java.io.File

import models.conf._
import org.joda.time.DateTime
import play.api._
import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc._
import play.api.cache._
import play.api.libs.json._
import service.SystemSettingsService
import utils.ConfHelp

import views._

/**
 * of546
 */
object Application extends Controller with Security with SystemSettingsService {

  lazy val siteDomain = app.configuration.getString("site.domain").getOrElse("ofpay.com")
  lazy val appVersion = app.configuration.getString("app.version").getOrElse("1.0")

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

  case class LoginForm(userName: String, password: String)
  val loginForm = Form(
    mapping(
    "userName" -> nonEmptyText,
    "password" -> nonEmptyText
    )(LoginForm.apply)(LoginForm.unapply)
  )

  def index = Action { implicit request =>
    Ok(html.index(siteDomain, appVersion))
  }

  def login = Action { implicit request =>
    loginForm.bindFromRequest.fold(
      formWithErrors => BadRequest(formWithErrors.errorsAsJson),
      form => {
        UserHelper.authenticate(loadSystemSettings(), form.userName, form.password) match {
          case Some(user) if user.locked => Locked
          case Some(user) =>
            UserHelper.update(user.jobNo, user.copy(lastIp = Some(request.remoteAddress), lastVisit = Some(DateTime.now)))
            Ok(Json.obj("jobNo" -> user.jobNo, "role" -> user.role)).withToken(java.util.UUID.randomUUID().toString -> user.jobNo)
          case _ => Forbidden
        }
      }
    )
  }

  def logout = Action { implicit request =>
    request.headers.get(AuthTokenHeader) map { token =>
      Ok.discardingToken(token)
    } getOrElse BadRequest("No Token")
  }

  def ping = HasToken() { token => jobNo => implicit request =>
    UserHelper.findByJobNo(jobNo) map { user =>
      Ok(Json.obj("jobNo" -> jobNo, "role" -> user.role)).withToken(token -> jobNo)
    } getOrElse NotFound("User Not Found")
  }

  def pkgs(pkg: String) = Action { implicit request =>
    Logger.info(s"Download file: ${pkg}")
    val file = new File(s"${ConfHelp.confPath}/${pkg}")

    if (file.exists() && file.isFile && (pkg.endsWith(".tar.gz") || pkg.endsWith(".md5"))) {
      Ok.sendFile(content = file, fileName = _ => pkg.split("/").last)
    }
    else {
      Ok
    }
  }

  def javascriptRoutes = Action { implicit request =>
    import controllers._
    Ok(
      Routes.javascriptRouter("PlayRoutes")(
        // user
        conf.routes.javascript.UserController.index,
        conf.routes.javascript.UserController.count,
        conf.routes.javascript.UserController.show,
        conf.routes.javascript.UserController.save,
        conf.routes.javascript.UserController.update,
        conf.routes.javascript.UserController.delete,

        // area
        conf.routes.javascript.AreaController.all,
        conf.routes.javascript.AreaController.get,
        conf.routes.javascript.AreaController.list,
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
        conf.routes.javascript.EnvController.allScriptVersion,
        // env member
        conf.routes.javascript.EnvController.member,
        conf.routes.javascript.EnvController.members,
        conf.routes.javascript.EnvController.saveMember,
        conf.routes.javascript.EnvController.deleteMember,

        // spirit
        conf.routes.javascript.SpiritController.all,
        conf.routes.javascript.SpiritController.get,
        conf.routes.javascript.SpiritController.refresh,
        conf.routes.javascript.SpiritController.add,
        conf.routes.javascript.SpiritController.delete,
        conf.routes.javascript.SpiritController.update,

        // project
        conf.routes.javascript.ProjectController.show,
        conf.routes.javascript.ProjectController.all,
        conf.routes.javascript.ProjectController.allExceptSelf,
        conf.routes.javascript.ProjectController.save,
        conf.routes.javascript.ProjectController.index,
        conf.routes.javascript.ProjectController.count,
        conf.routes.javascript.ProjectController.update,
        conf.routes.javascript.ProjectController.delete,
        // project attribute
        conf.routes.javascript.ProjectController.atts,
        // project variable
        conf.routes.javascript.ProjectController.vars,
        conf.routes.javascript.ProjectController.showAuth,
        // project member
        conf.routes.javascript.ProjectController.member,
        conf.routes.javascript.ProjectController.members,
        conf.routes.javascript.ProjectController.saveMember,
        conf.routes.javascript.ProjectController.updateMember,

        //project cluster
        conf.routes.javascript.ProjectController.addCluster,
        conf.routes.javascript.ProjectController.removeCluster,

        // template
        conf.routes.javascript.TemplateController.all,
        conf.routes.javascript.TemplateController.save,
        conf.routes.javascript.TemplateController.delete,
        conf.routes.javascript.TemplateController.show,
        conf.routes.javascript.TemplateController.update,
        // template item
        conf.routes.javascript.TemplateController.items,
        conf.routes.javascript.TemplateController.itemAttrs,
        conf.routes.javascript.TemplateController.itemVars,

        // version
        conf.routes.javascript.VersionController.nexusVersions,
        conf.routes.javascript.VersionController.index,
        conf.routes.javascript.VersionController.count,
        conf.routes.javascript.VersionController.show,
        conf.routes.javascript.VersionController.delete,
        conf.routes.javascript.VersionController.save,
        conf.routes.javascript.VersionController.update,
        conf.routes.javascript.VersionController.all,
        conf.routes.javascript.VersionController.getVersions,

        // dependency
        conf.routes.javascript.DependencyController.show,
        conf.routes.javascript.DependencyController.removeDependency,
        conf.routes.javascript.DependencyController.addDependency,
        conf.routes.javascript.DependencyController.updateTemplateProject,

        // conf
        conf.routes.javascript.ConfController.all,
        conf.routes.javascript.ConfController.show,
        conf.routes.javascript.ConfController.save,
        conf.routes.javascript.ConfController.delete,
        conf.routes.javascript.ConfController.update,
        conf.routes.javascript.ConfController.copy,
        conf.routes.javascript.ConfController.completer,

        // system
        conf.routes.javascript.SystemController.refresh,

        // conf logs
        conf.routes.javascript.ConfController.logs,
        conf.routes.javascript.ConfController.logsCount,
        conf.routes.javascript.ConfController.log,

        // relation
        conf.routes.javascript.RelationController.show,
        conf.routes.javascript.RelationController.index,
        conf.routes.javascript.RelationController.hosts,
        conf.routes.javascript.RelationController.count,
        conf.routes.javascript.RelationController.ips,
        conf.routes.javascript.RelationController.bind,
        conf.routes.javascript.RelationController.unbind,
        conf.routes.javascript.RelationController.update,

        //task
        task.routes.javascript.TaskController.findLastTaskStatus,
        task.routes.javascript.TaskController.findLastStatus,
        task.routes.javascript.TaskController.joinProcess,
        task.routes.javascript.TaskController.createNewTaskQueue,
        task.routes.javascript.TaskController.getTemplates,
        task.routes.javascript.TaskController.logHeaderContent,
        task.routes.javascript.TaskController.removeTaskQueue,
        task.routes.javascript.TaskController.forceTerminate,
        task.routes.javascript.TaskController.findClusterByEnv_Project,
        task.routes.javascript.TaskController.findHisTasks,
        task.routes.javascript.TaskController.findCatalinaWSUrl,
        task.routes.javascript.TaskController.logReader,

        // logs
        logs.routes.javascript.LogsController.search,
        logs.routes.javascript.LogsController.count
      )
    ).as(JAVASCRIPT)
  }

}