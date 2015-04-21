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
 * 网站入口
 * @author of546
 */
object Application extends Controller with Security with SystemSettingsService {

  lazy val siteDomain = app.configuration.getString("site.domain").getOrElse("ofpay.com")
  lazy val appVersion = app.configuration.getString("app.version").getOrElse("1.0")

  lazy val CacheExpiration = app.configuration.getInt("cache.expiration").getOrElse(60 /* seconds */ * 15 /* minutes */)

  implicit class ResultWithToken(result: Result) {

    def withToken(token: (String, String)): Result = {
      Cache.set(token._1, token._2, CacheExpiration)
      // todo 安全问题,angular读取cookie信息必须关闭httpOnly(防止js读取冲突)
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
        admin.routes.javascript.UserController.index,
        admin.routes.javascript.UserController.count,
        admin.routes.javascript.UserController.show,
        admin.routes.javascript.UserController.save,
        admin.routes.javascript.UserController.update,
        admin.routes.javascript.UserController.delete,

        // area
        admin.routes.javascript.AreaController.all,
        admin.routes.javascript.AreaController.get,
        admin.routes.javascript.AreaController.list,
        admin.routes.javascript.AreaController.save,
        admin.routes.javascript.AreaController.update,
        admin.routes.javascript.AreaController.delete,
        admin.routes.javascript.AreaController.refresh,

        // env
        admin.routes.javascript.EnvController.index,
        admin.routes.javascript.EnvController.all,
        admin.routes.javascript.EnvController.my,
        admin.routes.javascript.EnvController.count,
        admin.routes.javascript.EnvController.show,
        admin.routes.javascript.EnvController.save,
        admin.routes.javascript.EnvController.update,
        admin.routes.javascript.EnvController.delete,
        admin.routes.javascript.EnvController.allScriptVersion,

        // env member
        admin.routes.javascript.EnvController.member,
        admin.routes.javascript.EnvController.members,
        admin.routes.javascript.EnvController.saveMember,
        admin.routes.javascript.EnvController.updateMember,

        // spirit
        admin.routes.javascript.SpiritController.all,
        admin.routes.javascript.SpiritController.get,
        admin.routes.javascript.SpiritController.refresh,
        admin.routes.javascript.SpiritController.add,
        admin.routes.javascript.SpiritController.delete,
        admin.routes.javascript.SpiritController.update,

        // project
        admin.routes.javascript.ProjectController.show,
        admin.routes.javascript.ProjectController.all,
        admin.routes.javascript.ProjectController.my,
        admin.routes.javascript.ProjectController.allExceptSelf,
        admin.routes.javascript.ProjectController.save,
        admin.routes.javascript.ProjectController.index,
        admin.routes.javascript.ProjectController.count,
        admin.routes.javascript.ProjectController.update,
        admin.routes.javascript.ProjectController.delete,
        // project attribute
        admin.routes.javascript.ProjectController.atts,
        // project variable
        admin.routes.javascript.ProjectController.vars,
        admin.routes.javascript.ProjectController.showAuth,
        // project member
        admin.routes.javascript.ProjectController.member,
        admin.routes.javascript.ProjectController.members,
        admin.routes.javascript.ProjectController.saveMember,
        admin.routes.javascript.ProjectController.updateMember,

        //project cluster
        admin.routes.javascript.ProjectController.addCluster,
        admin.routes.javascript.ProjectController.removeCluster,

        // template
        admin.routes.javascript.TemplateController.all,
        admin.routes.javascript.TemplateController.save,
        admin.routes.javascript.TemplateController.delete,
        admin.routes.javascript.TemplateController.show,
        admin.routes.javascript.TemplateController.update,
        // template item
        admin.routes.javascript.TemplateController.items,
        admin.routes.javascript.TemplateController.itemAttrs,
        admin.routes.javascript.TemplateController.itemVars,

        // version
        admin.routes.javascript.VersionController.nexusVersions,
        admin.routes.javascript.VersionController.index,
        admin.routes.javascript.VersionController.count,
        admin.routes.javascript.VersionController.show,
        admin.routes.javascript.VersionController.delete,
        admin.routes.javascript.VersionController.save,
        admin.routes.javascript.VersionController.update,
        admin.routes.javascript.VersionController.all,
        admin.routes.javascript.VersionController.getVersions,

        // dependency
        admin.routes.javascript.DependencyController.show,
        admin.routes.javascript.DependencyController.removeDependency,
        admin.routes.javascript.DependencyController.addDependency,
        admin.routes.javascript.DependencyController.updateTemplateProject,

        // conf
        admin.routes.javascript.ConfController.all,
        admin.routes.javascript.ConfController.show,
        admin.routes.javascript.ConfController.save,
        admin.routes.javascript.ConfController.delete,
        admin.routes.javascript.ConfController.update,
        admin.routes.javascript.ConfController.copy,
        admin.routes.javascript.ConfController.completer,

        // script
        admin.routes.javascript.ScriptController.refresh,

        // conf logs
        admin.routes.javascript.ConfController.logs,
        admin.routes.javascript.ConfController.logsCount,
        admin.routes.javascript.ConfController.log,

        // relation
        admin.routes.javascript.RelationController.show,
        admin.routes.javascript.RelationController.index,
        admin.routes.javascript.RelationController.hosts,
        admin.routes.javascript.RelationController.count,
        admin.routes.javascript.RelationController.ips,
        admin.routes.javascript.RelationController.delete,
        admin.routes.javascript.RelationController.bind,
        admin.routes.javascript.RelationController.save,
        admin.routes.javascript.RelationController.saveBatch,
        admin.routes.javascript.RelationController.unbind,
        admin.routes.javascript.RelationController.update,

        // logs
        admin.routes.javascript.LogsController.search,
        admin.routes.javascript.LogsController.count,

        // system
        admin.routes.javascript.SystemController.index,
        admin.routes.javascript.SystemController.update,

        //task
        home.routes.javascript.TaskController.findLastTaskStatus,
        home.routes.javascript.TaskController.findLastStatus,
        home.routes.javascript.TaskController.joinProcess,
        home.routes.javascript.TaskController.createNewTaskQueue,
        home.routes.javascript.TaskController.getTemplates,
        home.routes.javascript.TaskController.logHeaderContent,
        home.routes.javascript.TaskController.removeTaskQueue,
        home.routes.javascript.TaskController.forceTerminate,
        home.routes.javascript.TaskController.findClusterByEnv_Project,
        home.routes.javascript.TaskController.findHisTasks,
        home.routes.javascript.TaskController.findCatalinaWSUrl,
        home.routes.javascript.TaskController.logReader

      )
    ).as(JAVASCRIPT)
  }

}