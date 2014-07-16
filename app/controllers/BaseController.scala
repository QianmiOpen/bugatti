package controllers

import enums.RoleEnum
import play.api.mvc._
import scala.concurrent.Future
import play.api.cache.Cache
import models.conf.{UserHelper, User, PermissionHelper}
import enums.FuncEnum._

class RequestWithUser[A](val user: User, request: Request[A]) extends WrappedRequest[A](request)

trait BaseController extends Controller with Security {

  val ALogger = play.Logger.of("action_log")

  def AuthAction(func: Func) = new ActionBuilder[RequestWithUser] {
    def invokeBlock[A](request: Request[A], block: (RequestWithUser[A]) => Future[SimpleResult]) = {
      // todo resem HasToken rework
      val maybeToken = request.headers.get(AuthTokenHeader).orElse(request.getQueryString(AuthTokenUrlKey))
      maybeToken flatMap { token =>
        Cache.getAs[String](token) map { jobNo =>
          UserHelper.findByJobNo(jobNo) match {
            case Some(user) if user.role == RoleEnum.admin =>
              block(new RequestWithUser(user, request))
            case Some(user) if user.role == RoleEnum.user =>
              findPermission(jobNo, func) match {
                case Some(true) =>
                  block(new RequestWithUser(user, request))
                case _ =>
                  Future.successful(Forbidden)
              }
            case None =>
              Future.successful(NotFound)
          }
        }
      } getOrElse Future.successful(Unauthorized)
    }

    def findPermission(jobNo: String, func: Func): Option[Boolean] = {
      PermissionHelper.findByJobNo(jobNo) map { p =>
        p.functions.exists(_ == func)
      }
    }

  }

}
