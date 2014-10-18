package utils

import models.conf._
import play.api.Play
import play.api.libs.json.Json

/**
 * Created by jinwei on 1/7/14.
 */
object TaskTools {

  implicit val hostFormat = Json.format[Host_v]
  implicit val envFormat = Json.format[Environment_v]
  implicit val projectFormat = Json.format[Project_v]
  implicit val versionFormat = Json.format[Version_v]
  implicit val taskFormat = Json.format[ProjectTask_v]

  /**
   * 去除字符串两边的引号
   * @param s
   * @return
   */
  def trimQuotes(s: String): String = {
    s.trim.stripPrefix("\"").stripSuffix("\"").trim
  }

  /**
   * 项目依赖
   * @param pid
   * @return
   */
  def findDependencies(pid: Int): Map[Int, Option[String]] = {
    ProjectDependencyHelper.findByProjectId(pid).map( t => t.dependencyId -> t.alias).toMap
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
    //根据projectId获取attribute
    val tempAttrs = TemplateItemHelper.findByTemplateId_ScriptVersion(templateId, realVersion).map(_.itemName)
    val attrMap = AttributeHelper.findByProjectId(projectId).filter(a => tempAttrs.contains(a.name)).map { a => a.name -> a.value.getOrElse("")}.toMap

    //根据envId + projectId 获取variable
    val varMap = VariableHelper.findByEnvId_ProjectId(envId, projectId).filter(v => !v.name.startsWith("t_") || tempAttrs.contains(v.name)).map { v => v.name -> v.value}.toMap

    //attribute + variable
    attrMap ++ varMap
  }

  def findHosts(envId: Int, projectId: Int): Seq[EnvironmentProjectRel] = {
    EnvironmentProjectRelHelper.findByEnvId_ProjectId(envId, projectId)
  }

  def findProject(envId: Int, projectId: Int, realVersion: String): Project_v = {
    val project = ProjectHelper.findById(projectId).get

    val hosts = findHosts(envId, projectId).map(c => Host_v(c.name, c.ip, Some(c.globalVariable.map(v => v.name -> v.value).toMap)))

    val attrs = getProperties(envId, projectId, project.templateId, realVersion)
    Project_v(s"$projectId", s"${project.templateId}", project.name, hosts, Some(attrs))
  }

  def findDependencies_v(envId: Int, projectId: Int, realVersion: String): Map[String, Project_v] = {
    val map = findDependencies(projectId)
    map.keySet.map {
      pid =>
        val project = ProjectHelper.findById(pid).get
        val hosts = findHosts(envId, project.id.get).map(c => Host_v(c.name, c.ip, None))
//        val attrs = getProperties(envId, project.id.get, project.templateId, realVersion).filter { t => t._1.startsWith("t_")}
        val attrs = getProperties(envId, project.id.get, project.templateId, realVersion)
        map.get(pid).get match {
          case Some(alias) =>
            alias -> Project_v(s"$projectId", s"${project.templateId}", alias, hosts, Option(attrs))
          case _ =>
            project.name -> Project_v(s"$projectId", s"${project.templateId}", project.name, hosts, Option(attrs))
        }
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

    Environment_v(s"$envId", env.name, scriptVersion, realVersion)
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
  def generateTaskObject(taskId: Int, envId: Int, projectId: Int, versionId: Option[Int]): ProjectTask_v = {
    val version: Option[Version_v] = versionId match {
      case Some(id) =>
        VersionHelper.findById(id).map { vs =>
          Version_v(vs.id.get.toString, vs.vs)
        }
      case None => None
    }

    val env = findEnvironment_v(envId)

    val project = findProject(envId, projectId, env.realVersion)
    val alias = findAlias(project.templateId.toInt, env.realVersion)
    val d = findDependencies_v(envId, projectId, env.realVersion)
    new ProjectTask_v(project, alias, d, env, s"$taskId", version, s"${getFileName()}", None, ConfHelp.system)
  }

  def generateCurrent(machine: String, task: ProjectTask_v): Host_v = {
    task.hosts.filter { t => t.name == machine}(0)
  }

  def generateCurrent(num: Int, task: ProjectTask_v): Host_v = {
    task.hosts(num)
  }

  def generateCodeCompleter(envId: Int, projectId: Int, versionId: Int) = {
    val task = generateTaskObject(0, envId, projectId, Some(versionId))
    if (task.hosts nonEmpty) {
      val (ret, result) = new ScriptEngineUtil(task, None).getAttrs()
      if (ret) {
        result
      } else {
        s"""{"$result", "error"}"""
      }
    } else {
      s"""{"没有关联机器!":"error"}"""
    }
  }
}


object ConfHelp {
  val app = Play.current

  lazy val logPath = app.configuration.getString("salt.log.dir").getOrElse("target/saltlogs")

  lazy val confPath: String = app.configuration.getString("salt.file.pkgs").getOrElse("target/pkgs")

  lazy val system: Map[String, String] = {
    app.configuration.keys.filter(_.startsWith("bugatti.system.")).map { key =>
      key.replace("bugatti.system.", "") -> app.configuration.getString(key).getOrElse("")
    }.toMap
  }
}

case class Host_v(name: String, ip: String, attrs: Option[Map[String, String]])

case class Environment_v(id: String, name: String, scriptVersion: String, realVersion: String)

case class Project_v(id: String, templateId: String, name: String, hosts: Seq[Host_v], attrs: Option[Map[String, String]])

case class Version_v(id: String, name: String)

case class ProjectTask_v(id: String, templateId: String, name: String, hosts: Seq[Host_v], attrs: Option[Map[String, String]],
                         alias: Map[String, String], dependence: Map[String, Project_v], env: Environment_v,
                         taskId: String, version: Option[Version_v], confFileName: String, cHost: Option[Host_v], system: Map[String, String]) {
  def this(project: Project_v, alias: Map[String, String], dependence: Map[String, Project_v], env: Environment_v,
           taskId: String, version: Option[Version_v], confFileName: String, cHost: Option[Host_v], system: Map[String, String]) =
    this(project.id, project.templateId, project.name, project.hosts, project.attrs,
      alias, dependence, env, taskId, version, confFileName, cHost, system)
}
