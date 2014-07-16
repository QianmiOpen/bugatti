package actor.task

import akka.actor.{Props, Actor}

/**
 * Created by jinwei on 13/7/14.
 */
class CheckJob extends Actor{
  import context._
  def receive = {
    case JobStatus(commandSeq, taskId, envId, projectId, versionId, order) => {
      val saltExecute = actorOf(Props[SaltExecute], "saltExecute")
      saltExecute ! SaltCheck(commandSeq, taskId, envId, projectId, versionId, order)
    }
  }
}

case class JobStatus(commandSeq: Seq[String], taskId: Int, envId: Int, projectId: Int, versionId: Option[Int], order: Int)

