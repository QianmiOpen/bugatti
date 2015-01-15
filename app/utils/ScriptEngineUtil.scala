package utils

import javax.script.{ScriptEngineManager, ScriptException}

import play.api.Logger
import play.api.libs.json.Json
import TaskTools._

/**
 * Created by mind on 8/23/14.
 */
class ScriptEngineUtil(projectTask: ProjectTask_v, hostname: Option[String]) {
  val engine = new ScriptEngineManager(null).getEngineByName("js")

  engine.eval(s"var __t__ = ${Json.toJson(projectTask).toString}")

  Logger.info(s"${engine.eval("JSON.stringify(__t__)")}")
  engine.eval("for (__attr in __t__) {this[__attr] = __t__[__attr];}")
  engine.eval("var alias = {};")
  try {
    projectTask.dependence.foreach {
      case (projectName, project_v) =>
        Logger.debug(s"execute dep alias: $projectName")
        project_v.alias.foreach {
          case (key, value) =>
            Logger.debug(s"execute dep alias: $projectName, $key, $value")
            engine.eval(s"dependence.$projectName.alias.$key = function (project){return $value}.call(dependence.$projectName, this)")
        }
    }

    projectTask.alias.foreach {
      case (key, value) =>
        Logger.debug(s"$key,$value")
        engine.eval(s"alias.$key = function (project){return $value}.call(this, this)")
    }
  } catch {
    case e: ScriptException => Logger.error(e.toString)
  }

  if (projectTask.hosts.length > 0) {
    hostname match {
      case Some(name) => {
        engine.eval(
          s"""
            | for (__i = 0; __i < hosts.length; __i++) {
            |   if (hosts[__i].name === "$name") {
            |     var cHost = hosts[__i];
            |     for (__attr in cHost.attrs) {
            |       attrs[__attr] = cHost.attrs[__attr]
            |     }
            |     break;
            |   }
            | }
          """.stripMargin)
        engine.eval(s"var confFileName = '${projectTask.confFileName}_$name'")
      }
      case None => {
        engine.eval("var cHost = hosts[0];")
      }
    }
  }

  def setCHost: ProjectTask_v= {
    if(projectTask.hosts.length > 0){
      hostname match {
        case Some(name) => {
          projectTask.copy(cHost = projectTask.hosts.filter(_.name == name).headOption)
        }
        case _ => {
          projectTask
        }
      }
    }else {
      projectTask
    }
  }


  def eval(script: String): (Boolean, String) = {
    try {
      val ret = engine.eval(script)
      if (ret == null || ret.toString.length == 0) {
        (false, s"$script is null.")
      } else {
        (true, ret.toString)
      }
    } catch {
      case e: ScriptException => {
        (false, e.toString)
      }
    }
  }

  def getAttrs(): (Boolean, String) = {
    try {
      (true, engine.eval(
        """
          | function pushMap(obj, prefix, m) {
          |   for (__o in obj) {
          |     if (typeof obj[__o] === 'object'
          |       && (__o != 'JavaAdapter' && __o != 'context' && __o.substring(0, 2) != '__')) {
          |       if (Array.isArray(obj[__o])) {
          |         m[prefix + __o] = 'array';
          |         var __arr = obj[__o];
          |         var __p = prefix + __o;
          |         for (__a in __arr) {
          |           if (typeof __arr[__a] === 'object') {
          |             pushMap(__arr[__a], __p + '[' + __a + '].', m);
          |           } else {
          |             m[__p + '[' + __a + ']'] = __arr[__a];
          |           }
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
          | JSON.stringify(__m__)
        """.stripMargin).toString)
    } catch {
      case e: ScriptException => {
        (false, e.toString)
      }
    }
  }
}
