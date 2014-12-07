package actor.git

import java.io.File

import akka.actor.ActorSystem
import akka.testkit.TestActorRef
import com.typesafe.config.ConfigFactory
import enums.RoleEnum
import models.conf.User
import org.specs2.mutable.Specification

/**
 * Created by mind on 12/7/14.
 */
class KeyGitActorTest extends Specification {
  implicit val actorSystem = ActorSystem("testsystem", ConfigFactory.parseString(
    """
      |akka {
      |  loggers = ["akka.testkit.TestEventListener"]
      |  loglevel = DEBUG
      |  actor {
      |    debug.receive = on
      |    log-remote-lifecycle-events = off
      |  }
      |}
    """.stripMargin))

  val of557KeyPrefix = "environment='SSH_USER=of557' ssh-rsa"
  val of558Key = "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQDGhpLNJnGRAMeHlWhm2lNDeCjaweFw93v1mWTME/3a23TJekMJkh84EGPSphFaawXAilnloriQYp4tztYpjY10YSLKSU16jGbt6Gnedrmob8tNZm/yeWMMJdLSATriCp7Eoa39ttwhQ7ROYaiY1X8huoxLNXXrYlDLs4K0NELV5sEA/lXXmygK2ma+krMskTfH2NOBPN57Q/ugb4y828yE8ZjpNRnFKrS0QqOUpicMwaousdh/f8AzIssYFTiKG37D1cINoNh0/gCcmTnxDAiXAicGC2tIVVLfRRrw/2qocNMofvWWBt3w9Wuncayt0Ak2BeVkYh5fh40ARkCOX4LB of557"
  val gitInfo = GitInfo("ssh://cicode@git.dev.ofpay.com:29418/cicode/salt-keys.git", new File("target/keys"))

  sequential

  "Test salt key git actor" should {
    val actorRef = TestActorRef(new KeyGitActor(gitInfo))
    val actor = actorRef.underlyingActor

    "test get rsa key without key" in {
      val key = actor._getKey(None, "of557")
      key must startWith(of557KeyPrefix)
      key.length > 300 should equalTo(true)
    }

    "test get key from file" in {
      val key = actor._getKeyFromFile("of557")
      println(key)
      key must startWith(of557KeyPrefix)
    }

    "create user without sshkey" in {
      actorRef ! AddUser(User("of557", "pengyi", RoleEnum.admin, true, false, None, None, None))
      "1" must equalTo("1")
    }

    "create user with sshkey" in {
      actorRef ! AddUser(User("of556", "pengyi1", RoleEnum.admin, true, false, None, None, Option(of558Key)))
      "1" must equalTo("1")
    }

    "update user with sshkey" in {
      actorRef ! UpdateUser(User("of557", "pengyi", RoleEnum.admin, true, false, None, None, Option(of558Key)))
      "1" must equalTo("1")
    }

    "delete user" in {
      actorRef ! DeleteUser("of556")
      actorRef ! DeleteUser("of557")

      "1" must equalTo("1")
    }

    "test add users" in {
      val users = Seq(User("of557", "pengyi", RoleEnum.admin, true, false, None, None, None),
        User("of558", "pengyi1", RoleEnum.admin, true, false, None, None, Option(of558Key)))

      actorRef ! AddUsers(users)

      "1" must equalTo("1")
    }

    "delete user" in {
      actorRef ! DeleteUser("of557")
      actorRef ! DeleteUser("of558")

      "1" must equalTo("1")
    }
  }
}
