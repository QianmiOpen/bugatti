package actor.task

import akka.actor.{Props, Actor}
import enums.TaskEnum
import models.conf._
import models.task._
import play.api.libs.json.{JsValue, Json, JsObject}
import utils.TaskTools

/**
 * Created by jinwei on 13/7/14.
 */
class TaskExecute extends Actor{
  implicit val taskQueueWrites = Json.writes[TaskQueue]

  var _tqId = 0
  var _envId = 0
  var _projectId = 0

  def receive = {
    case tgc: TaskGenerateCommand => {
      _envId = tgc.envId
      _projectId = tgc.projectId
      val taskQueue = TaskQueueHelper.findExecuteTask(_envId, _projectId)
      taskQueue match {
        case Some(tq) => {
          _tqId = tq.id.get
          val tqExecute = taskQueue.get
          //1、获取任务名称
          val taskName = TaskTemplateHelper.findById(tqExecute.taskTemplateId).name
          //2、insert 任务表
          val taskId = TaskHelper.addByTaskQueue(tqExecute)
          //获取队列信息
          val queues = TaskQueueHelper.findQueues(tqExecute.envId, tqExecute.projectId)
          val queuesJson: List[JsObject] = queues.map{
            x =>
              var json = Json.toJson(x)
              //增加模板名称
              json = json.as[JsObject] ++ Json.obj("taskTemplateName" -> TaskTemplateHelper.findById(x.taskTemplateId).name)
              json.as[JsObject]
          }
          //3、生成命令列表
          val (commandList, returnJson) = generateCommands(taskId, tqExecute)
          val totalNum = commandList.length
          val currentNum = 0

          MyActor.superviseTaskActor ! ChangeTaskStatus(tqExecute, taskName, queuesJson, currentNum, totalNum)
          TaskCommandHelper.create(commandList)
          //TODO executeCommands 从第1个命令开始执行
          val commandActor = context.actorOf(Props[CommandActor], s"commandActor_${tq.envId}_${tq.projectId}")
          commandActor ! InsertCommands(taskId, tqExecute.envId, tqExecute.projectId, tq.versionId, commandList, returnJson)
        }
        case _ => {
          context.stop(self)
        }
      }
    }

    case next: NextTaskQueue => {
      self ! TaskGenerateCommand(next.envId, next.projectId)
    }

    case removeTaskQueue: RemoveTaskQueue => {
      TaskQueueHelper.deleteById(_tqId)
          //没有任务，删除MyActor中的缓存
      MyActor.superviseTaskActor ! RemoveStatus(_envId, _projectId)
//      context.stop(self)
    }
  }

  def generateCommands(taskId: Int, tq: TaskQueue): (Seq[TaskCommand], JsObject)  = {
    val seqMachines = EnvironmentProjectRelHelper.findByEnvId_ProjectId(tq.envId, tq.projectId)
    if (seqMachines.length == 0) {
      return (Seq.empty[TaskCommand],  Json.obj("error" -> s"未关联机器"))
    }
    val nfsServer = EnvironmentHelper.findById(tq.envId).get.nfServer

    val project = ProjectHelper.findById(tq.projectId).get
    val projectName = project.name

    val versionId = tq.versionId
    var repository = "releases"
    if (TaskTools.isSnapshot(versionId.getOrElse(0))) {
      repository = "snapshots"
    }

    var versionName = ""
    versionId match {
      case Some(vid) => {
        versionName = VersionHelper.findById(vid).get.vs
      }
      case _ =>
    }

    val fileName = getFileName()

    var paramsJson = Json.obj(
      "nfsServer" -> nfsServer
      , "version" -> versionName
      , "versionId" -> versionId.getOrElse[Int](0)
      , "repository" -> repository
      , "projectName" -> projectName
      , "envId" -> tq.envId
      , "projectId" -> tq.projectId
      , "taskId" -> taskId
      , "confFileName" -> fileName
    )

    //envId -> Seq[template_item]
    var latestVersion = ScriptVersionHelper.Master
    EnvironmentHelper.findById(_envId).get.scriptVersion match {
      case ScriptVersionHelper.Latest => {
        latestVersion = ScriptVersionHelper.findLatest().get
      }
    }
    val items = TemplateItemHelper.findByTemplateId_ScriptVersion(project.templateId, latestVersion)
    val attrs = AttributeHelper.findByProjectId(tq.projectId)
    val attrsName = attrs.map(_.name)
    val errorItems = items.filterNot(t => attrsName.contains(t.itemName))
    if(errorItems.length > 0) {
      return (Seq.empty[TaskCommand], Json.obj("error" -> s"${errorItems} 未配置"))
    }
    val attributesJson = attrs.map {
      s =>
        paramsJson = paramsJson ++ Json.obj(s.name -> s.value)
    }

    val templateCommands = TaskTemplateStepHelper.findStepsByTemplateId(tq.taskTemplateId).map { step =>
      //参数替换，更改sls命令
      fillSls(step, taskId, paramsJson)
    }

    //获取机器，遍历机器&模板命令
    var count = 0
    val seq = for {machine <- seqMachines
                   c <- templateCommands
    } yield {
      count += 1
      val command = c.command.replaceAll("\\{\\{machine\\}\\}", machine.name).replaceAll("\\{\\{syndic\\}\\}", machine.syndicName)
      c.copy(command = command).copy(orderNum = count).copy(machine = s"${machine.name}")
    }
    (seq, paramsJson)

  }


  /**
   * 填写命令参数
   * @param sls
   * @param taskId
   * @param paramsJson
   * @return
   */
  def fillSls(sls: TaskTemplateStep, taskId: Int, paramsJson: JsValue): TaskCommand = {
    val keys: Set[String] = paramsJson match {
      case JsObject(fields) => {
        fields.toMap.keySet
      }
      case _ => {
        Set.empty[String]
      }
    }
    //sls.sls需要被填充
    TaskCommand(None, taskId, replaceSls(sls, paramsJson, keys), "machine", sls.name, TaskEnum.TaskWait, sls.orderNum)
  }

  def replaceSls(sls: TaskTemplateStep, paramsJson: JsValue, keys: Set[String]): String = {
    var result = sls.sls
    keys.map {
      key =>
        result = result.replaceAll("\\{\\{" + key + "\\}\\}", TaskTools.trimQuotes((paramsJson \ key).toString))
    }
    result
  }
  def getFileName() = {
    val timestamp: Long = System.currentTimeMillis / 1000
    s"${timestamp}"
  }
}


case class TaskGenerateCommand(envId: Int, projectId: Int)
case class RemoveTaskQueue()