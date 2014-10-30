package actor.git

import java.io.{File, FileFilter, FileInputStream}
import java.text.SimpleDateFormat
import java.util.{List => JList, Map => JMap}

import akka.actor.{Actor, ActorLogging}
import enums.ActionTypeEnum._
import enums.{ActionTypeEnum, ItemTypeEnum}
import models.conf._
import models.task.{TaskTemplate, TaskTemplateHelper, TaskTemplateStep, TaskTemplateStepHelper}
import org.eclipse.jgit.api.ListBranchCommand.ListMode
import org.eclipse.jgit.api.{ListBranchCommand, Git}
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.joda.time.DateTime
import org.yaml.snakeyaml.Yaml
import play.api.Play

import scala.collection.JavaConverters._

/**
 * Created by mind on 7/24/14.
 */

import ScriptGitActor._

object ScriptGitActor {

  case class ReloadFormulasTemplate()

  val DateFormat = new SimpleDateFormat("yyyyMMdd-HHmmss-SSS")
}

class ScriptGitActor extends Actor with ActorLogging {
  val TemplateSuffix = ".yaml"
  val TemplatePath = "/templates"
  val Ok = "Ok"

  val app = Play.current
  lazy val gitFormulasDir: File = new File(app.configuration.getString("git.formulas.dir").getOrElse("target/formulas"))
  lazy val gitFormulasUrl = app.configuration.getString("git.formulas.url").getOrElse("http://git.dev.ofpay.com/git/TDA/salt-formulas.git")

  var gitFormulas: Git = null

  override def preStart(): Unit = {
    // 启动时初始化git目录
    if (app.configuration.getBoolean("git.init").getOrElse(true)) {
      gitFormulas = _initGitDir(gitFormulasDir, gitFormulasUrl)
    }
  }

  def _initGitDir(workDir: File, girUrl: String) = {
    val gitWorkDir = new File(s"${workDir.getAbsolutePath}/.git")
    if (!workDir.exists() || !gitWorkDir.exists()) {
      _delDir(workDir)
      val clone = Git.cloneRepository()
      clone.setDirectory(workDir).setURI(girUrl)
      clone.call()
    }

    val builder = new FileRepositoryBuilder()
    val repo = builder.setGitDir(gitWorkDir).build()

    val git = new Git(repo)
    log.info(s"Init git: ${git.getRepository}")
    git
  }

  override def receive: Receive = {
    case ReloadFormulasTemplate => {
      _reloadTemplates
      sender ! Ok
    }
    case x => log.warning(s"Unknown message ${x}")
  }

  def _reloadTemplates() {
    if (gitFormulas != null) {
      // 将版本从线上下载到线下
      val localBranchNames = gitFormulas.branchList().call.asScala.map(_.getName.split("/").last)
      val remoteBranches = gitFormulas.branchList().setListMode(ListMode.REMOTE).call().asScala

      remoteBranches.foreach { remoteBranche =>
        val branchName = remoteBranche.getName.split("/").last
        if (localBranchNames.contains(branchName)){
          gitFormulas.checkout().setName(branchName).call()
          gitFormulas.pull().call()
        }else{
          gitFormulas.checkout().setCreateBranch(true).setName(branchName).call()
        }
      }

      // 根据本地版本更新版本表格
      val branches = gitFormulas.branchList().call()
      if (branches != null) {
        branches.asScala.map { branch =>
          val branchName = branch.getName
          val branchId = branch.getObjectId.toString
          ScriptVersionHelper.getVersionByName(branchName) match {
            case Some(sv) => {
              if (sv.message.get == branchId) {
                _loadTemplateFromDir(branchName, sv.message)
                ScriptVersionHelper.updateVersionByName(ScriptVersion(None, branchName, message = Some(branchId)))
              }
            }
            case None =>
              _loadTemplateFromDir(branchName, None)
              ScriptVersionHelper.updateVersionByName(ScriptVersion(None, branchName, message = Some(branchId)))
          }
        }
      }
    } else {
      log.warning("Reload template, but git is null")
    }
  }

  def _loadTemplateFromDir(branchName: String, branchId: Option[String]) {
    log.debug(s"Load branch: ${branchName}, ${branchId}")

    gitFormulas.checkout().setName(branchName).call()

    branchId match {
      case Some(bId) =>
        val backupName = s"$branchName-$bId"
        TemplateItemHelper.updateScriptVersion(branchName, backupName)
        TaskTemplateHelper.updateScriptVersion(branchName, backupName)
        TemplateAliasHelper.updateScriptVersion(branchName, backupName)
      case None =>
    }

    val templateDir = new File(s"${gitFormulasDir.getAbsolutePath}${TemplatePath}")
    templateDir.listFiles(new FileFilter {
      override def accept(pathname: File): Boolean = pathname.getName.endsWith(TemplateSuffix)
    }).foreach { file =>
      log.debug(s"Load file: ${file}")
      _initFromYaml(file, branchName)
    }
  }

  def _getProjectIdsFromProjectName(projectNames: String): Seq[Int] = {
    if (projectNames == null) {
      Seq.empty
    } else {
      projectNames.trim.split(",").filter(_.nonEmpty)
        .map(p => ProjectHelper.findByName(p.trim)).filter(_.nonEmpty).map(_.get.id.get)
    }
  }

  def _getProjectIdFromProjectName(typeName: String, projectName: String): Int = {
    if (projectName == null || projectName.isEmpty) {
      ProjectHelper.ProjectNotExistId
    } else {
      ProjectHelper.findByName(projectName.trim) match {
        case Some(p) => {
          val template = TemplateHelper.findById(p.templateId).get
          if (template.name == typeName) {
            p.id.get
          } else {
            ProjectHelper.ProjectNotExistId
          }
        }
        case _ =>
          ProjectHelper.ProjectNotExistId

      }
    }
  }

  def _initFromYaml(file: File, tagName: String) = {
    val yaml = new Yaml()
    val io = new FileInputStream(file)
    val template = yaml.load(io).asInstanceOf[JMap[String, AnyRef]]

    val templateName = template.get("name").asInstanceOf[String]

    val templateId = TemplateHelper.findByName(templateName) match {
      case Some(temp) => {
        val templateId = temp.id.get
        // 更新模板说明
        val tempObj = temp.copy(remark = Some(template.get("remark").asInstanceOf[String]),
          dependentProjectIds = Seq.empty)

        TemplateHelper.update(templateId, tempObj)

        //删除原模板
        TemplateDependenceHelper.deleteByTemplateId(templateId)

        //        _getProjectIdsFromProjectName(template.get("dependences").asInstanceOf[String])
        val templateDependenceProjects = template.get("dependences") match {
          case null => Seq.empty[TemplateDependence]
          case d: Any =>
            d.asInstanceOf[JList[JMap[String, String]]].asScala.map { dependence =>
              val defaultId = _getProjectIdFromProjectName(dependence.get("type"), dependence.get("default"))
              val projectDep = TemplateDependence(None, templateId, dependence.get("name"), dependence.get("type"), Some(dependence.get("description")), defaultId)
              TemplateDependenceHelper.create(projectDep)
              projectDep
            }
        }

        // 更新项目依赖
        ProjectHelper.allByTemplateId(templateId).foreach { project =>
          ProjectHelper.updateDependencesAlone(project, templateDependenceProjects)
        }

        templateId
      }
      case None => TemplateHelper.create(Template(None, templateName, Some(template.get("remark").asInstanceOf[String]),
        _getProjectIdsFromProjectName(template.get("dependences").asInstanceOf[String])))
    }

    // 创建template关联的alias
    var templateAliass = template.get("aliass")
    if (templateAliass != null) {
      templateAliass.asInstanceOf[JList[JMap[String, String]]].asScala.foreach { alias =>
        TemplateAliasHelper.create(TemplateAlias(None, Some(templateId), alias.get("aliasName"), alias.get("aliasValue"), alias.get("aliasDesc"), tagName))
      }
    }

    // 创建template关联的item
    val templateItems = template.get("items")
    if (templateItems != null) {
      templateItems.asInstanceOf[JList[JMap[String, String]]].asScala.zipWithIndex.foreach {
        case (x: JMap[String, String], index) =>
          TemplateItemHelper.create(TemplateItem(None, Some(templateId), x.get("itemName"), Some(x.get("itemDesc")),
            ItemTypeEnum.withName(Option(x.get("itemType")).getOrElse(ItemTypeEnum.attribute.toString)),
            Some(x.get("default")), index, tagName))
      }
    }

    // 创建template关联的actions
    val actions = template.get("actions")
    if (actions != null) {
      actions.asInstanceOf[JList[JMap[String, AnyRef]]].asScala.zipWithIndex.foreach {
        case (action, index) =>
          val taskId = TaskTemplateHelper.create(TaskTemplate(None, action.get("name").asInstanceOf[String],
            action.get("css").asInstanceOf[String], action.get("versionMenu").asInstanceOf[Boolean],
            templateId, index + 1, tagName,
            ActionTypeEnum.withName(Option(action.get("actionType")).getOrElse(ActionTypeEnum.project.toString).asInstanceOf[String])))

          val steps = action.get("steps").asInstanceOf[JList[JMap[String, String]]].asScala
          steps.zipWithIndex.foreach { case (step, index) =>
            val seconds = step.get("seconds").asInstanceOf[Int]
            TaskTemplateStepHelper.create(TaskTemplateStep(None, taskId, step.get("name"), step.get("sls"), if (seconds <= 0) 3 else seconds, index + 1))
          }
      }
    }
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