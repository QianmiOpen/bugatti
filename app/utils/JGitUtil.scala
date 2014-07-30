package utils

import ControlUtil._
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib._

/**
 * Provides complex JGit operations.
 */
object JGitUtil {

  def initRepository(dir: java.io.File): Unit = {
    using(new RepositoryBuilder().setGitDir(dir).build){ repository =>
      repository.create
      setReceivePack(repository)
    }
  }

  private def setReceivePack(repository: org.eclipse.jgit.lib.Repository): Unit =
    defining(repository.getConfig){ config =>
      config.setBoolean("http", null, "receivepack", true)
      config.save
    }

  def isEmpty(git: Git): Boolean = git.getRepository.resolve(Constants.HEAD) == null

}
