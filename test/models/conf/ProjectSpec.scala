package models.conf

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import play.api.test.WithApplication

/**
 * Created by li on 14-6-19.
 */
@RunWith(classOf[JUnitRunner])
object ProjectSpec extends Specification {

  "Project Test" should {
    "insert success return 1" in new WithApplication {
      ProjectHelper.create(Project(Some(2), "qm", 1, 1, Some("1.1.1"), Some(new DateTime(2012, 12, 4, 0, 0, 0, 0)))) === 1
    }
  }

}
