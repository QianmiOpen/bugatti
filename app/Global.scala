
import java.io.File
import java.util.Scanner
import actor.git.AddUsers
import actor.git.ScriptGitActor.ReloadFormulasTemplate
import actor.conf.ConfigureActor
import play.api.mvc.RequestHeader
import play.api.mvc.Results._
import utils.ControlUtil._

import actor.ActorUtils
import actor.salt.AddArea
import actor.task.MyActor
import enums.{ContainerTypeEnum, LevelEnum, RoleEnum}
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

import scala.concurrent.Future
import scala.slick.driver.MySQLDriver.simple._
import scala.slick.jdbc.meta.MTable

/**
 * 环境配置
 */
object Global extends GlobalSettings {

  override def onError(request: RequestHeader, ex: Throwable) = ex.getCause match {
    case _: NoSuchElementException  => Future.successful(BadRequest("Bad Request: " + ""))
    case _ => super.onError(request, ex)
  }

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

    /**
     * 升级脚本文件需满足3个条件：
     * 1. 不允许有select语句
     * 2. 不允许有注释
     * 3. 以 ; 分号结尾一个完整sql语句
     */
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

    if (app.configuration.getBoolean("sql.db.init").getOrElse(true)) {
      _db.withSession { implicit session =>
        TableQuery[ConfLogContentTable] ::
          TableQuery[ConfLogTable] ::
          TableQuery[ConfContentTable] ::
          TableQuery[ConfTable] ::
          TableQuery[VersionTable] ::
          TableQuery[PermissionTable] ::
          TableQuery[EnvironmentTable] ::
          TableQuery[ProjectMemberTable] ::
          TableQuery[ProjectTable] ::
          TableQuery[AttributeTable] ::
          TableQuery[TemplateItemTable] ::
          TableQuery[TemplateTable] ::
          TableQuery[UserTable] ::
          TableQuery[TemplateActionTable] ::
          TableQuery[TemplateActionStepTable] ::
          TableQuery[TaskCommandTable] ::
          TableQuery[TaskQueueTable] ::
          TableQuery[TaskSchemeTable] ::
          TableQuery[TaskTable] ::
          TableQuery[AreaTable] ::
          TableQuery[HostTable] ::
          TableQuery[ScriptVersionTable] ::
          TableQuery[VariableTable] ::
          TableQuery[ProjectDependencyTable] ::
          TableQuery[TemplateAliasTable] ::
          Nil foreach { table =>
          if (!MTable.getTables(table.baseTableRow.tableName).list.isEmpty) table.ddl.drop
          table.ddl.create
        }
      }

      AppData.initData
    }

    // 初始化区域
    if (app.configuration.getBoolean("area.init").getOrElse(true)) {
      AreaHelper.all.foreach(ActorUtils.areas ! AddArea(_))
    }

    // 启动时，重新加载formulas
    ActorUtils.scriptGit ! ReloadFormulasTemplate

    // 启动时，增加已有账号的key文件
    ActorUtils.keyGit ! AddUsers(UserHelper.all())

    if (app.configuration.getBoolean("sql.test.init").getOrElse(true)) {
      AppTestData.initData
    }

    //需要在taskQueue执行之前被初始化
    MyActor.generateSchedule

    //查看队列表中是否有可执行任务
    val set = TaskQueueHelper.findEnvId_ProjectId()
    set.foreach {
      s =>
        MyActor.createNewTask(s._1, s._2, s._3)
    }

    //初始化ConfigureActor中的projectMap
    ConfigureActor.initProjectMap
  }
}


object AppData {

  def initData = {
    // 初始化超级管理员
    Seq(
      User("of546", "李允恒", RoleEnum.admin, true, false, None, None, None),
      User("of557", "彭毅", RoleEnum.admin, true, false, None, None, None),
      User("of729", "金卫", RoleEnum.admin, false, false, None, None, None),
      User("of9999", "龚平", RoleEnum.admin, true, false, None, None, None)
    ).foreach(UserHelper.create)
  }
}

object AppTestData {

  def initData = {

    // 项目表初始化
    Seq(
      Project(None, "cardbase-master", Option(""), 3, 5, Option(1), Option("1.6.4-SNAPSHOT"), Option(new DateTime())),
      Project(None, "cardbase-slave", Option(""), 1, 5, Option(2), Option("1.6.4-SNAPSHOT"), Option(new DateTime())),
      Project(None, "qianmi1", Option(""), 1, 5, Option(3), Option("1.6.4-SNAPSHOT"), Option(new DateTime())),
      Project(None, "qianmi2", Option(""), 1, 5, Option(4), Option("1.6.4-SNAPSHOT"), Option(new DateTime())),
      Project(None, "qianmi3", Option(""), 1, 5, Option(5), Option("1.6.4-SNAPSHOT"), Option(new DateTime()))
    ).foreach(ProjectHelper.create)

    AppDB.db.withSession { implicit session =>
      // 初始化“cardbase-master”的attribute
      AttributeHelper._create(Seq(
        Attribute(None, Option(1), "t_groupId", Option("com.ofpay")),
        Attribute(None, Option(1), "t_artifactId", Option("cardserverimpl")),
        Attribute(None, Option(1), "t_unpacked", Option("false"))
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
      Environment(None, "pytest", None, Option("py测试"), Option("172.19.3.201"), Option("172.17.0.1/24"), LevelEnum.unsafe),
      Environment(None, "dev", None, Option("开发"), Option("192.168.111.201"), Option("192.168.111.1/24"), LevelEnum.unsafe),
      Environment(None, "test", None, Option("测试"), Option("172.19.111.201"), Option("172.19.111.1/24"), LevelEnum.unsafe),
      Environment(None, "内测", None, Option("内测"), Option("192.168.111.210"), Option("172.19.3.1/24"), LevelEnum.unsafe, ScriptVersionHelper.Master)
    )
    //    for (i <- 5 to 55) {
    //      seq = seq :+ Environment(None, s"内测$i", Option("内测"), Option("192.168.111.210"), Option("172.19.3.1/24"), LevelEnum.unsafe)
    //    }
    seq.foreach(EnvironmentHelper.create)

    // 初始化环境关系表
    Seq(
      Host(None, Option(4), Option(1), Option(1), "t-syndic", "d6a597315b01", "172.19.3.134", ContainerTypeEnum.vm
        , Option(""), Option("") ,Seq.empty[Variable])
      //EnvironmentProjectRel(None, Option(4), Option(1), "t-syndic", "8e6499e6412a", "172.19.3.134")
    ).foreach(HostHelper.create)

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
      val sqlPath = s"conf/upgrade/${majorVersion}_${minorVersion}.sql"

      using(new Scanner(Play.getFile(sqlPath), "UTF-8")){ scanner =>
        val sc = scanner.useDelimiter(";")
        using(session.createStatement()){ stat =>
          while (sc.hasNext) {
            val sql = sc.next
            if (sql.replaceAll("""\s""", "").nonEmpty) {
              Logger.debug(sqlPath + "=" + sql)
              stat.executeUpdate(sql)
            }
          }
        }
      }

//      Play.resourceAsStream(sqlPath).map { in =>
//        val sql = scala.io.Source.fromInputStream(in, "UTF-8").mkString
//        Logger.debug(sqlPath + "=" + sql)
//        session.createStatement().executeUpdate(sql)
//      }
    }

    /**
     * MAJOR.MINOR
     */
    val versionString = s"${majorVersion}.${minorVersion}"
  }

  val versions = Seq(
    Version(1, 11),
    Version(1, 10),
    Version(1, 9),
    Version(1, 8),
    Version(1, 7),
    Version(1, 6),
    Version(1, 5),
    Version(1, 4),
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
