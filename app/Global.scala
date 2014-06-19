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
        AppData.userScript
        AppData.projectScript
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
    q.insert(Project(Some(1), "qianmi", 1, Some("1.1.1"), Some(new DateTime(2012, 12, 4, 0, 0, 0, 0))))
  }
}