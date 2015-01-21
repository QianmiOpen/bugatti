package actor.task

import akka.actor.SupervisorStrategy.Escalate
import akka.actor.{OneForOneStrategy, ActorLogging, Props, Actor}
import enums.TaskEnum
import enums.TaskEnum._
import exceptions.TaskExecuteException
import models.conf._
import models.task._
import play.api.libs.json.{JsValue, Json, JsObject}
import utils.{ProjectTask_v, SaltTools, TaskTools}

/**
 * Created by jinwei on 13/7/14.
 */
class TaskExecute extends Actor with ActorLogging {
  import context._

  override val supervisorStrategy = OneForOneStrategy() {
    case e: Exception =>
      log.error(s"${self} catch exception: ${e.getMessage} ${e.getStackTraceString}")
//      postStop()
      taskFailed()
      Escalate
  }

  def taskFailed(): Unit ={
    terminate(TerminateCommands(TaskEnum.TaskFailed, _envId, _projectId, _clusterName))
  }

  implicit val taskQueueWrites = Json.writes[TaskQueue]

  var _envId = 0
  var _projectId = 0

  var (_commandList, _taskDoifList, _json) = (Seq.empty[TaskCommand], Seq.empty[String], Json.obj())

  var _clusterName = Option.empty[String]

  def receive = {
    case tgc: TaskGenerateCommand => {
      val taskQueue = TaskQueueHelper.findExecuteTask(tgc.epc.envId, tgc.epc.projectId, tgc.epc.clusterName)
      //for postStop
      _envId = tgc.epc.envId
      _projectId = tgc.epc.projectId
      _clusterName = tgc.epc.clusterName
      taskQueue match {
        case Some(tq) => {
//          _tqId = tq.id.get
//          _tqExecute = tq
          //1、获取任务名称
          val taskName = TemplateActionHelper.findById(tq.taskTemplateId).name
          //2、insert 任务表
          val taskId = TaskHelper.addByTaskQueue(tq)
          //获取队列信息
          val queues = TaskQueueHelper.findQueues(tq.envId, tq.projectId, tgc.epc.clusterName)
          val queuesJson = queues.map{
            x =>
              var json = Json.toJson(x)
              //增加模板名称
              json = json.as[JsObject] ++ Json.obj("taskTemplateName" -> TemplateActionHelper.findById(x.taskTemplateId).name)
              json.as[JsObject]
          }
          var taskObj: ProjectTask_v = null
          try{
            taskObj = TaskTools.generateTaskObject(taskId, tq.envId, tq.projectId, tq.versionId).copy(taskName = taskName)
          }catch {
            case e: Exception => {
              _commandList = Seq.empty[TaskCommand]
              _json = Json.obj("error" -> e.getMessage())
              log.error(s"${e.getStackTrace}")
              log.error(s"errorMessage => ${e.getMessage}")
            }
          }

          val seqMachines = HostHelper.findByEnvId_ProjectId(tq.envId, tq.projectId)
          if (seqMachines.length == 0) {
            _commandList = Seq.empty[TaskCommand]
            _json = Json.obj("error" -> s"未关联机器")
            log.error(Json.prettyPrint(_json))
          }
          //3、生成命令列表
          val templateStep = TemplateActionStepHelper.findStepsByTemplateId(tq.taskTemplateId)
          val totalNum =  tgc.epc.clusterName match {
            case Some(c) => {
              templateStep.length + 1 //获取机器状态是后来拼接到命令列表，因此命令总数需要+1
            }
            case _ => {
              tgc.epc.hosts.length * templateStep.length + 1
            }
          }
          val hostsIndex = tgc.epc.hostIndex
          _commandList = Seq.empty[TaskCommand]
          self ! GenerateCommands(tq, templateStep, tgc.epc.hosts, hostsIndex, taskObj)
          MyActor.superviseTaskActor ! ChangeTaskStatus(tq, taskName, queuesJson, hostsIndex, totalNum, tgc.epc.clusterName)
        }
        case _ => {
          MyActor.superviseTaskActor ! RemoveStatus(tgc.epc.envId, tgc.epc.projectId, tgc.epc.clusterName)
          context.stop(self)
        }
      }
    }

    case next: NextTaskQueue => {
      next.clusterName match {
        case Some(c) => {
          self ! TaskGenerateCommand(EPCParams(next.envId, next.projectId, next.clusterName, Seq.empty[Host], 0))
        }
        case _ => {
          val hosts = HostHelper.findByEnvId_ProjectId(next.envId, next.projectId)
          self ! TaskGenerateCommand(EPCParams(next.envId, next.projectId, next.clusterName, hosts, 0))
        }
      }
    }

    case gcommand: GenerateCommands => {
      log.info(s"_hostIndex ==> ${gcommand.hostsIndex}")
      log.info(s"_hosts ==> ${gcommand.hosts}")
      //增加对机器级别的控制
      gcommand.tq.clusterName match {
        case Some(c) =>{
          if(gcommand.hostsIndex == 0 && _json.keys.size == 0){
            val actorName = s"clusterActor_${gcommand.tq.envId}_${gcommand.tq.projectId}_${c}"
            val clusterActor = context.child(actorName).getOrElse(context.actorOf(Props[ClusterActor], actorName))
            clusterActor ! GenerateClusterCommands(gcommand.taskObj.taskId.toInt, gcommand.taskObj, gcommand.templateStep, c, gcommand.tq, gcommand.hosts, gcommand.hostsIndex)
//            _hostsIndex = _hostsIndex + 1
          }else {
            //发送CommandActor
            log.info(s"_commandList => ${_commandList}")
            log.info(s"_taskDoifList => ${_taskDoifList}")
            self ! SendCommandActor(gcommand.tq, gcommand.taskObj)
            TaskCommandHelper.create(_commandList)
          }
        }
        case _ => {
          if(gcommand.hostsIndex <= gcommand.hosts.length-1 && _json.keys.size == 0){
            val cluster = gcommand.hosts(gcommand.hostsIndex).name
            val actorName = s"clusterActor_${gcommand.tq.envId}_${gcommand.tq.projectId}_${cluster}"
            val clusterActor = context.child(actorName).getOrElse(context.actorOf(Props[ClusterActor], actorName))
            log.info(s"TaskExecute.gcc.templateStep ==> ${gcommand.templateStep}")
            log.info(s"TaskExecute.gcc.taskId ==> ${gcommand.taskObj.taskId}")
            clusterActor ! GenerateClusterCommands(gcommand.taskObj.taskId.toInt, gcommand.taskObj, gcommand.templateStep, cluster, gcommand.tq, gcommand.hosts, gcommand.hostsIndex)
//            _hostsIndex = _hostsIndex + 1
          }else {
            //发送CommandActor
            self ! SendCommandActor(gcommand.tq, gcommand.taskObj)
            TaskCommandHelper.create(_commandList)
          }
        }
      }
    }

    case successReplace: SuccessReplaceCommand => {
      _commandList = _commandList ++ successReplace.commandList
      _taskDoifList = _taskDoifList ++ successReplace.taskDoif
      log.info(s"successReplace ==> ${_commandList}")
      self ! GenerateCommands(successReplace.tq, successReplace.templateStep, successReplace.hosts, successReplace.hostsIndex, successReplace.taskObj)
    }

    case errorReplace: ErrorReplaceCommand => {
      log.info(s"TaskExecute errorCommand")
      _commandList = Seq.empty[TaskCommand]
      _json = Json.obj("error" -> s"变量异常! ${errorReplace.keys}")
      self ! GenerateCommands(errorReplace.tq, errorReplace.templateStep, errorReplace.hosts, errorReplace.hostsIndex, errorReplace.taskObj)
    }

    case timeout: TimeoutReplace => {
      sender ! ConfCopyFailed(s"${timeout.key} 表达式执行超时!")
//      context.stop(self)
    }

    case sc: SendCommandActor => {
//      val key = s"${_envId}_${_projectId}"
      val key = taskKey(sc.tq.envId, sc.tq.projectId, sc.tq.clusterName)
      context.child(s"commandActor_${key}").getOrElse(
        actorOf(Props[CommandFSMActor], s"commandActor_${key}")
      ) ! Insert(sc.taskObj.taskId.toInt, sc.tq.envId, sc.tq.projectId, sc.tq.versionId, _commandList, _json, sc.taskObj, sc.tq.clusterName, _taskDoifList)
    }

    case removeTaskQueue: RemoveTaskQueue => {

      val key = taskKey(removeTaskQueue.envId, removeTaskQueue.projectId, removeTaskQueue.clusterName)
      context.child(s"commandActor_${key}") match {
        case Some(actor) => {
          context.stop(actor)
        }
        case _ =>
      }
      TaskQueueHelper.findExecuteTask(removeTaskQueue.envId, removeTaskQueue.projectId, removeTaskQueue.clusterName) match {
        case Some(tq) =>
          TaskQueueHelper.deleteById(tq.id.get)
        case _ =>
      }


      //没有任务，删除MyActor中的缓存
      MyActor.superviseTaskActor ! RemoveStatus(removeTaskQueue.envId, removeTaskQueue.projectId, removeTaskQueue.clusterName)
    }

    case tc: TerminateCommands => {
      terminate(tc)
    }

    case ucs: UpdateCommandStatus =>
      TaskCommandHelper.update(ucs.taskId, ucs.orderNum, ucs.status)

    case st: StopTask =>
      val key = taskKey(st.envId, st.projectId, st.clusterName)
      context.child(s"commandActor_${key}").getOrElse(
        actorOf(Props[CommandFSMActor], s"commandActor_${key}")
      ) ! st
  }

  def terminate(tc: TerminateCommands): Unit ={
    TaskHelper.findLastTask(tc.envId, tc.projectId, tc.clusterName) match {
      case Some(t) =>
        log.info(s"taskExecute Terminate taskId ==> ${t.id}")
        TaskHelper.changeStatus(t.id.get, tc.status)
        val (task, version) = getTask_VS(t.id.get)
        MyActor.superviseTaskActor ! ChangeOverStatus(task.envId, task.projectId, tc.status, task.endTime.get, version, task.clusterName)
      case _ =>
        val err = s"环境:${tc.envId} 项目:${tc.projectId} 负载: ${tc.clusterName} 找不到相应的任务"
        log.error(err)
        throw new TaskExecuteException(err)
    }
  }

  /**
   * 生成正确的taskKey（区分项目和机器级别的key）
   * @param envId
   * @param projectId
   * @param cluster
   * @return
   */
  def taskKey(envId: Int, projectId: Int, cluster: Option[String]): String ={
    cluster match{
      case Some(c) =>
        s"${envId}_${projectId}_${c}"
      case _ =>
        s"${envId}_${projectId}"
    }
  }

//  def generateTaskCommand(sls: TaskTemplateStep, taskId: Int): TaskCommand = {
//    TaskCommand(None, taskId, sls.sls, "machine", sls.name, TaskEnum.TaskWait, sls.orderNum)
//  }

  def getTask_VS(taskId: Int): (Task, String) = {
    //TODO refactor
    val task = TaskHelper.findById(taskId)
    var version = ""
    task.versionId match {
      case Some(vid) => {
        version = VersionHelper.findById(vid).get.vs
      }
      case _ =>
    }
    (task, version)
  }

//  /**
//   * 填写命令参数
//   * @param sls
//   * @param taskId
//   * @param paramsJson
//   * @return
//   */
//  def fillSls(sls: TaskTemplateStep, taskId: Int, paramsJson: JsValue): TaskCommand = {
//    TaskCommand(None, taskId, replaceSls(sls, paramsJson), "machine", sls.name, TaskEnum.TaskWait, sls.orderNum)
//  }
//
//  def replaceSls(sls: TaskTemplateStep, paramsJson: JsValue): String = {
//    var result = sls.sls
//    _reg.findAllIn(result).foreach{
//      key =>
//        val realKey = key.replaceAll("\\{\\{", "").replaceAll("\\}\\}", "")
//        result = result.replaceAll(key, TaskTools.trimQuotes((paramsJson \ realKey).toString))
//    }
//
//    result
//  }
}


case class TaskGenerateCommand(epc: EPCParams)
case class RemoveTaskQueue(envId: Int, projectId: Int, clusterName: Option[String])

case class GenerateCommands(tq: TaskQueue, templateStep: Seq[TemplateActionStep], hosts: Seq[Host], hostsIndex: Int, taskObj: ProjectTask_v)
case class SendCommandActor(tq: TaskQueue, taskObj: ProjectTask_v)
case class UpdateCommandStatus(taskId: Int, orderNum: Int, status: TaskStatus)

case class EPCParams(envId: Int, projectId: Int, clusterName: Option[String], hosts: Seq[Host], hostIndex: Int)
case class StopTask(envId: Int, projectId: Int, clusterName: Option[String])