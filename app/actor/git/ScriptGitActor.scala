package actor.git

import java.io.{File, FileFilter, FileInputStream, InputStreamReader}
import java.nio.charset.{Charset, CodingErrorAction}
import java.text.SimpleDateFormat
import java.util.{List => JList, Map => JMap}

import akka.actor.{Actor, ActorLogging}
import akka.event.LoggingReceive
import enums.{ActionTypeEnum, ItemTypeEnum}
import models.conf._
import models.task.{TemplateAction, TemplateActionHelper, TemplateActionStep, TemplateActionStepHelper}
import org.eclipse.jgit.api.CreateBranchCommand.SetupUpstreamMode
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.ListBranchCommand.ListMode
import org.joda.time.DateTime
import org.yaml.snakeyaml.Yaml
import play.api.Logger
import utils.ConfHelp

import scala.collection.JavaConverters._
import sys.process._
import scala.language.postfixOps

/**
 * Created by mind on 7/24/14.
 */

import actor.git.ScriptGitActor._

object ScriptGitActor {

  case class ReloadFormulasTemplate()

  val DateFormat = new SimpleDateFormat("yyyyMMdd-HHmmss-SSS")
}

class ScriptGitActor(gitInfo: GitInfo) extends Actor with ActorLogging {
  val TemplateSuffix = ".yaml"
  val TemplatePath = "/templates"
  val Ok = "Ok"

  var gitFormulas: Git = null
  var gitFormulasDir: File = null

  override def preStart(): Unit = {
      val result = GitUtil.initGitDir(gitInfo)
      gitFormulas = result._1
      gitFormulasDir = result._2
  }

  override def receive: Receive = LoggingReceive {
    case ReloadFormulasTemplate => {
      _reloadTemplates
      sender ! Ok
    }
    case x => log.warning(s"Unknown message ${x}")
  }

  def _reloadTemplates() {
    if (gitFormulas != null) {
      // 将版本从git仓库下载到本地
      val localBranchNames = gitFormulas.branchList().call.asScala.map(_.getName.split("/").last)
      val remoteBranches = gitFormulas.branchList().setListMode(ListMode.REMOTE).call().asScala

      remoteBranches.foreach { remoteBranche =>
        val branchName = remoteBranche.getName.split("/").last
        if (localBranchNames.contains(branchName)) {
          gitFormulas.checkout().setName(branchName).call()
          gitFormulas.pull().call()
        } else {
          gitFormulas.checkout().setCreateBranch(true).setName(branchName).setUpstreamMode(SetupUpstreamMode.TRACK).setStartPoint(s"origin/$branchName").call()
        }
      }

      // 根据本地版本更新版本表格
      val branches = gitFormulas.branchList().call()
      if (branches != null) {
        branches.asScala.map { branch =>
          val branchName = branch.getName.split("/").last
          val branchId = branch.getObjectId.getName
          if (!ScriptVersionHelper.isSameBranch(branchName, branchId)){
            _loadTemplateFromDir(branchName)
            ScriptVersionHelper.updateVersionByName(ScriptVersion(None, branchName, message = Some(branchId)))

            //TODO 更新分支下的组件MD5
            val seq = ScriptVersionHelper.all()
            _loadComponentMd5(branchName, seq)

          }
        }
      }
    } else {
      log.warning("Reload template, but git is null")
    }
  }

  def _loadTemplateFromDir(branchName: String) {
    log.debug(s"Load branch: ${branchName}")

    gitFormulas.checkout().setName(branchName).setForce(true).call()

    val backupName = s"$branchName-bak-${DateFormat.format(DateTime.now.toDate)}"
    TemplateItemHelper.updateScriptVersion(branchName, backupName)
    TemplateActionHelper.updateScriptVersion(branchName, backupName)
    TemplateAliasHelper.updateScriptVersion(branchName, backupName)

    val templateDir = new File(s"${gitFormulasDir.getAbsolutePath}${TemplatePath}")
    templateDir.listFiles(new FileFilter {
      override def accept(pathname: File): Boolean = pathname.getName.endsWith(TemplateSuffix)
    }).foreach { file =>
      log.debug(s"Load file: ${file}")
      _initFromYaml(file, branchName)
    }
  }

  def _loadComponentMd5(branchName: String, seq: Seq[ScriptVersion]) {
    log.debug(s"Load branch component: ${branchName}")
    val scriptDir = new File(s"${gitFormulasDir.getAbsolutePath}")
    scriptDir.listFiles(new FileFilter {
      override def accept(pathname: File): Boolean = !_checkComponentIgnore(pathname.getName)
    }).foreach { file =>
      log.debug(s"Load file md5sum: ${file}")
      val md5sum = (s"find ${file.getAbsolutePath} -type f -exec md5sum {} +" #| "sort" #| "md5sum" !!)
      log.debug(s"Load file : ${file}, md5sum: ${md5sum}")
      val scriptVersionId = seq.filter(_.name == branchName).head.id
      ComponentMd5sumHelper.update(ComponentMd5sum(None, 0, scriptVersionId.getOrElse(0), file.getName(), md5sum))
    }
  }

  def _checkComponentIgnore(pathName: String): Boolean ={
    ConfHelp.componentIgnore.contains(pathName)
  }

  def _getProjectIdFromProjectName(typeName: String, projectName: String): Int = {
    if (projectName == null || projectName.isEmpty) {
      ProjectHelper.ProjectNotExistId
    } else {
      ProjectHelper.findByName(projectName.trim) match {
        case Some(p) => {
          TemplateHelper.findById(p.templateId) match {
            case Some(template) =>
              if (template.name == typeName) {
                p.id.get
              } else {
                ProjectHelper.ProjectNotExistId
              }
            case _ =>
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
    val decoder = Charset.forName("UTF-8").newDecoder()
    decoder.onMalformedInput(CodingErrorAction.IGNORE)

    val template = yaml.load(new InputStreamReader(io, decoder)).asInstanceOf[JMap[String, AnyRef]]

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

        templateId
      }
      case None => TemplateHelper.create(Template(None, templateName, Some(template.get("remark").asInstanceOf[String]), Seq.empty))
    }

    // 更新模板依赖
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

    // 创建template关联的alias
    var templateAliass = template.get("aliass")
    if (templateAliass != null) {
      templateAliass.asInstanceOf[JList[JMap[String, String]]].asScala.foreach { alias =>
        TemplateAliasHelper.create(TemplateAlias(None, Some(templateId), alias.get("aliasName"), alias.get("aliasValue"), alias.get("aliasDesc"), tagName))
      }
    }

    // 创建template关联的item
    val templateItems = template.get("items")
    val envs = EnvironmentHelper.allByBranch(tagName)
    val projects = ProjectHelper.allByTemplateId(templateId)
    if (templateItems != null) {
      templateItems.asInstanceOf[JList[JMap[String, String]]].asScala.zipWithIndex.foreach {
        case (x: JMap[String, String], index) =>
          val itemName = x.get("itemName")
          TemplateItemHelper.create(TemplateItem(None, Some(templateId), itemName, Some(x.get("itemDesc")),
            ItemTypeEnum.withName(Option(x.get("itemType")).getOrElse(ItemTypeEnum.attribute.toString)),
            Some(x.get("default")), index, tagName))

          //找到该模板下的所有项目，查看是否存在，不存在的加上属性以及默认值
          if(x.get("default") != null){
              val default = x.get("default")
              Logger.info(s"${itemName} => default => ${x.get("default")}")
              projects.foreach {
                p =>
                  Logger.info(s"${itemName} => itemType => ${x.get("itemType")}")
                  val itemType = x.get("itemType")
                  if(itemType != null) {
                    //var
                    envs.foreach {
                      e =>
                        val seq = VariableHelper.findByEnvId_ProjectId(e.id.get, p.id.get).filter(s => s.name == itemName)
                        if (seq.size == 0) {
                          VariableHelper.create(Seq(Variable(None, e.id, p.id, itemName, default, enums.LevelEnum.unsafe)))
                        }
                    }
                  }else {
                    //attr
                    val seq = AttributeHelper.findByProjectId(p.id.get).filter(s => s.name == itemName)
                    if(seq.size == 0){
                      AttributeHelper.create(Seq(Attribute(None, p.id, itemName, Option(default))))
                    }
                  }
              }
          }
      }
    }

    // 创建template关联的cluster级别的actions
    _genActions(template, "actions", templateId, tagName)

    // 创建template关联的project级别的actions
    _genActions(template, "actionsProject", templateId, tagName)

  }

  def _genActions(template: JMap[String, AnyRef], item: String, templateId: Int, tagName: String) = {
    val actionType = item match {
      case "actions" => ActionTypeEnum.withName(ActionTypeEnum.host.toString)
      case "actionsProject" => ActionTypeEnum.withName(ActionTypeEnum.project.toString)
      case _ => ActionTypeEnum.withName(ActionTypeEnum.host.toString)
    }
    val actions = template.get(item)
    if (actions != null) {
      actions.asInstanceOf[JList[JMap[String, AnyRef]]].asScala.zipWithIndex.foreach {
        case (action, index) =>
          val taskId = TemplateActionHelper.create(TemplateAction(None, action.get("name").asInstanceOf[String],
            action.get("css").asInstanceOf[String], action.get("versionMenu").asInstanceOf[Boolean],
            templateId, index + 1, tagName,
            actionType
          ))

          val steps = action.get("steps").asInstanceOf[JList[JMap[String, String]]].asScala
          steps.zipWithIndex.foreach { case (step, index) =>
            val seconds = step.get("seconds").asInstanceOf[Int]
            TemplateActionStepHelper.create(TemplateActionStep(None, taskId, step.get("name"), step.get("sls"), if (seconds <= 0) 3 else seconds, index + 1, Option(step.get("md5Check"))))
          }
      }
    }
  }


}