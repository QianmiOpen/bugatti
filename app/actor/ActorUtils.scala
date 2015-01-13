package actor

import actor.conf.ConfigureActor
import actor.git.{KeyGitActor, GitUtil, ScriptGitActor}
import actor.salt.{RefreshSpiritsActor, SpiritsActor}
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

  val spirits = system.actorOf(Props[SpiritsActor], name = "Spirits")

  val spiritsRefresh = system.actorOf(Props[RefreshSpiritsActor], name = "SpiritRefresh")

  val configuarActor = system.actorOf(Props[ConfigureActor], name = "configuarActor")
}
