package actor.conf

import actor.ActorUtils
import akka.actor.Actor
import models.conf.ProjectHelper

import scala.collection.mutable

/**
 * Created by jinwei on 12/8/14.
 */
object ConfigureActor{
  private var projectMap = mutable.Map.empty[Int, String]

  def get_projectMap = {
    projectMap.clone.asInstanceOf[mutable.Map[Int, String]]
  }

  def initProjectMap = {
    ActorUtils.configuarActor ! InitProject()
  }
}

class ConfigureActor extends Actor{
  def receive = {
    case UpdateProject(id, name) => {
      ConfigureActor.projectMap += (id -> name)
    }
    case RemoveProject(id) => {
      ConfigureActor.projectMap -= id
    }
    case InitProject() => {
      ProjectHelper.all().foreach{
        p =>
          ConfigureActor.projectMap += p.id.get -> p.name
      }
    }
  }
}

case class UpdateProject(id: Int, name: String)
case class RemoveProject(id: Int)
case class InitProject()