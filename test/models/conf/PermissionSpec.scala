package models.conf

import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import play.api.test._
/**
 * Created by li on 14-6-20.
 */
@RunWith(classOf[JUnitRunner])
class PermissionSpec extends Specification {

  "Permission Test" should {
    "insert success return 1" in new WithApplication {
      PermissionHelper.create(Permission("of111", Some("ceshi ..."))) === 1
    }


  }
}
