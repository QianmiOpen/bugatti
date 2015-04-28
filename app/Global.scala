
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.{StandardOpenOption, Files}
import java.util.Scanner
import actor.git.AddUsers
import actor.git.ScriptGitActor.ReloadFormulasTemplate
import actor.conf.ConfigureActor
import play.api.mvc.RequestHeader
import play.api.mvc.Results._
import utils.ControlUtil._

import actor.ActorUtils
import actor.salt.AddSpirit
import actor.task.MyActor
import enums.{StateEnum, ContainerTypeEnum, LevelEnum, RoleEnum}
import models.AppDB
import models.conf._
import models.task._
import org.joda.time.DateTime
import play.api.Play.current
import play.api._
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
    // Create BUGATTI_HOME directory if it does not exist
    val dir = new File(BugattiHome)
    if (!dir.exists) {
      dir.mkdir
    }
  }

  override def onStart(app: Application) {
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
        play.api.Logger.info(s"currentVersion=${currentVersion}, tailVersion=${lastVersion}")
        // init
        if (currentVersion == lastVersion) {
          _db.withSession { implicit session =>
            TableQuery[AreaTable] ::
              TableQuery[AreaEnvironmentRelTable] ::
              TableQuery[AttributeTable] ::
              TableQuery[ComponentMd5sumTable] ::
              TableQuery[ConfTable] ::
              TableQuery[ConfContentTable] ::
              TableQuery[ConfLogTable] ::
              TableQuery[ConfLogContentTable] ::
              TableQuery[EnvironmentTable] ::
              TableQuery[EnvironmentMemberTable] ::
              TableQuery[HostTable] ::
              TableQuery[ProjectTable] ::
              TableQuery[ProjectDependencyTable] ::
              TableQuery[ProjectMemberTable] ::
              TableQuery[ScriptVersionTable] ::
              TableQuery[SpiritTable] ::
              TableQuery[TemplateTable] ::
              TableQuery[TemplateAliasTable] ::
              TableQuery[TemplateDependenceTable] ::
              TableQuery[TemplateItemTable] ::
              TableQuery[UserTable] ::
              TableQuery[VariableTable] ::
              TableQuery[VersionTable] ::
              TableQuery[TaskTable] ::
              TableQuery[TaskCommandTable] ::
              TableQuery[TaskQueueTable] ::
              TableQuery[TaskSchemeTable] ::
              TableQuery[TemplateActionTable] ::
              TableQuery[TemplateActionStepTable] ::
              Nil foreach { table =>
              if (MTable.getTables(table.baseTableRow.tableName).list.nonEmpty) table.ddl.drop
              table.ddl.create
            }
          }
        }
        // update
        _db.withSession { implicit session =>
          versions.takeWhile(_ != currentVersion).reverse.foreach(_.update)
          Files.write(versionFile.toPath, headVersion.versionString.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE)
          Logger.debug(s"Updated from ${currentVersion.versionString} to ${headVersion.versionString}")
        }
      }

    }

    // 初始化spirit
    if (app.configuration.getBoolean("spirit.init").getOrElse(true)) {
      SpiritHelper.all.foreach(ActorUtils.spirits ! AddSpirit(_))
    }

    // 启动时，重新加载formulas
    ActorUtils.scriptGit ! ReloadFormulasTemplate

    // 启动时，增加已有账号的key文件
    ActorUtils.keyGit ! AddUsers(UserHelper.all())

    if (app.configuration.getBoolean("sql.test.init").getOrElse(true)) {
      AppTestData.initData
    }

    //需要在taskQueue执行之前被初始化
    MyActor.generateSchedule()

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

    val seq = Seq(
      Environment(None, "pytest", Option("py测试"), Option("172.19.3.201"), Option("172.17.0.1/24"), LevelEnum.unsafe, false),
      Environment(None, "dev", Option("开发"), Option("192.168.111.201"), Option("192.168.111.1/24"), LevelEnum.unsafe, false),
      Environment(None, "test", Option("测试"), Option("172.19.111.201"), Option("172.19.111.1/24"), LevelEnum.unsafe, true),
      Environment(None, "内测", Option("内测"), Option("192.168.111.210"), Option("172.19.3.1/24"), LevelEnum.unsafe, false, ScriptVersionHelper.Master)
    )
    seq.foreach(EnvironmentHelper.create(_, ""))

    // 初始化环境关系表
    Seq(
      Host(None, Option(4), Option(1), Option(1), Option(1), "t-syndic", 1, "d6a597315b01", "172.19.3.134", StateEnum.noKey, ContainerTypeEnum.vm
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

    }

    /**
     * MAJOR.MINOR
     */
    val versionString = s"${majorVersion}.${minorVersion}"
  }

  val versions = Seq(
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
   * Init Bugatti.
   */
  val lastVersion = versions.last

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
      new String(Files.readAllBytes(versionFile.toPath), StandardCharsets.UTF_8).trim.split("\\.") match {
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
