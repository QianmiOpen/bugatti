package utils

import java.io.{File, FileFilter, FileInputStream}
import java.util.{List => JList, Map => JMap}

import models.conf._
import models.task.{TaskTemplate, TaskTemplateHelper, TaskTemplateStep, TaskTemplateStepHelper}
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.yaml.snakeyaml.Yaml
import play.api.{Play, Application, Logger}

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
        result = version.vs.endsWith("-SNAPSHOT")
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
  val app = Play.current

  lazy val logPath = app.configuration.getString("salt.log.dir").getOrElse("target/saltlogs")

  lazy val confPath: String = app.configuration.getString("salt.file.pkgs").getOrElse("target/pkgs")
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

    //    checkoutTemplate("r201407230954")
  }

  def reloadTemplates() {
    if (_git != null) {
      _git.pull().call()

      val tags = _git.tagList().call()
      val scriptNames = ScriptVersionHelper.allName()

      // 加载新的tag脚本
      tags.asScala.filterNot(tag => scriptNames.contains(tag.getName)).foreach { tag =>
        _git.checkout().setName(tag.getName).call()

//        ScriptVersionHelper.create(ScriptVersion(None, tag.getName))
        loadTemplateFromDir(tag.getName)
      }

      // 重新加载master,先讲老master更新掉

      _git.checkout().setName("master").call()
      loadTemplateFromDir("master")
    }
  }

  def loadTemplateFromDir(versionName: String) {
    val templateDir = new File(s"${_gitWorkDir.getAbsolutePath}/templates")
    templateDir.listFiles(new FileFilter {
      override def accept(pathname: File): Boolean = pathname.getName.endsWith(".yaml")
    }).foreach { file =>
      _initFromYaml(file)
    }
  }

  def _initFromYaml(file: File) = {
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