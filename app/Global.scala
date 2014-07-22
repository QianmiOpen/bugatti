import java.io.{File, FileInputStream}

import controllers.actor.TaskProcess
import enums.{LevelEnum, RoleEnum}
import models.AppDB
import models.conf._
import models.task._
import org.joda.time.DateTime
import org.yaml.snakeyaml.Yaml
import play.api._
import play.api.Play.current
import utils.{ConfHelp, SaltTools, GitHelp}
import scala.slick.driver.MySQLDriver.simple._
import scala.slick.jdbc.meta.MTable
import scala.collection.JavaConverters._

import org.pac4j.core.client.Clients
import org.pac4j.play.Config
import org.pac4j.cas.client.CasClient

import java.util.{List => JList, Map => JMap}

/**
 * 环境配置
 */
object Global extends GlobalSettings {

  override def beforeStart(app: Application) {
    System.setProperty("javax.net.ssl.trustStore", app.configuration.getString("ssl.trustStore").getOrElse("conf/certificate.jks"))
  }

  override def onStart(app: Application) {
    // cas init
    val casClient = new CasClient()

    val loginUrl = app.configuration.getString("cas.login_url").getOrElse("https://oflogin.of-crm.com")
    casClient.setCasLoginUrl(loginUrl)

    val logoutUrl = app.configuration.getString("cas.logout_url").getOrElse("https://oflogin.of-crm.com")
    Config.setDefaultLogoutUrl(logoutUrl)

    val callbackUrl = app.configuration.getString("cas.callback_url").getOrElse("http://bugatti.dev.ofpay.com/callback")
    Config.setClients(new Clients(callbackUrl, casClient))

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

        AppData.initFromYaml(new File("conf/initial-data.yml"))
        AppData.initData
      }
    }

    if (app.configuration.getBoolean("sql.test.init").getOrElse(true)) {
      AppDB.db.withSession { implicit session =>
        AppTestData.userScript
        AppTestData.projectScript
        AppTestData.memberScript
        AppTestData.environmentScript
        AppTestData.permissionScript
        AppTestData.taskScript
        AppTestData.taskCommandScript
        AppTestData.taskQueueScript
        AppTestData.taskSchemeScript
        AppTestData.versionScript
        AppTestData.attributeScript
        AppTestData.areaScript
        AppTestData.environmentProjectRelScript
      }

      AppData.initFromYaml(new File("conf/initial-test-data.yml"))
    }

//    GitHelp.checkGitWorkDir(app)
    SaltTools.refreshHostList(app)
    SaltTools.baseLogPath(app)
    ConfHelp.initConfPath(app)

    //查看队列表中是否有可执行任务
    val set = TaskQueueHelper.findEnvId_ProjectId()
    set.foreach{
      s =>
        TaskProcess.checkQueueNum(s._1, s._2)
        TaskProcess.executeTasks(s._1, s._2)
    }

    TaskProcess.generateSchedule
  }
}


object AppData {

  def initData(implicit session: Session) = {
    val U = TableQuery[UserTable]
    Seq(
      User("of546", "李允恒", RoleEnum.admin, true, false, None, None),
      User("of557", "彭毅", RoleEnum.admin, false, false, None, None),
      User("of729", "金卫", RoleEnum.admin, false, false, None, None),
      User("of9999", "龚平", RoleEnum.admin, true, false, None, None)
    ).foreach(U.insert)
  }

  def initFromYaml(file: File) = {
    val yaml = new Yaml()
    val io = new FileInputStream(file)
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
}

object AppTestData {

  // 用户表初始化
  def userScript(implicit session: Session) = {
    val U = TableQuery[UserTable]
//    Seq(
//      User("of546", "李允恒", RoleEnum.admin, false, None, None),
//      User("of557", "彭毅", RoleEnum.admin, false, None, None),
//      User("of729", "金卫", RoleEnum.admin, false, None, None),
//      User("of999", "龚平", RoleEnum.admin, false, None, None)
//    ).foreach(U.insert)
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
      Attribute(None, Option(1), "artifactId", Option("cardserverimpl")),
      Attribute(None, Option(1), "unpacked", Option("false"))
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
  }

  // 环境
  def environmentScript(implicit session: Session) = {
    Seq(
      Environment(None, "pytest", Option("py测试"), Option("172.19.3.201"), Option("172.17.0.1/24"), LevelEnum.unsafe),
      Environment(None, "dev", Option("开发"), Option("192.168.111.201"), Option("192.168.111.1/24"), LevelEnum.unsafe),
      Environment(None, "test", Option("测试"), Option("172.19.111.201"), Option("172.19.111.1/24"), LevelEnum.unsafe),
      Environment(None, "内测", Option("内测"), Option("192.168.111.210"), Option("172.19.3.1/24"), LevelEnum.unsafe)
    ).foreach(EnvironmentHelper.create)
  }

  def environmentProjectRelScript(implicit session: Session) = {
    Seq(
      EnvironmentProjectRel(None, Option(3), Option(1), "t-syndic", "t-minion", "172.19.3.134")
    ).foreach(EnvironmentProjectRelHelper.create)
  }

  // 权限
  def permissionScript(implicit session: Session) = {
  }

  // 任务
  def taskScript(implicit session: Session) = {
  }

  //任务命令关联表
  def taskCommandScript(implicit session: Session) = {
  }

  //任务队列表
  def taskQueueScript(implicit session: Session) = {
  }

  //定时任务表
  def taskSchemeScript(implicit session: Session) = {
  }

  def areaScript(implicit session: Session) = {
    Seq (
      Area(None, "测试", "t-syndic", "172.19.3.149"),
      Area(None, "test-syndic", "t-syndic", "172.19.3.132"),
      Area(None, "syndic", "syndic", "172.19.3.131")
    ).foreach(AreaHelper.create)
  }
}
