package controllers.conf

import actor.ActorUtils
import actor.git.ScriptGitActor._
import akka.pattern.ask
import akka.util.Timeout
import controllers.BaseController
import enums.FuncEnum
import play.api.libs.json.Json

import scala.concurrent.Await
import scala.concurrent.duration._

/**
 * 区域管理
 * @author of557
 */
object SystemController extends BaseController {

  implicit val timeout = Timeout(30 seconds)

  def buildTag = AuthAction(FuncEnum.system) { implicit request =>
    val result = ActorUtils.scriptGit ? BuildScriptTag()

    Await.result(result, 30 seconds)
    Ok(Json.obj("r" -> Json.toJson(0)))
  }

  def refresh = AuthAction(FuncEnum.system) { implicit request =>
    val result = ActorUtils.scriptGit ? ReloadFormulasTemplate

    Await.result(result, 30 seconds)
    Ok(Json.obj("r" -> Json.toJson(0)))
  }
}
