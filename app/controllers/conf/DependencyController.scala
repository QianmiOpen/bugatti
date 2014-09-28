package controllers.conf

import actor.conf.ConfigureActor
import controllers.BaseController
import controllers.task.TaskController._
import models.conf._
import play.api.Logger
import play.api.libs.json.{JsValue, JsObject, Json}
import play.api.mvc.Action

/**
 * Created by jinwei on 9/8/14.
 */
object DependencyController extends BaseController{

  def show(id: Int) = Action{
    val project = ProjectHelper.findById(id).get
    val subProjects = ProjectHelper.findDependencyProjects(id)
    val dependencyProjectIds = TemplateHelper.findById(project.templateId).get.dependentProjectIds
    Logger.info(s"${dependencyProjectIds.mkString(",")}")
    val dependencyNests = subProjects.map{
      s =>
        if(dependencyProjectIds.contains(s.id.get)){
          DependencyNest(s.id.get, s.name, false, Seq.empty[DependencyNest])
        }else {
          DependencyNest(s.id.get, s.name, true, Seq.empty[DependencyNest])
        }
    }
    val result = Seq(
      DependencyNest(id, project.name, false, dependencyNests)
    )
    Logger.info(s"${Json.prettyPrint(Json.toJson(result))}")
    Ok(Json.toJson(result))
  }

  def removeDependency(parentId: Int, childId: Int) = Action {
    Ok(Json.toJson(ProjectDependencyHelper.removeByP_C(parentId, childId)))
  }

  def addDependency = Action(parse.json){implicit request =>
    request.body match {
      case JsObject(fields) => {
        Ok(Json.obj("r" -> add(fields)))
      }
      case _ => Ok(Json.obj("r" -> 0))
    }
  }
  def add(fields: Seq[(String, JsValue)]): Int = {
    val fieldsJson = Json.toJson(fields.toMap)
    val p = (fieldsJson \ "parent").as[DependencyNest]
    val c = (fieldsJson \ "child").as[Project]
    try{
      ProjectDependencyHelper.addByP_C(p, c)
    } catch {
      case e: Exception => 0
    }
  }

  implicit val dnWrites = Json.writes[DependencyNest]
  implicit val dnReads = Json.reads[DependencyNest]
  implicit val variableReads = Json.reads[Variable]
  implicit val projectReads = Json.reads[Project]
}


case class DependencyNest(id: Int, name: String, canRemove: Boolean , dependency: Seq[DependencyNest])
