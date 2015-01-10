package controllers.conf

import actor.ActorUtils
import actor.git.ScriptGitActor._
import actor.salt.{RefreshFiles, ConnectedSpirits, RefreshHosts}
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

//    val future = ActorUtils.areas ? ConnectedAreas
//    val areaIds = Await.result(future, timeout.duration).asInstanceOf[Seq[Int]]
//
//    ALogger.debug(s"Auto refresh server files, areaIds: ${areaIds}")
//
//    areaIds.foreach { areaId =>
//      ActorUtils.areaRefresh ! RefreshFiles(areaId)
//    }

    // TODO: 刷新所有spirit
    Await.result(result, 30 seconds)
    Ok(Json.toJson(0))
  }
}
