package utils

import java.net.URLConnection
import ControlUtil._

object FileUtil {

  def getMimeType(name: String): String =
    defining(URLConnection.getFileNameMap()){ fileNameMap =>
      fileNameMap.getContentTypeFor(name) match {
        case null     => "application/octet-stream"
        case mimeType => mimeType
      }
    }

  def getContentType(name: String, bytes: Array[Byte]): String = {
    defining(getMimeType(name)){ mimeType =>
      if(mimeType == "application/octet-stream" && isText(bytes)){
        "text/plain"
      } else {
        mimeType
      }
    }
  }

  def isImage(name: String): Boolean = getMimeType(name).startsWith("image/")

  def isLarge(size: Long): Boolean = (size > 1024 * 1000)

  def isText(content: Array[Byte]): Boolean = !content.contains(0)

  def getExtension(name: String): String =
    name.lastIndexOf('.') match {
      case i if(i >= 0) => name.substring(i + 1)
      case _ => ""
    }

}
