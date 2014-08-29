package utils

import javax.script.{ScriptEngineManager, ScriptException}

import play.api.Logger
import play.api.libs.json.Json
import TaskTools._

/**
 * Created by mind on 8/23/14.
 */
class ScriptEngineUtil(projectTask: ProjectTask_v, hostname: Option[String]) {
  val engine = new ScriptEngineManager().getEngineByName("js")

  engine.eval(s"var __t__ = ${Json.toJson(projectTask).toString}")

  Logger.debug(s"${engine.eval("JSON.stringify(__t__)")}")
  engine.eval("for (__attr in __t__) {this[__attr] = __t__[__attr];}")
  engine.eval("var alias = {};")

  try {
    projectTask.alias.foreach {
      case (key, value) => engine.eval(s"alias.$key = $value;")
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
          | JSON.stringify(__m__)
        """.stripMargin).toString)
    } catch {
      case e: ScriptException => {
        (false, e.toString)
      }
    }
  }
}
