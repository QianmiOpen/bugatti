package models.conf

import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import play.api.test._
/**
 * Created by li on 14-6-20.
 */
@RunWith(classOf[JUnitRunner])
class SubPConfSpec extends Specification {
  "Sub project conf Test" should {
    "insert success return 1" in new WithApplication {
      SubPConfHelper.create(SubPConf(1, 1, 1, 1, "jdbc.properties", "/temp1/jdbc.properties", None)) === 1
    }
  }
}
