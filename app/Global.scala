import enums.{LevelEnum, RoleEnum}
import models.AppDB
import models.conf._
import models.task._
import org.joda.time.DateTime
import play.api._
import play.api.Play.current
import scala.slick.driver.MySQLDriver.simple._
import scala.slick.jdbc.meta.MTable

/**
 * Created by li on 14-6-19.
 */
object Global extends GlobalSettings {

  override def onStart(app: Application) {

    app.configuration.getBoolean("sql.not.init").getOrElse(
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
        Nil foreach { table =>
          if (!MTable.getTables(table.baseTableRow.tableName).list.isEmpty) table.ddl.drop
          table.ddl.create
        }
        AppData.userScript
        AppData.projectScript
        AppData.memberScript
        AppData.environmentScript
        AppData.permissionScript
        AppData.taskScript
        AppData.taskTemplateScript
        AppData.taskTemplateStepScript
        AppData.templateScript
        AppData.taskCommandScript
        AppData.taskQueueScript
        AppData.taskSchemeScript
      }


    )

  }

}
object AppData {

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
      Project(None, "cardbase-master", 1, 5, Option("1.6.4-SNAPSHOT"), Option(new DateTime()))
      ,Project(None, "cardbase-slave", 1, 5, Option("1.6.4-SNAPSHOT"), Option(new DateTime()))
      ,Project(None, "qianmi1", 1, 5, Option("1.6.4-SNAPSHOT"), Option(new DateTime()))
      ,Project(None, "qianmi2", 1, 5, Option("1.6.4-SNAPSHOT"), Option(new DateTime()))
      ,Project(None, "qianmi3", 1, 5, Option("1.6.4-SNAPSHOT"), Option(new DateTime()))
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
    val q = TableQuery[EnvironmentTable]
    if (!MTable.getTables(q.baseTableRow.tableName).list.isEmpty) q.ddl.drop
    q.ddl.create

    val seq = Seq(
      Environment(None, "dev", Option("开发"), Option("192.168.111.201"), Option("192.168.111.1/24"), LevelEnum.unsafe)
      ,Environment(None, "test", Option("测试"), Option("172.19.111.201"), Option("172.19.111.1/24"), LevelEnum.unsafe)
    )
    q.insertAll(seq: _*)
  }

  // 权限
  def permissionScript(implicit session: Session) = {
    val q = TableQuery[PermissionTable]
    if (!MTable.getTables(q.baseTableRow.tableName).list.isEmpty) q.ddl.drop
    q.ddl.create
    q.insert(Permission("of111", List(enums.FuncEnum.user, enums.FuncEnum.project)))
  }
  // 任务
  def taskScript(implicit session:Session) = {
    val task = TableQuery[TaskTable]
    if (!MTable.getTables(task.baseTableRow.tableName).list.isEmpty) task.ddl.drop
    task.ddl.create
  }
  //任务模板表
  def taskTemplateScript(implicit session: Session) = {
    val taskTemplate = TableQuery[TaskTemplateTable]
    if(!MTable.getTables(taskTemplate.baseTableRow.tableName).list.isEmpty) taskTemplate.ddl.drop
    taskTemplate.ddl.create

    val TaskTemplateSeq = Seq(
      TaskTemplate(Option(1), "install", 1, 0)
      , TaskTemplate(Option(2), "restart", 1, 1)
      , TaskTemplate(Option(3), "start", 1, 2)
      , TaskTemplate(Option(4), "stop", 1, 3)
      , TaskTemplate(Option(5), "status", 1, 4)
    )
    taskTemplate.insertAll(TaskTemplateSeq: _*)
  }
  //任务模板步骤
  def taskTemplateStepScript(implicit session: Session) = {
    val taskTemplateStep = TableQuery[TaskTemplateStepTable]
    if(!MTable.getTables(taskTemplateStep.baseTableRow.tableName).list.isEmpty) taskTemplateStep.ddl.drop
    taskTemplateStep.ddl.create

    val taskTemplateSeq = Seq(
      TaskTemplateStep(Option(1), 1, "java.install", 0)
      ,TaskTemplateStep(Option(2), 1, "tomcat.install", 1)
      ,TaskTemplateStep(Option(3), 1, "webapp.install", 2)
      ,TaskTemplateStep(Option(4), 1, "conf.install", 3)
    )
    taskTemplateStep.insertAll(taskTemplateSeq: _*)
  }
  //项目类型
  def templateScript(implicit session: Session) ={
    val template = TableQuery[TemplateTable]
    if(!MTable.getTables(template.baseTableRow.tableName).list.isEmpty) template.ddl.drop
    template.ddl.create

    val projectTypeSeq = Seq(
      Template(Some(1), "webapi")
    )
    template.insertAll(projectTypeSeq: _*)
  }
  
  //任务命令关联表
  def taskCommandScript(implicit session: Session) = {
    val taskCommand = TableQuery[TaskCommandTable]
    if(!MTable.getTables(taskCommand.baseTableRow.tableName).list.isEmpty) taskCommand.ddl.drop
    taskCommand.ddl.create
  }
  //任务队列表
  def taskQueueScript(implicit session: Session) = {
    val taskQueue = TableQuery[TaskQueueTable]
    if(!MTable.getTables(taskQueue.baseTableRow.tableName).list.isEmpty) taskQueue.ddl.drop
    taskQueue.ddl.create
  }
  //定时任务表
  def taskSchemeScript(implicit session: Session) = {
    val taskScheme = TableQuery[TaskSchemeTable]
    if(!MTable.getTables(taskScheme.baseTableRow.tableName).list.isEmpty) taskScheme.ddl.drop
    taskScheme.ddl.create
  }

}