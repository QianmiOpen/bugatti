package actor.git

import java.io.File

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import play.api.{Logger, Play}

/**
 * Created by mind on 12/6/14.
 */

case class GitInfo(gitUrl: String, workDir: File)

object GitUtil {
  def init(app: play.api.Application) = app.configuration.getBoolean("git.init").getOrElse(true)

  def getGitInfo(app: play.api.Application, gitType: String): GitInfo = {
    val workDir = new File(app.configuration.getString(s"git.$gitType.dir").getOrElse(s"target/$gitType"))
    val gitUrl = app.configuration.getString(s"git.$gitType.url").getOrElse(s"http://git.dev.qianmi.com/tda/salt-$gitType.git")

    GitInfo(gitUrl, workDir)
  }

  def initGitDir(gitInfo: GitInfo) = {

    val gitWorkDir = new File(s"${gitInfo.workDir.getAbsolutePath}/.git")
    if (!gitInfo.workDir.exists() || !gitWorkDir.exists()) {
      _delDir(gitInfo.workDir)
      val clone = Git.cloneRepository()
      clone.setDirectory(gitInfo.workDir).setURI(gitInfo.gitUrl)
      clone.call()
    }

    val builder = new FileRepositoryBuilder()
    val repo = builder.setGitDir(gitWorkDir).build()

    val git = new Git(repo)
    Logger.info(s"Init git: ${git.getRepository}")

    (git, gitInfo.workDir)
  }

  def _delDir(dir: File) {
    if (dir.exists()) {
      dir.listFiles().foreach { x =>
        if (x.isDirectory()) {
          _delDir(x)
        } else {
          x.delete()
        }
      }
      dir.delete()
    }
  }
}
