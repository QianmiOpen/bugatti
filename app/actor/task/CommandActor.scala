package actor.task

import enums.TaskEnum.TaskStatus
import scala.language.postfixOps

/**
 * Created by jinwei on 14/7/14.
 */
object CommandActor{

  def command2Seq(command: String): Seq[String] = {
    var retSeq = Seq.empty[String]
    var bAppend = false
    command.split(" ").foreach { c =>
      if (bAppend) {
        retSeq = retSeq.dropRight(1) :+ (retSeq.last + s" $c")
      } else {
        retSeq = retSeq :+ c
      }
      if (c.contains("'") && !c.endsWith("'")) {
        bAppend = !bAppend
      }
    }

    retSeq.map(_.replace("'",""))
  }
}

case class TerminateCommands(status: TaskStatus, envId: Int, projectId: Int, clusterName: Option[String])

case class ConfCopyFailed(str: String)