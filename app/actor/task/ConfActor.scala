package actor.task

import java.io.File

import akka.actor.{Props, Actor}
import models.conf.{ConfContent, ConfContentHelper, ConfHelper}
import play.api.libs.json.{Json, JsObject}
import utils.{ProjectTask_v, SaltTools, TaskTools, ConfHelp}
import scala.sys.process._
import scalax.file.Path

/**
 * Created by jinwei on 14/7/14.
 */
class ConfActor extends Actor{
  var _json = Json.obj()
  val _reg = """\{\{ *[^}]+ *\}\}""".r
  var _hostname = ""
  var _taskObj: ProjectTask_v = null
  var _envId = 0
  var _projectId = 0
  var _versionId = 0
  var _order = 0

  def receive = {
    case CopyConfFile(taskId, envId, projectId, versionId, order, json, hostname, taskObj) => {
      _json = json
      _hostname = hostname
      _taskObj = taskObj
      _envId = envId
      _projectId = projectId
      _versionId = versionId
      _order = order

      self ! GenerateConf()
    }

    case GenerateConf() => {
      val clusterActor = context.actorOf(Props[ClusterActor], s"clusterActor_${_envId}_${_projectId}_${_hostname}")
      clusterActor ! GenerateClusterConfs(_envId, _projectId, _versionId, _taskObj, _hostname)
    }

    case successConf: SuccessReplaceConf => {
      sender ! ExecuteCommand(successConf.taskId, successConf.envId, successConf.projectId, successConf.versionId, _order + 1)
      context.stop(self)
    }

    case errorConf: ErrorReplaceConf => {
      sender ! ConfCopyFailed(errorConf.str)
      context.stop(self)
    }

    case timeout: TimeoutReplace => {
      sender ! ConfCopyFailed(s"${timeout.key} 表达式执行超时!")
      context.stop(self)
    }
  }
}

case class CopyConfFile(taskId: Int, envId: Int, projectId: Int, versionId: Int, order: Int, json: JsObject, hostname: String, taskObj: ProjectTask_v)
case class GenerateConf()