package actor.task

import enums.TaskEnum
import models.task.TaskQueue
import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import play.api.test._

/**
 * Created by jinwei on 16/7/14.
 */
@RunWith(classOf[JUnitRunner])
class MyActorSpec extends Specification {

  "Actors should run like this" should {
    "actor begin" in new WithApplication {
      val seq = Seq(
      TaskQueue(Option(1), 1, 1, Option(1), 2, TaskEnum.TaskWait, new DateTime(), None, None, 1),
      TaskQueue(Option(2), 2, 1, Option(1), 2, TaskEnum.TaskWait, new DateTime(), None, None, 1),
      TaskQueue(Option(3), 3, 1, Option(1), 2, TaskEnum.TaskWait, new DateTime(), None, None, 1),
      TaskQueue(Option(4), 4, 1, Option(1), 2, TaskEnum.TaskWait, new DateTime(), None, None, 1),
      TaskQueue(Option(5), 5, 1, Option(1), 2, TaskEnum.TaskWait, new DateTime(), None, None, 1),
      TaskQueue(Option(6), 6, 1, Option(1), 2, TaskEnum.TaskWait, new DateTime(), None, None, 1),
      TaskQueue(Option(7), 7, 1, Option(1), 2, TaskEnum.TaskWait, new DateTime(), None, None, 1),
      TaskQueue(Option(8), 8, 1, Option(1), 2, TaskEnum.TaskWait, new DateTime(), None, None, 1),
      TaskQueue(Option(9), 9, 1, Option(1), 2, TaskEnum.TaskWait, new DateTime(), None, None, 1),
      TaskQueue(Option(10), 10, 1, Option(1), 2, TaskEnum.TaskWait, new DateTime(), None, None, 1),
      TaskQueue(Option(11), 11, 1, Option(1), 2, TaskEnum.TaskWait, new DateTime(), None, None, 1),
      TaskQueue(Option(12), 12, 1, Option(1), 2, TaskEnum.TaskWait, new DateTime(), None, None, 1),
      TaskQueue(Option(13), 13, 1, Option(1), 2, TaskEnum.TaskWait, new DateTime(), None, None, 1),
      TaskQueue(Option(14), 14, 1, Option(1), 2, TaskEnum.TaskWait, new DateTime(), None, None, 1),
      TaskQueue(Option(15), 15, 1, Option(1), 2, TaskEnum.TaskWait, new DateTime(), None, None, 1),
      TaskQueue(Option(16), 16, 1, Option(1), 2, TaskEnum.TaskWait, new DateTime(), None, None, 1),
      TaskQueue(Option(17), 17, 1, Option(1), 2, TaskEnum.TaskWait, new DateTime(), None, None, 1),
      TaskQueue(Option(18), 18, 1, Option(1), 2, TaskEnum.TaskWait, new DateTime(), None, None, 1),
      TaskQueue(Option(19), 19, 1, Option(1), 2, TaskEnum.TaskWait, new DateTime(), None, None, 1),
      TaskQueue(Option(20), 20, 1, Option(1), 2, TaskEnum.TaskWait, new DateTime(), None, None, 1),
      TaskQueue(Option(21), 21, 1, Option(1), 2, TaskEnum.TaskWait, new DateTime(), None, None, 1),
      TaskQueue(Option(22), 1, 1, Option(1), 2, TaskEnum.TaskWait, new DateTime(), None, None, 1)
      )
      seq.foreach{
        t => {
          MyActor.createNewTask(t)
        }
      }
    }
  }
}
