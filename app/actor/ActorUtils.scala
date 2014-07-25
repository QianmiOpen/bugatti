package actor

import actor.git.FormulasActor
import akka.actor.{Props, ActorSystem}

/**
 * Created by mind on 7/25/14.
 */
object ActorUtils {
  lazy val system = ActorSystem("bugatti")

  lazy val formulasActor = system.actorOf(Props[FormulasActor])
}
