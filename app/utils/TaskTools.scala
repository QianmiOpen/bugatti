package utils

import java.io.{FileInputStream, FilenameFilter, FileFilter, File}
import java.util.{List, Map}

import models.conf._
import models.task.{TaskTemplateStep, TaskTemplateStepHelper, TaskTemplate, TaskTemplateHelper}
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.yaml.snakeyaml.Yaml
import play.api.{Application, Logger}

import java.util.{List => JList, Map => JMap}

import scala.collection.JavaConverters._

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
    _confPath = app.configuration.getString("salt.file.pkgs").getOrElse("target/pkgs")
  }

  def confPath = {
    _confPath
  }
}

object FormulasHelp {
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
    _gitWorkDir = new File(app.configuration.getString("git.work.dir").getOrElse("target/formulas"))

    if (app.configuration.getBoolean("git.work.init").getOrElse(true)) {
      val gitRemoteUrl = app.configuration.getString("git.work.url").getOrElse("http://git.dev.ofpay.com/git/TDA/salt-formulas.git")

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

    reloadTemplates
  }

  def reloadTemplates() {
    if (_git != null) {
      _git.pull().call()

      val templateDir = new File(s"${_gitWorkDir.getAbsolutePath}/templates")
      templateDir.listFiles(new FileFilter {
        override def accept(pathname: File): Boolean = pathname.getName.endsWith(".yaml")
      }).foreach { file =>
        initFromYaml(file)
      }
    }
  }

  def initFromYaml(file: File) = {
    val yaml = new Yaml()
    val io = new FileInputStream(file)
    val template = yaml.load(io).asInstanceOf[JMap[String, AnyRef]]

    val templateName = template.get("name").asInstanceOf[String]


    val templateId = TemplateHelper.findByName(templateName) match {
      case Some(temp) => {
        val templateId = temp.id.get
        // 删除关联数据
        TemplateItemHelper.deleteItemsByTemplateId(templateId)
        TaskTemplateHelper.findTaskTemplateByTemplateId(templateId).foreach { taskTemplate =>
          TaskTemplateStepHelper.deleteStepsByTaskTemplateId(taskTemplate.id.get)
        }
        TaskTemplateHelper.deleteTaskTemplateByTemplateId(templateId)

        // 更新模板说明
        TemplateHelper.update(templateId, temp.copy(remark = Some(template.get("remark").asInstanceOf[String])))
        templateId
      }
      case None => TemplateHelper.create(Template(None, templateName, Some(template.get("remark").asInstanceOf[String])))
    }

    // 创建template关联的item
    val templateItems = template.get("items")
    if (templateItems != null) {
      templateItems.asInstanceOf[JList[JMap[String, String]]].asScala.zipWithIndex.foreach {
        case (x: JMap[String, String], index) =>
          TemplateItemHelper.create(TemplateItem(None, Some(templateId), x.get("itemName"), Some(x.get("itemDesc")), Some(x.get("default")), index))
      }
    }

    // 创建template关联的actions
    val actions = template.get("actions")
    if (actions != null) {
      actions.asInstanceOf[JList[JMap[String, AnyRef]]].asScala.zipWithIndex.foreach {
        case (action, index) =>
          val taskId = TaskTemplateHelper.create(TaskTemplate(None, action.get("name").asInstanceOf[String], action.get("css").asInstanceOf[String], action.get("versionMenu").asInstanceOf[Boolean], templateId, index + 1))
          val steps = action.get("steps").asInstanceOf[JList[JMap[String, String]]].asScala
          steps.zipWithIndex.foreach { case (step, index) =>
            val seconds = step.get("seconds").asInstanceOf[Int]
            TaskTemplateStepHelper.create(TaskTemplateStep(None, taskId, step.get("name"), step.get("sls"), if (seconds <= 0) 3 else seconds, index + 1))
          }
      }
    }
  }
}