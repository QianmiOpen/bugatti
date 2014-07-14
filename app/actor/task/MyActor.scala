package actor.task

import akka.actor.{Props, ActorSystem, Actor}
import enums.TaskEnum
import enums.TaskEnum.TaskStatus
import models.task.{TaskCommand, TaskQueueHelper, TaskQueue}

/**
 * Created by jinwei on 13/7/14.
 */
object MyActor {
  val system = ActorSystem("mySystem")
  //管理taskQueue中，在同一时间只有一个eid_pid的任务在执行
  val superviseTaskActor = system.actorOf(Props[MyActor], "superviseActor")
  //check salt执行结果
  val superviseJobActor = system.actorOf(Props[CheckJob], "checkJob")

  //容器：管理eid_pid状态
  var envId_projectIdStatus = Map.empty[String, TaskStatus]



  /**
   * 新建一个任务需要到actor的队列中处理
   * @param tq
   */
  def createNewTask(tq: TaskQueue) = {
    superviseTaskActor ! CreateNewTaskActor(tq)
  }

}

class MyActor extends Actor{
  import context._
  def receive = {
    case CreateNewTaskActor(tq) => {
      if(!MyActor.envId_projectIdStatus.keySet.contains(s"${tq.envId}_${tq.projectId}")){
        val taskExecute = actorOf(Props[TaskExecute], "taskExecute")
        MyActor.envId_projectIdStatus += s"${tq.envId}_${tq.projectId}" -> TaskEnum.TaskProcess
        taskExecute ! TaskGenerateCommand(tq)
      }
    }
    case NextTaskQueue(envId, projectId) => {
      MyActor.envId_projectIdStatus = MyActor.envId_projectIdStatus - s"${envId}_${projectId}"
      val taskQueue = TaskQueueHelper.findExecuteTask(envId, projectId)
      //TODO 更新队列状态 该envId_projectId下的taskqueues
      taskQueue match {
        case Some(tq) => {
          self ! CreateNewTaskActor(tq)
        }
        case _ => //do nothing
      }
    }
  }
}

case class CreateNewTaskActor(taskQueue: TaskQueue)
case class NextTaskQueue(envId: Int, projectId: Int)
