package controllers.actor

import akka.actor._
import models.task.Task
import play.api.libs.iteratee._
import play.api.libs.json._
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.concurrent.Akka
import java.io.File
import utils.Reader
import play.api.Logger
import play.api.Play.current
import play.api.libs.json.JsArray
import play.api.libs.json.JsString
import play.api.libs.json.JsObject
//import models.configure.ProjectHelper

/**
 * Created by jinwei on 8/7/14.
 */
class TaskLog extends Actor{
  def receive = {
    case _ =>
  }


}

case class JoinLog(taskId: Int, task: Task, logFilePath: String)

case class ConnectedLog(enumerator: Enumerator[JsValue], text: String)


