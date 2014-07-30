package utils

import java.io.File

/**
 * Provides directories used by Bugatti
 */
object Directory {

  val BugattiHome = (System.getProperty("bugatti.home") match {
    // -Dbugatti.home=<path>
    case path if (path != null) => new File(path)
    case _ => scala.util.Properties.envOrNone("BUGATTI_HOME") match {
      // environment variable BUGATTI_HOME
      case Some(env) => new File(env)
      // default is HOME/.bugatti
      case None => new File(System.getProperty("user.home"), ".bugatti")
    }
  }).getAbsolutePath

  val RepositoryHome = s"${BugattiHome}/repositories"

  def getRepositoryDir(projectId: Int): File = new File(s"${RepositoryHome}/${projectId}/.git")

}
