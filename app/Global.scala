import enums.RoleEnum
import models.AppDB
import models.conf._
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
        TableQuery[SubPConfLogContentTable] ::
        TableQuery[SubPConfLogTable] ::
        TableQuery[SubPConfContentTable] ::
        TableQuery[SubPConfTable] ::
        TableQuery[SubProjectTable] ::
        TableQuery[PermissionTable] ::
        TableQuery[EnvironmentTable] ::
        TableQuery[MemberTable] ::
        TableQuery[ProjectTable] ::
        TableQuery[UserTable] ::
        Nil foreach { table =>
          if (!MTable.getTables(table.baseTableRow.tableName).list.isEmpty) table.ddl.drop
          table.ddl.create
        }

//        AppData.userScript
//        AppData.projectScript
//        AppData.memberScript
//        AppData.environmentScript
//        AppData.permissionScript
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
    q.insert(User("of546", Some("li"), Some(RoleEnum.admin), Some(true), Some("1.1.1.1"), Some(DateTime.now())))
  }

  // 项目表初始化
  def projectScript(implicit session: Session) = {
    val q = TableQuery[ProjectTable]
    if (!MTable.getTables(q.baseTableRow.tableName).list.isEmpty) q.ddl.drop
    q.ddl.create
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
  }

  // 权限
  def permissionScript(implicit session: Session) = {
    val q = TableQuery[PermissionTable]
    if (!MTable.getTables(q.baseTableRow.tableName).list.isEmpty) q.ddl.drop
    q.ddl.create
  }
}