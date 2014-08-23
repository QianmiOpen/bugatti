package actor.task

import akka.actor.{ActorLogging, Props, Actor}
import enums.TaskEnum
import models.conf._
import models.task._
import play.api.libs.json.{JsValue, Json, JsObject}
import utils.{Task_v, SaltTools, TaskTools}

/**
 * Created by jinwei on 13/7/14.
 */
class TaskExecute extends Actor with ActorLogging {
  import context._
  implicit val taskQueueWrites = Json.writes[TaskQueue]

  var _tqId = 0
  var _envId = 0
  var _projectId = 0

  val _reg = """\{\{ *[^}]+ *\}\}""".r

  var _hosts = Seq.empty[EnvironmentProjectRel]
  var _hostsIndex = 0

  var _templateStep = Seq.empty[TaskTemplateStep]
  var _taskObj: Task_v = null

  var (_commandList, _json) = (Seq.empty[TaskCommand], Json.obj())

  var _taskId = 0
  var _taskName = ""
  var _tqExecute: TaskQueue = null
  var _queuesJson = List.empty[JsObject]
  var _totalNum = 0

  def receive = {
    case tgc: TaskGenerateCommand => {
      _envId = tgc.envId
      _projectId = tgc.projectId
      val taskQueue = TaskQueueHelper.findExecuteTask(_envId, _projectId)
      taskQueue match {
        case Some(tq) => {
          _tqId = tq.id.get
          _tqExecute = taskQueue.get
          //1、获取任务名称
          _taskName = TaskTemplateHelper.findById(_tqExecute.taskTemplateId).name
          //2、insert 任务表
          _taskId = TaskHelper.addByTaskQueue(_tqExecute)
          //获取队列信息
          val queues = TaskQueueHelper.findQueues(_tqExecute.envId, _tqExecute.projectId)
          _queuesJson = queues.map{
            x =>
              var json = Json.toJson(x)
              //增加模板名称
              json = json.as[JsObject] ++ Json.obj("taskTemplateName" -> TaskTemplateHelper.findById(x.taskTemplateId).name)
              json.as[JsObject]
          }

          try{
            _taskObj = TaskTools.generateTaskObject(_taskId, tq.envId, tq.projectId, tq.versionId)
          }catch {
            case e: Exception => {
              _commandList = Seq.empty[TaskCommand]
              _json = Json.obj("error" -> e.getMessage())
              log.error(e.toString)
              log.error(e.getMessage)
            }
          }

          val seqMachines = EnvironmentProjectRelHelper.findByEnvId_ProjectId(tq.envId, tq.projectId)
          if (seqMachines.length == 0) {
            _commandList = Seq.empty[TaskCommand]
            _json = Json.obj("error" -> s"未关联机器")
            log.error(Json.prettyPrint(_json))
          }
          //3、生成命令列表
          _templateStep = TaskTemplateStepHelper.findStepsByTemplateId(_tqExecute.taskTemplateId)
          _totalNum = _hosts.length * _templateStep.length
          self ! GenerateCommands()
          MyActor.superviseTaskActor ! ChangeTaskStatus(_tqExecute, _taskName, _queuesJson, 0, _totalNum)
        }
        case _ => {
          MyActor.superviseTaskActor ! RemoveStatus(_envId, _projectId)
          context.stop(self)
        }
      }
    }

    case next: NextTaskQueue => {
      _hosts = EnvironmentProjectRelHelper.findByEnvId_ProjectId(next.envId, next.projectId)
      self ! TaskGenerateCommand(next.envId, next.projectId)
    }

    case gcommand: GenerateCommands => {
      log.info(s"_hostIndex ==> ${_hostsIndex}")
      log.info(s"_hosts ==> ${_hosts}")
      if(_hostsIndex <= _hosts.length-1 && _json.keys.size == 0){
        val cluster = _hosts(_hostsIndex).name
        val clusterActor = context.actorOf(Props[ClusterActor], s"clusterActor_${_envId}_${_projectId}_${cluster}")
        clusterActor ! GenerateClusterCommands(_taskId, _taskObj, _templateStep, cluster)
        _hostsIndex = _hostsIndex + 1
      }else {
        //发送CommandActor
        self ! SendCommandActor()
        TaskCommandHelper.create(_commandList)
      }
    }

    case successReplace: SuccessReplaceCommand => {
      _commandList = _commandList ++ successReplace.commandList
      self ! GenerateCommands()
    }

    case errorReplace: ErrorReplaceCommand => {
      log.info(s"TaskExecute errorCommand")
      _commandList = Seq.empty[TaskCommand]
      _json = Json.obj("error" -> s"${errorReplace.keys.mkString(",")} 变量异常!")
      self ! GenerateCommands()
    }

    case timeout: TimeoutReplace => {
      sender ! ConfCopyFailed(s"${timeout.key} 表达式执行超时!")
      context.stop(self)
    }

    case sc: SendCommandActor => {
      val key = s"${_envId}_${_projectId}"
      context.child(s"commandActor_${key}").getOrElse(
        actorOf(Props[CommandActor], s"commandActor_${key}")
      ) ! InsertCommands(_taskId, _tqExecute.envId, _tqExecute.projectId, _tqExecute.versionId, _commandList, _json, _taskObj)
    }

    case removeTaskQueue: RemoveTaskQueue => {
      TaskQueueHelper.deleteById(_tqId)
      //没有任务，删除MyActor中的缓存
      MyActor.superviseTaskActor ! RemoveStatus(_envId, _projectId)
    }
  }

  def generateTaskCommand(sls: TaskTemplateStep, taskId: Int): TaskCommand = {
    TaskCommand(None, taskId, sls.sls, "machine", sls.name, TaskEnum.TaskWait, sls.orderNum)
  }

  /**
   * 填写命令参数
   * @param sls
   * @param taskId
   * @param paramsJson
   * @return
   */
  def fillSls(sls: TaskTemplateStep, taskId: Int, paramsJson: JsValue): TaskCommand = {
    TaskCommand(None, taskId, replaceSls(sls, paramsJson), "machine", sls.name, TaskEnum.TaskWait, sls.orderNum)
  }

  def replaceSls(sls: TaskTemplateStep, paramsJson: JsValue): String = {
    var result = sls.sls
    _reg.findAllIn(result).foreach{
      key =>
        val realKey = key.replaceAll("\\{\\{", "").replaceAll("\\}\\}", "")
        result = result.replaceAll(key, TaskTools.trimQuotes((paramsJson \ realKey).toString))
    }

    result
  }
}


case class TaskGenerateCommand(envId: Int, projectId: Int)
case class RemoveTaskQueue()

case class GenerateCommands()
case class SendCommandActor()