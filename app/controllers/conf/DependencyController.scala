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

  implicit def pd2dn(pds: Seq[ProjectDependency]): Seq[DependencyNest] = {
    pds.map{
      pd =>
        DependencyNest(pd.projectId, ConfigureActor.get_projectMap.get(pd.projectId).getOrElse("未知项目"), Seq.empty[DependencyNest])
    }.toSet.toSeq
  }

  def show(id: Int) = Action{
    val seq = ProjectDependencyHelper.all
    val pdSeq = findSubProjects(id, seq)
    var subProjects = Seq.empty[ProjectDependency]
    pdSeq.foreach{
      p =>
        subProjects = subProjects ++ findSubProjects(p.dependencyId, seq)
    }
    val result = Seq(
      DependencyNest(id, ProjectHelper.findById(id).get.name, subProjects)
    )
    Logger.info(s"${Json.prettyPrint(Json.toJson(result))}")
    Ok(Json.toJson(result))
  }

  def findSubProjects(pId: Int, seq: Seq[ProjectDependency]): Seq[ProjectDependency] = {
    var seqProjects = Seq.empty[ProjectDependency]
    seq.foreach{
      p =>
        if(p.projectId == pId){
          seqProjects = seqProjects :+ p
        }
    }
    seqProjects
  }

  def removeDependency(parentId: Int, childId: Int) = Action {
    Ok(Json.toJson(ProjectDependencyHelper.removeByP_C(parentId, childId)))
  }

  def addDependency = Action(parse.json){implicit request =>
    request.body match {
      case JsObject(fields) => {
        Ok(Json.toJson(add(fields)))
      }
      case _ => Ok(Json.toJson(0))
    }
  }
  def add(fields: Seq[(String, JsValue)]): Int = {
    val fieldsJson = Json.toJson(fields.toMap)
    val p = (fieldsJson \ "parent").as[DependencyNest]
    val c = (fieldsJson \ "child").as[Project]
    ProjectDependencyHelper.addByP_C(p, c)
  }

  implicit val dnWrites = Json.writes[DependencyNest]
  implicit val dnReads = Json.reads[DependencyNest]
  implicit val variableReads = Json.reads[Variable]
  implicit val projectReads = Json.reads[Project]
}


case class DependencyNest(id: Int, name: String, dependency: Seq[DependencyNest])
