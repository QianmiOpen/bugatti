package actor.task

import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

/**
 * Created by mind on 11/15/14.
 */
@RunWith(classOf[JUnitRunner])
class CommandActorTest extends Specification {

  "scala process" should {
    "单引号测试" in {
     val ret = CommandActor.command2Seq("salt lin-salt-8-6.localdomain state.sls java.install pillar='{java: {version: jdk7}}'")

     ret mustEqual Seq("salt", "lin-salt-8-6.localdomain", "state.sls", "java.install", "pillar={java: {version: jdk7}}")
    }
  }
}
