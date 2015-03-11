package controllers

import enums.RoleEnum
import play.api.mvc._
import scala.concurrent.Future
import play.api.cache.Cache
import models.conf.{UserHelper, User}
import enums.RoleEnum._

case class RequestWithUser[A](val user: User, request: Request[A]) extends WrappedRequest[A](request)

/**
 * of546
 */
trait BaseController extends Controller with Security {

  val ALogger = play.Logger.of("ActionLog")

  def AuthAction[A](implicit role: Role = RoleEnum.user) = new ActionBuilder[({ type R[A] = RequestWithUser[A] })#R] {

    def invokeBlock[A](request: Request[A], block: (RequestWithUser[A]) => Future[Result]) = {
      authenticate(request, block)
    }
  }

  private def authenticate[A](request: Request[A], block: (RequestWithUser[A]) => Future[Result])(implicit role: Role) = {
    val maybeToken = request.headers.get(AuthTokenHeader).orElse(request.getQueryString(AuthTokenUrlKey))
    maybeToken flatMap { token =>
      Cache.getAs[String](token) map { jobNo =>
        UserHelper.findByJobNo(jobNo) match {
          case Some(user) if (user.role == RoleEnum.admin || role == user.role) => block(new RequestWithUser[A](user, request))
          case _ => Future.successful(Forbidden)
        }
      }
    } getOrElse Future.successful(Unauthorized)
  }

  // 页面返回
  import play.api.libs.json.Json

  val _Success = Json.obj("r" -> "ok")
  val _Fail = Json.obj("r" -> "error")
  val _Exist = Json.obj("r" -> "exist")
  val _None = Json.obj("r" -> "none")

  def resultUnique(data: String) = _Exist.+("u", Json.toJson(data))

}

