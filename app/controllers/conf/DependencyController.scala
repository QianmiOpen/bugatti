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
    val tdMaps = ProjectDependencyHelper.findByProjectId(id).filter(pd => pd.alias != None).map(t => t.dependencyId -> t.alias.get).toMap
    val dependencyNests = subProjects.map{
      s =>
        if(tdMaps.keySet.contains(s.id.get)){
          DependencyNest(s.id.get, s.name, false, tdMaps.get(s.id.get).get, s.templateId, Seq.empty[DependencyNest])
        }else {
          DependencyNest(s.id.get, s.name, true, "", s.templateId, Seq.empty[DependencyNest])
        }
    }
    val result = Seq(
      DependencyNest(id, project.name, false, "", project.templateId, dependencyNests)
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

  def updateTemplateProject = Action(parse.json){implicit request =>
    request.body match {
      case JsObject(fields) => {
        Ok(Json.obj("r" -> update(fields)))
      }
      case _ => Ok(Json.obj("r" -> 0))
    }
  }

  def update(fields: Seq[(String, JsValue)]): Int = {
    val fieldsJson = Json.toJson(fields.toMap)
    val p = (fieldsJson \ "parentId").as[Int]
    val o = (fieldsJson \ "oldId").as[Int]
    val n = (fieldsJson \ "newId").as[Int]

    try{
      ProjectDependencyHelper.updateByP_C(p, o, n)
    } catch {
      case e: Exception => 0
    }
  }

  implicit val dnWrites = Json.writes[DependencyNest]
  implicit val dnReads = Json.reads[DependencyNest]
  implicit val variableReads = Json.reads[Variable]
  implicit val projectReads = Json.reads[Project]
}


case class DependencyNest(id: Int, name: String, canRemove: Boolean , alias: String, templateId: Int, dependency: Seq[DependencyNest])
