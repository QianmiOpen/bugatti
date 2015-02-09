package models.conf

import org.joda.time.DateTime

import scala.slick.driver.MySQLDriver.simple._
import com.github.tototoshi.slick.MySQLJodaSupport._
import play.api.Play.current
/**
 * Created by jinwei on 7/2/15.
 */
case class ComponentMd5sum(id: Option[Int], scriptType: Int = 0, scriptVersionId: Int, componentName: String, md5sum: String, updateTime: DateTime = new DateTime)

case class ComponentMd5sumTable(tag: Tag) extends Table[ComponentMd5sum](tag, "component_md5sum"){
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def scriptType = column[Int]("script_type")
  def scriptVersionId = column[Int]("script_version_id")
  def componentName = column[String]("component_name")
  def md5sum = column[String]("md5sum")
  def updateTime = column[DateTime]("update_time", O.Default(DateTime.now))

  override def * = (id.?, scriptType, scriptVersionId, componentName, md5sum, updateTime) <> (ComponentMd5sum.tupled, ComponentMd5sum.unapply _)
}

object ComponentMd5sumHelper {
  import models.AppDB._

  val qCompMd5 = TableQuery[ComponentMd5sumTable]

  def findByScriptVersionId(scriptVersionId: Int): Seq[ComponentMd5sum]= db withSession{implicit session=>
    qCompMd5.filter(_.scriptVersionId === scriptVersionId).list
  }

  def update(componentMd5sum: ComponentMd5sum)= db withSession {implicit session=>
    qCompMd5.filter(t => t.scriptVersionId === componentMd5sum.scriptVersionId && t.componentName === componentMd5sum.componentName).firstOption match {
      case Some(component) =>
        qCompMd5.filter(_.id === component.id).update(component.copy(md5sum = componentMd5sum.md5sum, updateTime = DateTime.now))
      case _ =>
        qCompMd5.insert(componentMd5sum)
    }
  }
}