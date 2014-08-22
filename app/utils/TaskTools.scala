package utils

import java.io.StringWriter
import javax.script.{ScriptException, ScriptEngineManager}

import models.conf._
import play.api.libs.json.Json
import play.api.{Logger, Play}
import scala.collection.mutable

/**
 * Created by jinwei on 1/7/14.
 */
object TaskTools {

  implicit val hostFormat = Json.format[Host_v]
  implicit val envFormat = Json.format[Environment_v]
  implicit val projectFormat = Json.format[Project_v]
  implicit val versionFormat = Json.format[Version_v]
  implicit val taskFormat = Json.format[Task_v]

  /**
   * 判断版本是否是snapshot
   * @param versionId
   * @return
   */
  def isSnapshot(versionId: Int): Boolean = {
    var result = true
    VersionHelper.findById(versionId) match {
      case Some(version) => {
        result = isSnapshot(version.vs)
      }
      case _ => {
        result = false
      }
    }
    Logger.info("isSnapshot ==>" + result.toString)
    result
  }

  def isSnapshot(version: String): Boolean = {
    if (version.endsWith("-SNAPSHOT")) {
      true
    } else {
      false
    }
  }

  /**
   * 去除字符串两边的引号
   * @param s
   * @return
   */
  def trimQuotes(s: String): String = {
    s.trim.stripPrefix("\"").stripSuffix("\"").trim
  }

  def findTemplateItem(envId: Int, projectId: Int): mutable.Map[String, String] = {
    //3、根据envId -> script_version, projectId -> template_id
    //4、根据script_version,template_id -> template_item
    val latestVersion = getLatestVersion(envId)
    val templateId = ProjectHelper.findById(projectId).get.templateId
    val itemMap = mutable.Map.empty[String, String]
    TemplateItemHelper.findByTemplateId_ScriptVersion(templateId, latestVersion).foreach { t => itemMap.put(t.itemName, t.itemName)}
    itemMap
  }

  def getLatestVersion(envId: Int): String = {
    val environment = EnvironmentHelper.findById(envId).get
    // 如果是latest，则获取最后的一个可用版本
    val latestVersion = environment.scriptVersion match {
      case ScriptVersionHelper.Latest => ScriptVersionHelper.findLatest().get
      case x => x
    }
    latestVersion
  }

  def propertyRule(str: String) = {
    str.contains("\\.t\\.")
    str.startsWith("")
  }

  /**
   * 项目依赖
   * @param pid
   * @return
   */
  def findDependencies(pid: Int): Seq[Int] = {
    ProjectDependencyHelper.findByProjectId(pid).map(_.dependencyId).filter(_ != -1)
  }

  def getFileName() = {
    val timestamp: Long = System.currentTimeMillis / 1000
    s"${timestamp}"
  }

  /**
   * 根据环境关联的版本号
   * @param envId
   * @param projectId
   * @return
   */
  def getProperties(envId: Int, projectId: Int, templateId: Int, realVersion: String): Map[String, String] = {
    //1、根据projectId获取attribute
    var tempAttrs = TemplateItemHelper.findByTemplateId_ScriptVersion(templateId, realVersion).map(_.itemName)
    val attrMap = AttributeHelper.findByProjectId(projectId).filter(a => tempAttrs.contains(a.name)).map { a => a.name -> a.value.get}.toMap

    //2、根据envId + projectId 获取variable
    val varMap = VariableHelper.findByEnvId_ProjectId(envId, projectId).filter(v => !v.name.startsWith("t_") || tempAttrs.contains(v.name)).map { v => v.name -> v.value}.toMap

    //5、attribute + variable
    attrMap ++ varMap
  }

  def findCluster(envId: Int, projectId: Int): Seq[EnvironmentProjectRel] = {
    EnvironmentProjectRelHelper.findByEnvId_ProjectId(envId, projectId)
  }

  def findProject(envId: Int, projectId: Int, realVersion: String): Project_v = {
    val project = ProjectHelper.findById(projectId).get

    val hosts = findCluster(envId, projectId).map {
      c =>
        Host_v(c.name, c.ip, Option(getProperties(envId, projectId, project.templateId, realVersion)))
    }.toArray

    Project_v(s"$projectId", s"${project.templateId}", project.name, hosts, None)
  }

  def findDependencies_v(envId: Int, projectId: Int, realVersion: String): Map[String, Project_v] = {
    findDependencies(projectId).map {
      pid =>
        val project = ProjectHelper.findById(pid).get
        val hosts = findCluster(envId, project.id.get).map {
          c =>
            Host_v(c.name, c.ip, None)
        }.toArray
        val attrs = getProperties(envId, project.id.get, project.templateId, realVersion).filter { t => t._1.startsWith("t_")}
        project.name -> Project_v(s"$projectId", s"${project.templateId}", project.name, hosts, Option(attrs))
    }.toMap
  }

  def findEnvironment_v(envId: Int): Environment_v = {
    val env = EnvironmentHelper.findById(envId).get
    val realVersion = ScriptVersionHelper.findRealVersion(env.scriptVersion)
    // 如果是master，需要替换成base，在gitfs中，是需要这么映射的
    val scriptVersion = realVersion match {
      case ScriptVersionHelper.Master => "base"
      case x => x
    }
    Environment_v(s"$envId", env.name, env.nfServer.get, scriptVersion, realVersion)
  }

  def findAlias(templateId: Int, scriptVersion: String): Map[String, String] = {
    TemplateAliasHelper.findByTemplateId_Version(templateId, scriptVersion).map { x => x.name -> x.value}.toMap
  }

  /**
   * 生成task meta
   * @param taskId
   * @param envId
   * @param projectId
   * @param versionId
   * @return
   */
  def generateTaskObject(taskId: Int, envId: Int, projectId: Int, versionId: Option[Int]): Task_v = {
    val version: Option[Version_v] = versionId match {
      case Some(id) =>
        VersionHelper.findById(id).map { vs =>
          Version_v(vs.id.get.toString, vs.vs)
        }
      case None => None
    }

    val env = findEnvironment_v(envId)

    val repository = version match {
      case Some(v) => {
        if (isSnapshot(v.name)) {
          Option("snapshots")
        } else {
          Option("releases")
        }
      }
      case _ => None
    }
    val project = findProject(envId, projectId, env.realVersion)
    val alias = findAlias(project.templateId.toInt, env.realVersion)
    val d = findDependencies_v(envId, projectId, env.realVersion)
    Task_v(s"$taskId", version, repository, s"${getFileName()}", None, alias, env, project, d)
  }

  def generateCurrent(machine: String, task: Task_v): Host_v = {
    task.project.hosts.filter { t => t.name == machine}(0)
  }

  def generateCurrent(num: Int, task: Task_v): Host_v = {
    task.project.hosts(num)
  }

  def generateCodeCompleter(envId: Int, projectId: Int, versionId: Int) = {
    val task = generateTaskObject(0, envId, projectId, Some(versionId))
    if (task.project.hosts nonEmpty) {
      val engine = new ScriptEngineManager().getEngineByName("js")
      val w = new StringWriter
      engine.getContext.setWriter(w)

      engine.eval(s"var __t__ = ${Json.toJson(task).toString}")
      println(engine.eval("JSON.stringify(__t__)"))
      engine.eval("for (__attr in __t__) {this[__attr] = __t__[__attr];}")
      engine.eval("var alias = {};")
      try {
        task.alias.foreach { case (key, value) => engine.eval(s"alias.${key} = ${value};")}
      } catch {
        case e: ScriptException => Logger.error(e.toString)
      }
      engine.eval("var current = project.hosts[0];")

      try {
        engine.eval(
          """
            | function pushMap(obj, prefix, m) {
            |   for (__o in obj) {
            |     if (typeof obj[__o] === 'object'
            |      && (__o != 'JavaAdapter' && __o != 'context' && __o.substring(0, 2) != '__')) {
            |       if (Array.isArray(obj[__o])) {
            |         m[prefix + __o] = 'array';
            |         var __arr = obj[__o];
            |         var __p = prefix + __o;
            |         for (__a in __arr) {
            |           pushMap(__arr[__a], __p + '[' + __a + '].', m);
            |         }
            |       } else {
            |         m[prefix + __o] = 'object';
            |         pushMap(obj[__o], prefix + __o + '.', m);
            |       }
            |     }
            |     if (typeof obj[__o] === 'string' && __o.substring(0, 2) != '__') {
            |       m[prefix + __o] = obj[__o];
            |     }
            |   }
            | }
            |
            | var __m__ = {}
            |
            | pushMap(this, "", __m__)
            |
            | println(JSON.stringify(__m__))
          """.stripMargin)
      } catch {
        case exception: ScriptException => {
          Logger.error(exception.toString)
        }
      }
      w.toString
    } else {
      "{'没有关联机器!':'error'}"
    }
  }

}

object ConfHelp {
  val app = Play.current

  lazy val logPath = app.configuration.getString("salt.log.dir").getOrElse("target/saltlogs")

  lazy val confPath: String = app.configuration.getString("salt.file.pkgs").getOrElse("target/pkgs")
}

case class Host_v(name: String, ip: String, attrs: Option[Map[String, String]])

case class Environment_v(id: String, name: String, nfsServer: String, scriptVersion: String, realVersion: String)

case class Project_v(id: String, templateId: String, name: String, hosts: Seq[Host_v], attrs: Option[Map[String, String]])

case class Version_v(id: String, name: String)

case class Task_v(taskId: String, version: Option[Version_v], repository: Option[String], confFileName: String, current: Option[Host_v], alias: Map[String, String], env: Environment_v, project: Project_v, dependence: Map[String, Project_v])


