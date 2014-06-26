package utils

import play.api.libs.json._
import java.util.Date
import org.joda.time._
import java.text.SimpleDateFormat

/**
 * date to json
 * @author of729
 */
object DateFormatter {

  implicit object JsonDateFormatter extends Format[DateTime] {

    val dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss")

    def writes(date: DateTime): JsValue = {
      Json.toJson(
         dateFormat.format(date.toDate)
      )
    }


    def reads(j: JsValue): JsResult[DateTime] = {
        JsSuccess(DateTime.parse(j.as[String]))
        //        j.asOpt[Date].map(date => Some(dateFormat.parse(date))).getOrElse(NotAssigned)
    }

  }
}
