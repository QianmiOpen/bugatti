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
class ConfLogContentSpec extends Specification {

  "Sub project conf log content" should {
    "insert success return 1" in new WithApplication {
//      ConfLogContentHelper.create(ConfLogContent(1, "测试")) === 1
    }
  }

}
