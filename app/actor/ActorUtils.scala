package actor

import actor.git.ScriptGitActor
import akka.actor.{Props, ActorSystem}

/**
 * Created by mind on 7/25/14.
 */
object ActorUtils {
  lazy val system = ActorSystem("bugatti")

  lazy val scriptGitActor = system.actorOf(Props[ScriptGitActor])
}
