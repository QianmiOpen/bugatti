package models.conf

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import play.api.test._
/**
 * Created by li on 14-6-20.
 */
@RunWith(classOf[JUnitRunner])
class VersionSpec extends Specification {

  "Sub project" should {
    "insert success return 1" in new WithApplication {
      VersionHelper.create(Version(1, 1, "1.1.1", Some(DateTime.now()))) === 1
    }
  }

}
