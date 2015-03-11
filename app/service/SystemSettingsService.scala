package service

import java.io.FileInputStream
import java.util.Properties

import play.api.Play.current
import play.api.cache.Cache
import service.SystemSettingsService._
import utils.ControlUtil._
import utils.Directory._

/**
 * of546
 */
trait SystemSettingsService {

  val _cacheKey = "systemSettings"

  def saveSystemSettings(settings: SystemSettings): Unit = {
    defining(new Properties()) { props =>
      props.setProperty(LdapAuthentication, settings.ldapAuthentication.toString)
      if (settings.ldapAuthentication) {
        settings.ldap.map { ldap =>
          props.setProperty(LdapHost, ldap.host)
          ldap.port.foreach(x => props.setProperty(LdapPort, x.toString))
          ldap.bindDN.foreach(x => props.setProperty(LdapBindDN, x))
          ldap.bindPassword.foreach(x => props.setProperty(LdapBindPassword, x))
          props.setProperty(LdapBaseDN, ldap.baseDN)
          props.setProperty(LdapUserNameAttribute, ldap.userNameAttribute)
          ldap.additionalFilterCondition.foreach(x => props.setProperty(LdapAdditionalFilterCondition, x))
          ldap.fullNameAttribute.foreach(x => props.setProperty(LdapFullNameAttribute, x))
          ldap.mailAttribute.foreach(x => props.setProperty(LdapMailAddressAttribute, x.toString))
          ldap.tls.foreach(x => props.setProperty(LdapTls, x.toString))
          ldap.keystore.foreach(x => props.setProperty(LdapKeystore, x))
        }
      }
      using(new java.io.FileOutputStream(BugattiConf)){ out =>
        props.store(out, null)
      }
    }
    Cache.remove(_cacheKey)
  }

  def loadSystemSettings(): SystemSettings = {
    defining(new Properties()) { props =>
      if (BugattiConf.exists) {
        using(new FileInputStream(BugattiConf)) { in =>
          props.load(in)
        }
      }
      val systemSettings = SystemSettings(
        getValue(props, LdapAuthentication, false),
        if (getValue(props, LdapAuthentication, false)) {
          Some(Ldap(
            getValue(props, LdapHost, ""),
            getOptionValue(props, LdapPort, Some(DefaultLdapPort)),
            getOptionValue(props, LdapBindDN, None),
            getOptionValue(props, LdapBindPassword, None),
            getValue(props, LdapBaseDN, ""),
            getValue(props, LdapUserNameAttribute, ""),
            getOptionValue(props, LdapAdditionalFilterCondition, None),
            getOptionValue(props, LdapFullNameAttribute, None),
            getOptionValue(props, LdapMailAddressAttribute, None),
            getOptionValue[Boolean](props, LdapTls, None),
            getOptionValue(props, LdapKeystore, None)))
        } else {
          None
        }
      )
      Cache.getAs[SystemSettings](_cacheKey) match {
        case Some(_systemSettings) => _systemSettings
        case None =>
          Cache.set(_cacheKey, systemSettings)
          systemSettings
      }
    }
  }

}

object SystemSettingsService {
  import scala.reflect.ClassTag

  case class SystemSettings(
                             ldapAuthentication: Boolean,
                             ldap: Option[Ldap])

  case class Ldap(
                   host: String,
                   port: Option[Int],
                   bindDN: Option[String],
                   bindPassword: Option[String],
                   baseDN: String,
                   userNameAttribute: String,
                   additionalFilterCondition: Option[String],
                   fullNameAttribute: Option[String],
                   mailAttribute: Option[String],
                   tls: Option[Boolean],
                   keystore: Option[String])

  val DefaultLdapPort = 389

  private val LdapAuthentication = "ldap_authentication"
  private val LdapHost = "ldap.host"
  private val LdapPort = "ldap.port"
  private val LdapBindDN = "ldap.bindDN"
  private val LdapBindPassword = "ldap.bind_password"
  private val LdapBaseDN = "ldap.baseDN"
  private val LdapUserNameAttribute = "ldap.username_attribute"
  private val LdapAdditionalFilterCondition = "ldap.additional_filter_condition"
  private val LdapFullNameAttribute = "ldap.fullname_attribute"
  private val LdapMailAddressAttribute = "ldap.mail_attribute"
  private val LdapTls = "ldap.tls"
  private val LdapKeystore = "ldap.keystore"

  private def getValue[A: ClassTag](props: java.util.Properties, key: String, default: A): A =
    defining(props.getProperty(key)){ value =>
      if(value == null || value.isEmpty) default
      else convertType(value).asInstanceOf[A]
    }

  private def getOptionValue[A: ClassTag](props: java.util.Properties, key: String, default: Option[A]): Option[A] =
    defining(props.getProperty(key)){ value =>
      if(value == null || value.isEmpty) default
      else Some(convertType(value)).asInstanceOf[Option[A]]
    }

  private def convertType[A: ClassTag](value: String) =
    defining(implicitly[ClassTag[A]].runtimeClass){ c =>
      if(c == classOf[Boolean])  value.toBoolean
      else if(c == classOf[Int]) value.toInt
      else value
    }

}