package controllers.conf

import actor.ActorUtils
import actor.git.ScriptGitActor._
import actor.salt.{ConnectedSpirits, RefreshSpiritsActor}
import akka.pattern.ask
import akka.util.Timeout
import controllers.BaseController
import enums.FuncEnum
import play.api.libs.json.Json

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

/**
 * 区域管理
 * @author of557
 */
object SystemController extends BaseController {

  implicit val timeout = Timeout(30 seconds)

  def refresh = AuthAction(FuncEnum.system) { implicit request =>
    val result = ActorUtils.scriptGit ? ReloadFormulasTemplate

    val future = ActorUtils.spirits ? ConnectedSpirits
    val spiritIds = Await.result(future, timeout.duration).asInstanceOf[Seq[Int]]

    ALogger.debug(s"Auto refresh server files, spiritIds: ${spiritIds}")

    spiritIds.foreach { spiritId =>
      ActorUtils.spiritsRefresh ! RefreshSpiritsActor.RefreshFiles(spiritId)
    }

    Await.result(result, 30 seconds)
    Ok(Json.toJson(0))
  }
}
