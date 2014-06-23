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
class MemberSpec extends Specification {

  "Member Test" should {
    "insert success return 1" in new WithApplication {
      MemberHelper.create(Member(1, 1, LevelEnum.unsafe, "job_no")) === 1
    }

    "delete success return 1" in new WithApplication {
      MemberHelper.create(Member(1, 1, LevelEnum.unsafe, "job_no"))
      MemberHelper.delete(1) === 1
    }

    "update success return 1" in new WithApplication {
      val testLevel = LevelEnum.safe
      MemberHelper.create(Member(1, 1, LevelEnum.unsafe, "job_no"))
      val member = MemberHelper.findById(1).get
      MemberHelper.update(1, member.copy(level = testLevel))
      val member2 = MemberHelper.findById(1).get
      member2.level === testLevel
    }

  }
}
