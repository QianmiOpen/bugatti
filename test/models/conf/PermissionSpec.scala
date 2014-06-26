package models.conf

import enums.FuncEnum
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
      PermissionHelper.create(Permission("of112", List(FuncEnum.project, FuncEnum.user))) === 1
    }

    "update success return 1" in new WithApplication {
      PermissionHelper.create(Permission("of112", List(FuncEnum.project, FuncEnum.user)))
      PermissionHelper.update("of112", Permission("of112", List(FuncEnum.task))) === 1
    }

    "find permission by user " in new WithApplication {
      val permission = Permission("of113", List(FuncEnum.project, FuncEnum.user))
      PermissionHelper.create(permission)
      PermissionHelper.findByJobNo("of113") === Some(permission)
    }

  }
}
