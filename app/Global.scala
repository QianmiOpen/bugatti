import java.io.{File, FileInputStream}

import enums.{LevelEnum, RoleEnum}
import models.AppDB
import models.conf._
import models.task._
import org.joda.time.DateTime
import org.yaml.snakeyaml.Yaml
import play.api._
import play.api.Play.current
import utils.{SaltTools, GitHelp}
import scala.slick.driver.MySQLDriver.simple._
import scala.slick.jdbc.meta.MTable
import scala.collection.JavaConverters._

import java.util.{List => JList, Map => JMap}

/**
 * Created by li on 14-6-19.
 */
object Global extends GlobalSettings {

  override def onStart(app: Application) {

    if (app.configuration.getBoolean("sql.db.init").getOrElse(true)) {
      AppDB.db.withSession { implicit session =>
        TableQuery[ConfLogContentTable] ::
          TableQuery[ConfLogTable] ::
          TableQuery[ConfContentTable] ::
          TableQuery[ConfTable] ::
          TableQuery[VersionTable] ::
          TableQuery[PermissionTable] ::
          TableQuery[EnvironmentTable] ::
          TableQuery[MemberTable] ::
          TableQuery[ProjectTable] ::
          TableQuery[AttributeTable] ::
          TableQuery[TemplateItemTable] ::
          TableQuery[TemplateTable] ::
          TableQuery[UserTable] ::
          TableQuery[TaskTemplateTable] ::
          TableQuery[TaskTemplateStepTable] ::
          TableQuery[TaskCommandTable] ::
          TableQuery[TaskQueueTable] ::
          TableQuery[TaskSchemeTable] ::
          TableQuery[TaskTable] ::
          TableQuery[AreaTable] ::
          TableQuery[EnvironmentProjectRelTable] ::
          Nil foreach { table =>
          if (!MTable.getTables(table.baseTableRow.tableName).list.isEmpty) table.ddl.drop
          table.ddl.create
        }
        AppData.initFromYaml
      }
    }

    if (app.configuration.getBoolean("sql.test.init").getOrElse(true)) {
      AppDB.db.withSession { implicit session =>
        AppData.userScript
        AppData.projectScript
        AppData.memberScript
        AppData.environmentScript
        AppData.permissionScript
        AppData.taskScript
        AppData.taskCommandScript
        AppData.taskQueueScript
        AppData.taskSchemeScript
        AppData.versionScript
        AppData.attributeScript
        AppData.areaScript
      }
    }

    GitHelp.checkGitWorkDir(app)
    SaltTools.refreshHostList(app)
    SaltTools.baseLogPath(app)
  }
}


object AppData {
  def initFromYaml(implicit session: Session) = {
    val yaml = new Yaml()
    val io = new FileInputStream(new File("conf/initial-data.yml"))
    val templates = yaml.load(io).asInstanceOf[JMap[String, AnyRef]].get("templates").asInstanceOf[JList[JMap[String, AnyRef]]].asScala
    templates.foreach(_initTemplate)
  }

  def _initTemplate(template: JMap[String, AnyRef]) = {
    val templateId = TemplateHelper.create(Template(None, template.get("name").asInstanceOf[String], Some(template.get("remark").asInstanceOf[String])))

    // 创建template关联的item
    val templateItems = template.get("items").asInstanceOf[JList[JMap[String, String]]].asScala
    templateItems.zipWithIndex.foreach { case (x: JMap[String, String], index) =>
      TemplateItemHelper.create(TemplateItem(None, Some(templateId), x.get("itemName"), Some(x.get("itemDesc")), Some(x.get("default")), index))
    }

    // 创建template关联的actions
    val actions = template.get("actions").asInstanceOf[JList[JMap[String, AnyRef]]].asScala
    actions.zipWithIndex.foreach { case (action, index) =>

      val taskId = TaskTemplateHelper.create(TaskTemplate(None, action.get("name").asInstanceOf[String], action.get("css").asInstanceOf[String], action.get("versionMenu").asInstanceOf[Boolean], templateId, index + 1))
      val steps = action.get("steps").asInstanceOf[JList[JMap[String, String]]].asScala
      steps.zipWithIndex.foreach { case (step, index) =>
        val seconds = step.get("seconds").asInstanceOf[Int]
        TaskTemplateStepHelper.create(TaskTemplateStep(None, taskId, step.get("name"), step.get("sls"), if (seconds <= 0) 3 else seconds, index + 1))
      }
    }
  }

  // 用户表初始化
  def userScript(implicit session: Session) = {
    val q = TableQuery[UserTable]
    if (!MTable.getTables(q.baseTableRow.tableName).list.isEmpty) q.ddl.drop
    q.ddl.create
    q.insert(User("of546", "li", RoleEnum.admin, true, Some("1.1.1.1"), Some(DateTime.now())))
  }

  // 项目表初始化
  def projectScript(implicit session: Session) = {
    val q = TableQuery[ProjectTable]
    if (!MTable.getTables(q.baseTableRow.tableName).list.isEmpty) q.ddl.drop
    q.ddl.create

    val seq = Seq(
      Project(None, "cardbase-master", 1, 5, Option(1), Option("1.6.4-SNAPSHOT"), Option(new DateTime())),
      Project(None, "cardbase-slave", 1, 5, Option(2), Option("1.6.4-SNAPSHOT"), Option(new DateTime())),
      Project(None, "qianmi1", 1, 5, Option(3), Option("1.6.4-SNAPSHOT"), Option(new DateTime())),
      Project(None, "qianmi2", 1, 5, Option(4), Option("1.6.4-SNAPSHOT"), Option(new DateTime())),
      Project(None, "qianmi3", 1, 5, Option(5), Option("1.6.4-SNAPSHOT"), Option(new DateTime()))
    )
    q.insertAll(seq: _*)
  }

  def attributeScript(implicit session: Session) = {
    val q = TableQuery[AttributeTable]
    val seq = Seq(
      Attribute(None, Option(1), "groupId", Option("com.ofpay")),
      Attribute(None, Option(1), "artifactId", Option("cardserverimpl"))
    )
    q.insertAll(seq: _*)
  }

  //版本初始化
  def versionScript(implicit session: Session) = {
    val q = TableQuery[VersionTable]
    val seq = Seq(
      Version(None, 1, "1.6.4-SNAPSHOT", new DateTime(2014, 6, 30, 7, 31)),
      Version(None, 1, "1.6.3-RELEASE", new DateTime(2014, 6, 29, 7, 31)),
      Version(None, 1, "1.6.3-SNAPSHOT", new DateTime(2014, 6, 28, 7, 31)),
      Version(None, 1, "1.6.2-RELEASE", new DateTime(2014, 6, 28, 7, 31)),
      Version(None, 1, "1.6.2-SNAPSHOT", new DateTime(2014, 6, 27, 7, 31)),
      Version(None, 1, "1.6.1-RELEASE", new DateTime(2014, 6, 26, 7, 31))
    )
    q.insertAll(seq: _*)
  }

  // 成员
  def memberScript(implicit session: Session) = {
    val q = TableQuery[MemberTable]
    if (!MTable.getTables(q.baseTableRow.tableName).list.isEmpty) q.ddl.drop
    q.ddl.create
  }

  // 环境
  def environmentScript(implicit session: Session) = {
    Seq(
      //Environment(None, "pytest", Option("py测试"), Option("172.19.3.201"), Option("172.19.3.1/24"), LevelEnum.unsafe),
      Environment(None, "dev", Option("开发"), Option("192.168.111.201"), Option("192.168.111.1/24"), LevelEnum.unsafe),
      Environment(None, "test", Option("测试"), Option("172.19.111.201"), Option("172.19.111.1/24"), LevelEnum.unsafe)
    ).foreach(EnvironmentHelper.create)
  }

  // 权限
  def permissionScript(implicit session: Session) = {
    val q = TableQuery[PermissionTable]
    if (!MTable.getTables(q.baseTableRow.tableName).list.isEmpty) q.ddl.drop
    q.ddl.create
    q.insert(Permission("of111", List(enums.FuncEnum.user, enums.FuncEnum.project)))
  }

  // 任务
  def taskScript(implicit session: Session) = {
    val task = TableQuery[TaskTable]
    if (!MTable.getTables(task.baseTableRow.tableName).list.isEmpty) task.ddl.drop
    task.ddl.create
  }

  //任务命令关联表
  def taskCommandScript(implicit session: Session) = {
    val taskCommand = TableQuery[TaskCommandTable]
    if (!MTable.getTables(taskCommand.baseTableRow.tableName).list.isEmpty) taskCommand.ddl.drop
    taskCommand.ddl.create
  }

  //任务队列表
  def taskQueueScript(implicit session: Session) = {
    val taskQueue = TableQuery[TaskQueueTable]
    if (!MTable.getTables(taskQueue.baseTableRow.tableName).list.isEmpty) taskQueue.ddl.drop
    taskQueue.ddl.create
  }

  //定时任务表
  def taskSchemeScript(implicit session: Session) = {
    val taskScheme = TableQuery[TaskSchemeTable]
    if (!MTable.getTables(taskScheme.baseTableRow.tableName).list.isEmpty) taskScheme.ddl.drop
    taskScheme.ddl.create
  }

  def areaScript(implicit session: Session) = {
    Seq {
      Area(None, "测试", "t-syndic", "172.19.3.149")
    }.foreach(AreaHelper.create)
  }
}
