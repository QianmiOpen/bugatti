package actor.salt

import akka.actor.{Props, ActorSystem}
import akka.testkit.{ImplicitSender, TestKit}
import com.qianmi.bugatti.actors.{ListHostsResult, ListHosts}
import com.typesafe.config.ConfigFactory
import models.conf.Spirit
import org.scalatest.BeforeAndAfterAll
import org.scalatest.Matchers
import org.scalatest.WordSpecLike
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created by mind on 1/9/15.
 */
class SpiritsActorTest(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
with WordSpecLike with Matchers with BeforeAndAfterAll {
  def this() = this(ActorSystem("SpiritsActorTest", ConfigFactory.parseString(
    """
      | akka {
      |  loggers = ["akka.testkit.TestEventListener"]
      |  loglevel = DEBUG
      |  actor {
      |    debug {
      |      receive = on
      |      autoreceive = off
      |      lifecycle = off
      |      fsm = on
      |    }
      |
      |    provider = "akka.remote.RemoteActorRefProvider"
      |
      |    log-remote-lifecycle-events = off
      |  }
      |  remote {
      |    netty.tcp {
      |      hostname = "172.19.3.94"
      |      port = 0
      |      send-buffer-size = 4m
      |      receive-buffer-size = 4m
      |      maximum-frame-size = 2m
      |    }
      |  }
      |}
      |
    """.stripMargin)))

  val spirits = system.actorOf(Props[SpiritsActor], name = "Spirits")

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  "Salt spirit actor test" must {
    "spirit up" in {
      spirits ! AddSpirit(Spirit(Option(1), "test64.1", "172.19.64.1"))

      system.scheduler.schedule(3 second, 1 second, spirits, RemoteSpirit(1, ListHosts()))

      while (true) {
        println(receiveOne(3 seconds))
      }
    }
  }
}
