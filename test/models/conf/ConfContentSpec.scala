package models.conf

import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import play.api.test._
/**
 * Created by li on 14-6-20.
 */
@RunWith(classOf[JUnitRunner])
class ConfContentSpec extends Specification {
  "Sub project conf content Test" should {
    "insert success return 1" in new WithApplication {
//      ConfContentHelper.create(ConfContent(1, "测试。。")) === 1
    }
  }
}
