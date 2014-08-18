package actor.task

import akka.actor.{ActorLogging, Props, Actor}
import enums.TaskEnum
import models.conf._
import models.task._
import play.api.libs.json.{JsValue, Json, JsObject}
import utils.{SaltTools, TaskTools}

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
          val key = s"${tq.envId}_${tq.projectId}"
          context.child(s"commandActor_${key}").getOrElse(
            actorOf(Props[CommandActor], s"commandActor_${key}")
          ) ! InsertCommands(taskId, tqExecute.envId, tqExecute.projectId, tq.versionId, commandList, returnJson)
        }
        case _ => {
          MyActor.superviseTaskActor ! RemoveStatus(_envId, _projectId)
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

    val environment = EnvironmentHelper.findById(_envId).get

    // 如果是latest，则获取最后的一个可用版本
    val latestVersion = ScriptVersionHelper.findRealVersion(environment.scriptVersion)

    // 如果是master，需要替换成base，在gitfs中，是需要这么映射的
    val scriptVersion = latestVersion match {
      case ScriptVersionHelper.Master => "base"
      case x => x
    }

    log.info(s"latestVersion:${latestVersion}; scriptVersion:${scriptVersion}")

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
      , "scriptVersion" -> scriptVersion
    )
    //模板需要的item与项目的attribute做对比
    val items = TemplateItemHelper.findByTemplateId_ScriptVersion(project.templateId, latestVersion)
    val attrs = AttributeHelper.findByProjectId(tq.projectId)
    val attrsName = attrs.map(_.name)
    val errorItems = items.filterNot(t => attrsName.contains(t.itemName))
    if(errorItems.length > 0) {
      return (Seq.empty[TaskCommand], Json.obj("error" -> s"${errorItems.map(_.itemName)} 未配置"))
    }

    //attributes 按照 projectName.attrs.key -> value
    attrs.foreach {
      s =>
//        paramsJson = paramsJson ++ Json.obj(s"${project.name}.attrs.${s.name}" -> s.value)
        paramsJson = paramsJson ++ Json.obj(s.name -> s.value)
    }

    // todo
//    //properties 按照 projectName.props.key -> value
//    environment.globalVariable.foreach{
//      e =>
////        paramsJson = paramsJson ++ Json.obj(s"${project.name}.props.${e.name}" -> e.value)
//        paramsJson = paramsJson ++ Json.obj(e.name -> e.value)
//    }
//    project.globalVariable.foreach{
//      p =>
////        paramsJson = paramsJson ++ Json.obj(s"${project.name}.props.${p.name}" -> p.value)
//        paramsJson = paramsJson ++ Json.obj(p.name -> p.value)
//    }

    log.debug(s"paramsJson ==> ${Json.prettyPrint(paramsJson)}")

    val templateStep = TaskTemplateStepHelper.findStepsByTemplateId(tq.taskTemplateId)

    val templateCommands = templateStep.map{
      step =>
        generateTaskCommand(step, taskId)
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

    log.info(s"seq1 ==> ${seq}")


    //校验参数是否配置
    var errorSls = Seq.empty[String]
    seq.foreach{
      c =>
        val errorList = SaltTools.findErrorConf(paramsJson, c.command)
        if(errorList.length > 0){
          errorSls = errorSls :+ errorList.mkString(",")
        }
    }
    if(errorSls.length > 0){
      return (Seq.empty[TaskCommand], Json.obj("error" -> s"${errorSls.mkString(",")} 未配置"))
    }

    val seqResult = seq.map{
      t =>
        val cmd = t.command
        log.debug(s"cmd ==> ${cmd}")
        var taskCommand = t
        log.debug(s"reg ==> ${_reg.findAllIn(cmd)}")
        _reg.findAllIn(cmd).foreach{
          key => {
            log.debug(s"key ==> ${key}")
            val realKey = key.replaceAll("\\{\\{", "").replaceAll("\\}\\}", "")
            log.debug(s"realKey ==> ${realKey}")
            log.debug(s"paramsJson ==> ${Json.prettyPrint(paramsJson)}")
            log.debug(s"json ==> ${(paramsJson \ realKey).as[JsValue]}")
            val temp = taskCommand.command.replaceAll(s"\\{\\{${realKey}\\}\\}", TaskTools.trimQuotes((paramsJson \ realKey).toString))
            log.debug(s"temp ==> ${temp}")
            taskCommand = taskCommand.copy(command = temp)
            log.debug(s"${taskCommand}")
          }
        }
        log.debug(s"taskCommand ==> ${taskCommand}")
        taskCommand
    }

//    val templateCommands2 = templateStep.map { step =>
//      //参数替换，更改sls命令
//      fillSls(step, taskId, paramsJson)
//    }


    (seqResult, paramsJson)

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
//    val keys: Set[String] = paramsJson match {
//      case JsObject(fields) => {
//        fields.toMap.keySet
//      }
//      case _ => {
//        Set.empty[String]
//      }
//    }
    //sls.sls需要被填充
    TaskCommand(None, taskId, replaceSls(sls, paramsJson), "machine", sls.name, TaskEnum.TaskWait, sls.orderNum)
  }


  def replaceSls(sls: TaskTemplateStep, paramsJson: JsValue): String = {
    var result = sls.sls
    _reg.findAllIn(result).foreach{
      key =>
        val realKey = key.replaceAll("\\{\\{", "").replaceAll("\\}\\}", "")
        result = result.replaceAll(key, TaskTools.trimQuotes((paramsJson \ realKey).toString))
    }
//    keys.map {
//      key =>
//
//        result = result.replaceAll("\\{\\{" + key + "\\}\\}", TaskTools.trimQuotes((paramsJson \ key).toString))
//    }
    result
  }
  def getFileName() = {
    val timestamp: Long = System.currentTimeMillis / 1000
    s"${timestamp}"
  }
}


case class TaskGenerateCommand(envId: Int, projectId: Int)
case class RemoveTaskQueue()