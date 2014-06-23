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
class SubPConfLogContentSpec extends Specification {

  "Sub project conf log content" should {
    "insert success return 1" in new WithApplication {
      SubPConfLogContentHelper.create(SubPConfLogContent(1, "测试")) === 1
    }
  }

}
