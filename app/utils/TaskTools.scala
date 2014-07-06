package utils

import java.io.File

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import play.api.{Application, Logger}

/**
 * Created by jinwei on 1/7/14.
 */
object TaskTools {
  /**
   * 判断版本是否是snapshot
   * @param version
   * @return
   */
  def isSnapshot(version: String): Boolean = {
    val result = version.contains("-SNAPSHOT")
    Logger.info("isSnapshot ==>" + result.toString)
    result
  }

  /**
   * 去除字符串两边的引号
   * @param s
   * @return
   */
  def trimQuotes(s: String): String = {
    s.trim.stripPrefix("\"").stripSuffix("\"").trim
  }
}

object GitHelp {
  var _git: Git = null
  var _gitWorkDir: File = null

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

  def checkGitWorkDir(app: Application) = {
    val gitRemoteUrl = app.configuration.getString("git.work.url").getOrElse("ssh://cicode@git.dev.ofpay.com:29418/cicode/salt-work.git")
    _gitWorkDir = new File(app.configuration.getString("git.work.dir").getOrElse("target/salt-work"))
    val gitWorkDir_git = new File(s"${_gitWorkDir.getAbsolutePath}/.git")
    if (!_gitWorkDir.exists() || !gitWorkDir_git.exists()) {
      _delDir(_gitWorkDir)
      val clone = Git.cloneRepository()
      clone.setDirectory(_gitWorkDir).setURI(gitRemoteUrl)
      clone.call()
    }

    val builder = new FileRepositoryBuilder()
    val repo = builder.setGitDir(gitWorkDir_git).build()
    _git = new Git(repo)
  }

  def push(message: String) {
    if (_git != null) {
      _git.add().addFilepattern(".").call()
      _git.commit().setMessage(message).call()
      _git.push().call()
    }
  }

  def workDir = {
    _gitWorkDir
  }
}