package utils

import models.conf._
import play.api.{Logger, Play}
import scala.collection.mutable

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
    if(version.endsWith("-SNAPSHOT")){
      true
    }else{
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


  def getPropertiesByEnv_Project(projectName: String, envId: Int): Map[String, String] ={
    ProjectHelper.findByName(projectName) match{
      case Some(project) =>
        getProperties(envId, project.id.get)
      case _ => Map.empty[String, String]
    }
  }

//  def replaceRecursion(envId: Int, projectId: Int, map: mutable.Map[String, String]): mutable.Map[String, String] = {
//    var flag = false
//    var map = TaskTools.getProperties(envId, projectId)
//    map.foreach{
//      s =>
//        if(s._2.startsWith("\\{\\{")){
//          flag = true
//          val tmp = s._2.replaceAll("\\{\\{", "").replaceAll("\\}\\}", "").split("\\.t\\.")
//          TaskTools.getPropertiesByEnv_Project(tmp(0), envId).get(s"t.${tmp(1)}") match {
//            case Some(value) => map.put(s._1, value)
//            case _ => map -= s._1
//          }
//        }
//    }
//    if(flag){
//      map = replaceRecursion(envId, projectId, map)
//    }
//    map
//  }

  def findTemplateItem(envId: Int, projectId: Int): mutable.Map[String, String] = {
    //3、根据envId -> script_version, projectId -> template_id
    //4、根据script_version,template_id -> template_item
    val latestVersion = getLatestVersion(envId)
    val templateId = ProjectHelper.findById(projectId).get.templateId
    val itemMap = mutable.Map.empty[String, String]
    TemplateItemHelper.findByTemplateId_ScriptVersion(templateId, latestVersion).foreach{ t => itemMap.put(t.itemName, t.itemName)}
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

  def propertyRule(str: String)= {
    str.contains("\\.t\\.")
    str.startsWith("")
  }

  /**
   * 项目依赖
   * @param pid
   * @return
   */
  def findDependencies(pid: Int): Seq[Int]= {
    ProjectDependencyHelper.findByProjectId(pid).map(_.dependencyId)
  }

  /**
   * 根据环境关联的版本号
   * @param envId
   * @param projectId
   * @return
   */
  def getProperties(envId: Int, projectId: Int): Map[String, String] = {
    //1、根据projectId获取attribute
    val attrMap = mutable.Map.empty[String, String]
    AttributeHelper.findByProjectId(projectId).map{ a => attrMap.put(a.name, a.value.get) }
    //2、根据envId + projectId 获取variable
    val varMap = mutable.Map.empty[String, String]
    VariableHelper.findByEnvId_ProjectId(envId, projectId).map{ v => varMap.put(v.name, v.value) }
    //5、attribute + variable
    attrMap.foreach{
      t =>
        if(!varMap.contains(t._1)){
          varMap.put(t._1, t._2)
        }
    }
    varMap.toMap
  }

  def findCluster(envId: Int, projectId: Int): Seq[EnvironmentProjectRel]= {
    EnvironmentProjectRelHelper.findByEnvId_ProjectId(envId, projectId)
  }

  def findSelfProject(envId: Int, projectId: Int, version: String): SelfProject_v= {
    val hosts = findCluster(envId, projectId).map{
      c =>
        Machine_v(c.name, c.ip, getProperties(envId, projectId))
    }.toArray
    val project = ProjectHelper.findById(projectId).get
    val repository = if(isSnapshot(version)) {
      "snapshots"
    }else {
      "releases"
    }
    SelfProject_v(projectId, project.name, version, repository, hosts)
  }

  def findDependencies_v(envId: Int, projectId: Int): Map[String, DependencyProject_v]= {
    findDependencies(projectId).map{
      pid =>
        val project = ProjectHelper.findById(pid).get
        val hosts = findCluster(envId, project.id.get).map{
          c =>
            Machine_v(c.name, c.ip, Map.empty)
        }.toArray
        val attrs = getProperties(envId, project.id.get).filter{t => t._1.startsWith("t_")}
        project.name -> DependencyProject_v(hosts, attrs)
    }.toMap
  }

  def findEnvironment_v(envId: Int): Environment_v = {
    val env = EnvironmentHelper.findById(envId).get
    Environment_v(envId, env.name, env.nfServer.get, env.scriptVersion)
  }

  /**
   * 生成task meta
   * @param taskId
   * @param envId
   * @param projectId
   * @param version
   * @return
   */
  def generateTaskObject(taskId: Int, envId: Int, projectId: Int, version: String): Task_v= {
    val env = findEnvironment_v(envId)
    val s = findSelfProject(envId, projectId, version)
    val d = findDependencies_v(envId, projectId)
    Task_v(taskId, s"${taskId}", None, Map.empty[String, String], env, s, d)
  }

  def generateCurrent(machine: String, task: Task_v): Machine_v= {
    task.s.hosts.filter{t => t.hostName == machine}(0)
  }

  def generateCurrent(num: Int, task: Task_v): Machine_v= {
    task.s.hosts(num)
  }

}

object ConfHelp {
  val app = Play.current

  lazy val logPath = app.configuration.getString("salt.log.dir").getOrElse("target/saltlogs")

  lazy val confPath: String = app.configuration.getString("salt.file.pkgs").getOrElse("target/pkgs")
}

case class Machine_v(hostName: String, ip: String, attrs: Map[String, String])
case class SelfProject_v(id: Int, name: String, version: String, repository: String, hosts: Array[Machine_v])
case class Environment_v(id: Int, name: String, nfsServer: String, scriptVersion: String)
case class DependencyProject_v(hosts: Array[Machine_v], attrs: Map[String, String])
case class Task_v(taskId: Int, confFileName: String, c: Option[Machine_v], a: Map[String, String], env: Environment_v, s: SelfProject_v, d: Map[String, DependencyProject_v])



