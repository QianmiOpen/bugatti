package models.conf

import enums.LevelEnum
import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import play.api.test._
/**
 * Created by li on 14-6-20.
 */
@RunWith(classOf[JUnitRunner])
class EnvironmentSpec extends Specification {

  "Environment Test" in new WithApplication {
    "insert success return 1" in new WithApplication {
//      EnvironmentHelper.create(Environment(Some(1), "测试", Some("备注"), None, None, LevelEnum.unsafe))
    }
  }

}
