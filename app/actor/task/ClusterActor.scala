package actor.task

import akka.actor.SupervisorStrategy.Escalate
import akka.actor.{OneForOneStrategy, Props, ActorLogging, Actor}
import models.conf.Host
import models.task.{TaskQueue, TaskCommand, TemplateActionStep}
import utils.ProjectTask_v

/**
 * Created by jinwei on 21/8/14.
 */
class ClusterActor extends Actor with ActorLogging{

  override val supervisorStrategy = OneForOneStrategy() {
    case e: Exception =>
      log.error(s"${self} catch ${sender} exception: ${e.getStackTrace}")
      Escalate
  }

  def receive = {
    case gcc: GenerateClusterCommands => {
      context.actorOf(Props(classOf[EngineActor], 3)) ! ReplaceCommand(gcc.taskObj, gcc.templateStep, gcc.hostname, gcc.tq, gcc.hosts, gcc.hostsIndex)
    }
    case success: SuccessReplaceCommand => {
      context.parent ! SuccessReplaceCommand(success.commandList, success.tq, success.templateStep, success.hosts, success.hostsIndex, success.taskObj)
      context.stop(self)
    }
    case error: ErrorReplaceCommand => {
      log.info(s"cluster errorCommand")
      context.parent ! error
      context.stop(self)
    }
    case gcc: GenerateClusterConfs => {
      context.actorOf(Props(classOf[EngineActor], 15)) ! ReplaceConfigure(gcc.taskObj, gcc.hostname, gcc.order)
    }
    case successConf: SuccessReplaceConf => {
      context.parent ! successConf
    }
    case errorConf: ErrorReplaceConf => {
      context.parent ! errorConf
      context.stop(self)
    }
    case timeout: TimeoutReplace => {
      context.parent ! TimeoutReplace(timeout.key)
      context.stop(self)
    }
    case _ =>
  }
}

case class GenerateClusterCommands(taskId: Int, taskObj: ProjectTask_v, templateStep: Seq[TemplateActionStep], hostname: String, tq: TaskQueue, hosts: Seq[Host], hostsIndex: Int)
case class SuccessReplaceCommand(commandList: Seq[TaskCommand], tq: TaskQueue, templateStep: Seq[TemplateActionStep], hosts: Seq[Host], hostsIndex: Int, taskObj: ProjectTask_v)
case class ErrorReplaceCommand(keys: String, tq: TaskQueue, templateStep: Seq[TemplateActionStep], hosts: Seq[Host], hostsIndex: Int, taskObj: ProjectTask_v)

case class GenerateClusterConfs(envId: Int, projectId: Int, versionId: Int, taskObj: ProjectTask_v, hostname: String, order: Int)
case class SuccessReplaceConf(taskId: Int, envId: Int, projectId: Int, versionId: Option[Int], order: Int)
case class ErrorReplaceConf(str: String)

case class TimeoutReplace(key: String)
