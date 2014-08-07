
import java.io.File

import actor.ActorUtils
import actor.salt.AddArea
import actor.task.MyActor
import enums.{LevelEnum, RoleEnum}
import models.AppDB
import models.conf._
import models.task._
import org.joda.time.DateTime
import org.pac4j.cas.client.CasClient
import org.pac4j.core.client.Clients
import org.pac4j.play.Config
import play.api.Play.current
import play.api._
import play.api.libs.Files
import utils.Directory._

import scala.slick.driver.MySQLDriver.simple._
import scala.slick.jdbc.meta.MTable

/**
 * 环境配置
 */
object Global extends GlobalSettings {

  override def beforeStart(app: Application) {
    System.setProperty("javax.net.ssl.trustStore", app.configuration.getString("ssl.trustStore").getOrElse("conf/certificate.jks"))

    // Create BUGATTI_HOME directory if it does not exist
    val dir = new File(BugattiHome)
    if (!dir.exists) {
      dir.mkdir
    }

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

    val _db = AppDB.db
    if (app.configuration.getBoolean("sql.db.init").getOrElse(true)) {
      _db.withSession { implicit session =>
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
          TableQuery[ScriptVersionTable] ::
          Nil foreach { table =>
          if (!MTable.getTables(table.baseTableRow.tableName).list.isEmpty) table.ddl.drop
          table.ddl.create
        }
      }

      AppData.initData
    }

    // 初始化区域
    AreaHelper.all.foreach(ActorUtils.areas ! AddArea(_))

    /**
     * mysql bug
     *
    import AutoUpdate._
    defining(getCurrentVersion()) { currentVersion =>
      if (currentVersion == headVersion) {
        Logger.debug("No update")
      } else if (!versions.contains(currentVersion)) {
        Logger.warn(s"Skip migration because ${currentVersion.versionString} is illegal version.")
      } else {
        _db.withSession { implicit session =>
          versions.takeWhile(_ != currentVersion).reverse.foreach(_.update)
          Files.writeFile(versionFile, headVersion.versionString)
          Logger.debug(s"Updated from ${currentVersion.versionString} to ${headVersion.versionString}")
        }
      }
    }
    */

    if (app.configuration.getBoolean("sql.test.init").getOrElse(true)) {
      AppTestData.initData
    }

    // 启动时reload一下所有标签
//    import actor.git.ScriptGitActor._
//    ActorUtils.scriptGit ! ReloadFormulasTemplate

//    SaltTools.refreshHostList(app)

    //需要在taskQueue执行之前被初始化
    MyActor.refreshSyndic
    MyActor.generateSchedule

    //查看队列表中是否有可执行任务
    val set = TaskQueueHelper.findEnvId_ProjectId()
    set.foreach {
      s =>
        MyActor.createNewTask(s._1, s._2)
    }
  }

}


object AppData {

  def initData = {
    // 初始化超级管理员
    Seq(
      User("of546", "李允恒", RoleEnum.admin, true, false, None, None),
      User("of557", "彭毅", RoleEnum.admin, false, false, None, None),
      User("of729", "金卫", RoleEnum.admin, false, false, None, None),
      User("of9999", "龚平", RoleEnum.admin, true, false, None, None)
    ).foreach(UserHelper.create)
  }
}

object AppTestData {

  def initData = {

    // 项目表初始化
    Seq(
      Project(None, "cardbase-master", 3, 5, Option(1), Option("1.6.4-SNAPSHOT"), Option(new DateTime()), List.empty[Variable]),
      Project(None, "cardbase-slave", 1, 5, Option(2), Option("1.6.4-SNAPSHOT"), Option(new DateTime()), List.empty[Variable]),
      Project(None, "qianmi1", 1, 5, Option(3), Option("1.6.4-SNAPSHOT"), Option(new DateTime()), List.empty[Variable]),
      Project(None, "qianmi2", 1, 5, Option(4), Option("1.6.4-SNAPSHOT"), Option(new DateTime()), List.empty[Variable]),
      Project(None, "qianmi3", 1, 5, Option(5), Option("1.6.4-SNAPSHOT"), Option(new DateTime()), List.empty[Variable])
    ).foreach(ProjectHelper.create)

    AppDB.db.withSession { implicit session =>
      // 初始化“cardbase-master”的attribute
      AttributeHelper._create(Seq(
        Attribute(None, Option(1), "groupId", Option("com.ofpay")),
        Attribute(None, Option(1), "artifactId", Option("cardserverimpl")),
        Attribute(None, Option(1), "unpacked", Option("false"))
      ))
    }

    //版本初始化
    Seq(
      Version(None, 1, "1.6.4-SNAPSHOT", new DateTime(2014, 6, 30, 7, 31)),
      Version(None, 1, "1.6.3-RELEASE", new DateTime(2014, 6, 29, 7, 31)),
      Version(None, 1, "1.6.3-SNAPSHOT", new DateTime(2014, 6, 28, 7, 31)),
      Version(None, 1, "1.6.2-RELEASE", new DateTime(2014, 6, 28, 7, 31)),
      Version(None, 1, "1.6.2-SNAPSHOT", new DateTime(2014, 6, 27, 7, 31)),
      Version(None, 1, "1.6.1-RELEASE", new DateTime(2014, 6, 26, 7, 31))
    ).foreach(VersionHelper.create)

    var seq = Seq(
      Environment(None, "pytest", Option("py测试"), Option("172.19.3.201"), Option("172.17.0.1/24"), LevelEnum.unsafe, globalVariable = List.empty[Variable]),
      Environment(None, "dev", Option("开发"), Option("192.168.111.201"), Option("192.168.111.1/24"), LevelEnum.unsafe, globalVariable = List.empty[Variable]),
      Environment(None, "test", Option("测试"), Option("172.19.111.201"), Option("172.19.111.1/24"), LevelEnum.unsafe, globalVariable = List.empty[Variable]),
      Environment(None, "内测", Option("内测"), Option("192.168.111.210"), Option("172.19.3.1/24"), LevelEnum.unsafe, ScriptVersionHelper.Master, globalVariable = List.empty[Variable])
    )
    //    for (i <- 5 to 55) {
    //      seq = seq :+ Environment(None, s"内测$i", Option("内测"), Option("192.168.111.210"), Option("172.19.3.1/24"), LevelEnum.unsafe)
    //    }
    seq.foreach(EnvironmentHelper.create)

    // 初始化环境关系表
    Seq(
      EnvironmentProjectRel(None, Option(4), Option(1), "t-syndic", "d6a597315b01", "172.19.3.134")
      //EnvironmentProjectRel(None, Option(4), Option(1), "t-syndic", "8e6499e6412a", "172.19.3.134")
    ).foreach(EnvironmentProjectRelHelper.create)

    // 初始化区域
    Seq(
      Area(None, "测试", "t-syndic", "192.168.59.3"),
      Area(None, "syndic", "syndic", "172.19.3.131")
    ).foreach(AreaHelper.create)
  }
}


object AutoUpdate {

  /**
   * Version of Bugatti
   *
   * @param majorVersion 主版本号
   * @param minorVersion 次版本号
   */
  case class Version(majorVersion: Int, minorVersion: Int) {

    /**
     * Execute update/MAJOR_MINOR.sql to update schema to this version.
     * If corresponding SQL file does not exist, this method do nothing.
     *
     * @param session
     */
    def update(implicit session: Session): Unit = {
      val sqlPath = s"upgrade/${majorVersion}_${minorVersion}.sql"

      Play.resourceAsStream(sqlPath).map { in =>
        val sql = scala.io.Source.fromInputStream(in, "UTF-8").mkString
        Logger.debug(sqlPath + "=" + sql)
        session.createStatement().executeUpdate(sql)
      }
    }

    /**
     * MAJOR.MINOR
     */
    val versionString = s"${majorVersion}.${minorVersion}"
  }

  val versions = Seq(
    Version(1, 3),
    Version(1, 2),
    Version(1, 1),
    Version(0, 0)
  )

  /**
   * The head version of Bugatti.
   */
  val headVersion = versions.head

  /**
   * The version file (BUGATTI_HOME/version).
   */
  lazy val versionFile = new File(BugattiHome, "version")

  /**
   * Returns the current version from thr version file.
   * @return
   */
  def getCurrentVersion(): Version = {
    if (versionFile.exists) {
      Files.readFile(versionFile).trim.split("\\.") match {
        case Array(majorVersion, minorVersion) => {
          versions.find { v =>
            v.majorVersion == majorVersion.toInt && v.minorVersion == minorVersion.toInt
          }.getOrElse(Version(0, 0))
        }
        case _ => Version(0, 0)
      }
    } else Version(0, 0)
  }

}
