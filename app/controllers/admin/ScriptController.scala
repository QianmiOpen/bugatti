package controllers.admin

import actor.ActorUtils
import actor.git.ScriptGitActor._
import actor.salt.{ConnectedSpirits, RefreshSpiritsActor}
import akka.pattern.ask
import akka.util.Timeout
import controllers.BaseController
import enums.{ModEnum, RoleEnum}
import play.api.libs.json.Json

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

/**
 * 脚本管理
 * @author of557
 */
object ScriptController extends BaseController {

  implicit val timeout = Timeout(30 seconds)

  def refresh = AuthAction(RoleEnum.admin) { implicit request =>
    val result = ActorUtils.scriptGit ? ReloadFormulasTemplate

    val future = ActorUtils.spirits ? ConnectedSpirits
    val spiritIds = Await.result(future, timeout.duration).asInstanceOf[Seq[Int]]

    ALogger.debug(s"Auto refresh server files, spiritIds: ${spiritIds}")

    spiritIds.foreach { spiritId =>
      ActorUtils.spiritsRefresh ! RefreshSpiritsActor.RefreshFiles(spiritId)
    }

    Await.result(result, 30 seconds)

    ALogger.info(Json.obj("mod" -> ModEnum.script.toString, "user" -> request.user.jobNo,
      "ip" -> request.remoteAddress, "msg" -> "脚本刷新", "data" -> Json.toJson("")).toString)

    Ok(Json.toJson(0))
  }

}
