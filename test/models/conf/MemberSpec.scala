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
      ProjectMemberHelper.create(ProjectMember(Some(1), 1, LevelEnum.unsafe, "job_no")) === 1
    }

    "delete success return 1" in new WithApplication {
      ProjectMemberHelper.create(ProjectMember(Some(1), 1, LevelEnum.unsafe, "job_no"))
      ProjectMemberHelper.delete(1) === 1
    }

    "update success return 1" in new WithApplication {
      val testLevel = LevelEnum.safe
      ProjectMemberHelper.create(ProjectMember(Some(1), 1, LevelEnum.unsafe, "job_no"))
      val member = ProjectMemberHelper.findById(1).get
      ProjectMemberHelper.update(1, member.copy(level = testLevel))
      val member2 = ProjectMemberHelper.findById(1).get
      member2.level === testLevel
    }

  }
}
