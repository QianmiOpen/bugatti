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
class SubPConfLogSpec extends Specification {

  "Sub project conf log" should {
    "insert success return 1"  in new WithApplication {
      SubPConfLogHelper.create(SubPConfLog(1, 1, 1, 1, "jdbc.properties", "/temp1/jdbc.properties", Some("remark"), Some(DateTime.now()))) === 1
    }
  }

}
