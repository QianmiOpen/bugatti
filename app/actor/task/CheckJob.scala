package actor.task

import akka.actor.Actor
import models.task.TaskQueue

/**
 * Created by jinwei on 13/7/14.
 */
object CheckJob {
  val jobStatus = Map.empty[String, TaskQueue]
}

class CheckJob extends Actor{
  def receive = {
    case _ =>
  }
}


