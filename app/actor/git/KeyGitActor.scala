package actor.git

import java.io.File
import java.security.KeyPairGenerator

import akka.actor.{Actor, ActorLogging}
import akka.event.LoggingReceive
import models.conf.User
import org.apache.commons.codec.binary.Base64
import org.eclipse.jgit.api.Git

import scala.io.Source
import scalax.file.Path

/**
 * Created by mind on 12/6/14.
 */

case class AddUser(user: User)

case class UpdateUser(user: User)

case class DeleteUser(jobNo: String)

case class AddUsers(users: Seq[User])

class KeyGitActor(gitInfo: GitInfo) extends Actor with ActorLogging {

  val keyPath = "auth/keys"

  var git: Git = null
  var keyDir: File = null

  override def preStart(): Unit = {
    val result = GitUtil.initGitDir(gitInfo)
    git = result._1
    keyDir = new File(s"${result._2.getAbsolutePath}/$keyPath")
  }

  override def receive = LoggingReceive {
    case AddUser(user) => {
      if (keyDir != null) {
        if (_generateKeyFile(user.sshKey, user.jobNo)) {
          git.add().addFilepattern(".").call()
          _commitGit(s"Create ${user.jobNo} file")
        }
      }
    }

    case UpdateUser(user) => {
      if (keyDir != null) {
        val keyInFile = _getKeyFromFile(user.jobNo)
        val currentKey = _getKey(user.sshKey, user.jobNo)
        if (keyInFile != currentKey) {
          val keyFile = _getPath(user.jobNo)
          keyFile.write(currentKey)
          git.add().addFilepattern(".").call()
          _commitGit(s"Update ${user.jobNo} file")
        }
      }
    }

    case DeleteUser(jobNo) => {
      if (keyDir != null) {
        val keyFile = _getPath(jobNo)
        if (keyFile.exists) {
          git.rm().addFilepattern(s"$keyPath/${jobNo.trim}").call()
          _commitGit(s"Remote $jobNo file")
        }
      }
    }

    case AddUsers(users) => {
      var needCommit = false
      users.foreach (user => {
        if (_generateKeyFile(user.sshKey, user.jobNo)) {
          needCommit = true
        }
      })

      if (needCommit) {
        git.add().addFilepattern(".").call()
        _commitGit("Add Users.")
      }
    }

    case x => log.warning(s"Unknown ${x}")
  }

  def _getKey(sshKey: Option[String], jobNo: String): String = {
    sshKey match {
      case Some(x) => {
        val keyStringList = x.split(" ")
        if (keyStringList.length > 1 && keyStringList(1).length > 300) {
          s"environment='SSH_USER=$jobNo' ${keyStringList(0)} ${keyStringList(1)}"
        } else {
          _getNewKey(jobNo)
        }
      }
      case None => _getNewKey(jobNo)
    }
  }

  def _getNewKey(jobNo: String): String = {
    val keygen = KeyPairGenerator.getInstance("RSA")
    keygen.initialize(2048)
    val strKey = new String(new Base64().encode(keygen.generateKeyPair().getPublic.getEncoded))
    s"environment='SSH_USER=$jobNo' ssh-rsa $strKey"
  }

  def _generateKeyFile(sshKey: Option[String], jobNo: String): Boolean = {
    val file = _getPath(jobNo)
    if (file.exists) {
      false
    } else {
      file.write(_getKey(sshKey, jobNo))
      true
    }
  }

  def _getKeyFromFile(jobNo: String): String = {
    val file = _getPath(jobNo)
    if (file.exists) {
      Source.fromFile(file.path).getLines().mkString
    } else {
      _getNewKey(jobNo)
    }
  }

  def _getPath(jobNo: String) = Path.fromString(s"${keyDir.getAbsolutePath}/${jobNo.trim}")

  def _commitGit(commitMsg: String): Unit = {
    git.commit().setMessage(commitMsg).call()
    git.push().call()
  }
}
