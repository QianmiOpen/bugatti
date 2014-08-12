package actor

import actor.conf.ConfigureActor
import actor.git.ScriptGitActor
import actor.salt.{RefreshAreasActor, AreasActor}
import akka.actor.{Props, ActorSystem}

/**
 * Created by mind on 7/25/14.
 */
object ActorUtils {
  lazy val system = ActorSystem("bugatti")

  val scriptGit = system.actorOf(Props[ScriptGitActor], name = "ScriptGit")

  val areas = system.actorOf(Props[AreasActor], name = "Areas")

  val areaRefresh = system.actorOf(Props[RefreshAreasActor], name = "AreaRefresh")

  val configuarActor = system.actorOf(Props[ConfigureActor], name = "configuarActor")
}
