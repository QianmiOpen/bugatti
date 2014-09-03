package controllers

import play.api.mvc._

import play.api.cache._

trait Security { self: Controller =>
  implicit val app: play.api.Application = play.api.Play.current

  val AuthTokenHeader = app.configuration.getString("auth_token_header").getOrElse("X-XSRF-TOKEN")
  val AuthTokenCookieKey = app.configuration.getString("auth_token.cookie.key").getOrElse("XSRF-TOKEN")
  val AuthTokenUrlKey = app.configuration.getString("auth_token.url.key").getOrElse("auth")

  def HasToken[A](p: BodyParser[A] = parse.anyContent)(f: String => String => Request[A] => Result): Action[A] =
    Action(p) { implicit request =>
      val maybeToken = request.headers.get(AuthTokenHeader).orElse(request.getQueryString(AuthTokenUrlKey))
      maybeToken flatMap { token =>
        Cache.getAs[String](token) map { jobNo =>
          f(token)(jobNo)(request)
        }
      } getOrElse Unauthorized("No Token")
    }

}