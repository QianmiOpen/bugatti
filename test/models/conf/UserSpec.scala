package models.conf

import enums.RoleEnum
import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import play.api.test._

/**
 * 用户测试
 */
@RunWith(classOf[JUnitRunner])
class UserSpec extends Specification {

  "User Test" should {
    "insert success return 1" in new WithApplication {
      UserHelper.create(User("of556", Some("li"), Some(RoleEnum.user), Some(false), Some("2.2.1.1"), Some(DateTime.now()))) === 1
    }

    "delete success return 1" in new WithApplication {
      UserHelper.create(User("of556", Some("li"), Some(RoleEnum.user), Some(false), Some("2.2.1.1"), Some(DateTime.now()))) === 1
      UserHelper.delete("of556") === 1
    }

    "update success return 1" in new WithApplication {
      UserHelper.create(User("of556", Some("li"), Some(RoleEnum.user), Some(false), Some("2.2.1.1"), Some(DateTime.now())))
      UserHelper.update("of556", User("of526", Some("test"), Some(RoleEnum.user), Some(false), Some("2.2.1.1"), Some(DateTime.now()))) === 1
    }

    "find user" in new WithApplication {
      val user = User("of556", Some("li"), Some(RoleEnum.user), Some(false), Some("2.2.1.1"), Some(new DateTime(2012, 12, 4, 0, 0, 0, 0)))
      UserHelper.create(user)
      UserHelper.findByJobNo("of556") === Some(user)
    }

  }

}