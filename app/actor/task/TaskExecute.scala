package actor.task

import java.io.File

import akka.actor.{Props, Actor}
import enums.TaskEnum
import models.conf._
import models.task._
import play.api.libs.json.{JsValue, Json, JsObject}
import utils.TaskTools

/**
 * Created by jinwei on 13/7/14.
 */
object TaskExecute {

}

class TaskExecute extends Actor{
  def receive = {
    case TaskGenerateCommand(tq) => {
      //1、获取任务名称
      val taskName = TaskTemplateHelper.findById(tq.taskTemplateId).name
      //2、insert 任务表
      val taskId = TaskHelper.addByTaskQueue(tq)
      //3、生成命令列表
      val (commandList, paramsJson) = generateCommands(taskId, tq)
      TaskCommandHelper.create(commandList)
      //TODO executeCommands
      
    }
  }

  def generateCommands(taskId: Int, tq: TaskQueue): (Seq[TaskCommand], JsObject)  = {
    val seqMachines = EnvironmentProjectRelHelper.findByEnvId_ProjectId(tq.envId, tq.projectId)
    if (seqMachines.length == 0) {
      return (Seq.empty[TaskCommand], null)
    }
    val nfsServer = EnvironmentHelper.findById(tq.envId).get.nfServer

    val projectName = ProjectHelper.findById(tq.projectId).get.name

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

    val attributesJson = AttributeHelper.findByPid(tq.projectId).map {
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


case class TaskGenerateCommand(taskQueue: TaskQueue)