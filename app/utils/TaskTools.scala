package utils

import java.io.File

import models.conf.VersionHelper
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import play.api.{Application, Logger}

/**
 * Created by jinwei on 1/7/14.
 */
object TaskTools {
  /**
   * 判断版本是否是snapshot
   * @param versionId
   * @return
   */
  def isSnapshot(versionId: Int): Boolean = {
    var result = true
    VersionHelper.findById(versionId) match {
      case Some(version) => {
        result = version.vs.contains("-SNAPSHOT")
      }
      case _ => {
        result = false
      }
    }
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

object ConfHelp {
  var _confPath: String = ""

  def initConfPath(app: Application) = {
    _confPath = app.configuration.getString("git.work.dir").getOrElse("/srv/salt")
  }

  def confPath {
    _confPath
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
    _gitWorkDir = new File(app.configuration.getString("git.work.dir").getOrElse("/srv/salt"))

    if (app.configuration.getBoolean("git.work.init").getOrElse(true)) {
      val gitRemoteUrl = app.configuration.getString("git.work.url").getOrElse("ssh://cicode@git.dev.ofpay.com:29418/cicode/salt-work.git")

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
      Logger.info(s"Init git: ${_git.getRepository}")
    }
  }

  def push(message: String) {
    if (_git != null) {
      val addRet = _git.add().addFilepattern(".").call()
      val commitRet = _git.commit().setMessage(message).call()
      val pushRet = _git.push().call()
      Logger.debug(s"git execute: addRet: $addRet, commitRet: $commitRet, pushRet: $pushRet")
    }
  }

  def workDir = {
    _gitWorkDir
  }
}