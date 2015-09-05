package dto

import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._

case class FacebookProfile(id: String, email: String, first_name: String, last_name: String, gender: String, link: String, locale: String, name: String)

object FacebookProfileBuilder {

  implicit val facebookProfileReads: Reads[FacebookProfile] = (
    (JsPath \ "id").read[String] and
      (JsPath \ "email").read[String] and
      (JsPath \ "first_name").read[String] and
      (JsPath \ "last_name").read[String] and
      (JsPath \ "gender").read[String] and
      (JsPath \ "link").read[String] and
      (JsPath \ "locale").read[String] and
      (JsPath \ "name").read[String]
    )(FacebookProfile.apply _)
}