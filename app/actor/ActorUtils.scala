package actor

import actor.conf.ConfigureActor
import actor.git.{KeyGitActor, GitUtil, ScriptGitActor}
import actor.salt.{RefreshAreasActor, AreasActor}
import akka.actor.{ActorRef, Props, ActorSystem}
import play.api.Play

/**
 * Created by mind on 7/25/14.
 */
object ActorUtils {
  lazy val system = ActorSystem("bugatti")

  val app = Play.current

  val scriptGit: ActorRef = {
    if (GitUtil.init(app)) {
      system.actorOf(Props(classOf[ScriptGitActor], GitUtil.getGitInfo(app, "formulas")), name = "ScriptGit")
    } else {
      ActorRef.noSender
    }
  }

  val keyGit: ActorRef = {
    if (GitUtil.init(app)) {
      system.actorOf(Props(classOf[KeyGitActor], GitUtil.getGitInfo(app, "keys")), name = "KeyGit")
    } else {
      ActorRef.noSender
    }
  }

  val areas = system.actorOf(Props[AreasActor], name = "Areas")

  val areaRefresh = system.actorOf(Props[RefreshAreasActor], name = "AreaRefresh")

  val configuarActor = system.actorOf(Props[ConfigureActor], name = "configuarActor")
}
