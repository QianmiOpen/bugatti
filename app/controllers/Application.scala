package controllers

import play.api._
import play.api.mvc._

object Application extends Controller with Security {

  lazy val siteDomain = app.configuration.getString("site.domain").getOrElse("ofpay.com")

  def index = Action { implicit request =>
    Ok(views.html.index(siteDomain))
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
        conf.routes.javascript.UserController.delete
      )
    ).as(JAVASCRIPT)
  }

}