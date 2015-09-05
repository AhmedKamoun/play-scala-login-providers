package util

import dto.{FacebookProfile, FacebookProfileBuilder}
import play.api.{Application, Play}
import play.api.http.{HeaderNames, MimeTypes}
import play.api.libs.json.{JsError, JsSuccess}
import play.api.libs.ws.WS
import play.api.mvc.{Action, Controller, Request, Results}
import FacebookProfileBuilder.facebookProfileReads

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class OAuth2Facebook(application: Application) {
  lazy val facebookAuthId = application.configuration.getString("facebook.client.id").get
  lazy val facebookAuthSecret = application.configuration.getString("facebook.client.secret").get

  def getAuthorizationUrl(redirectUri: String, scope: String, state: String): String = {
    val baseUrl = application.configuration.getString("facebook.redirect.url").get
    baseUrl.format(facebookAuthId, redirectUri, scope, state)
  }

  def getToken(code: String)(implicit request: Request[_]): Future[String] = {
    val tokenResponse = WS.url("https://graph.facebook.com/v2.3/oauth/access_token")(application).
      withQueryString("client_id" -> facebookAuthId,
        "client_secret" -> facebookAuthSecret,
        "code" -> code,
        "redirect_uri" -> util.routes.OAuth2Facebook.callback(None, None).absoluteURL()).
      withHeaders(HeaderNames.ACCEPT -> MimeTypes.JSON).
      post(Results.EmptyContent())

    tokenResponse.flatMap { response =>
      (response.json \ "access_token").asOpt[String].fold(Future.failed[String](new IllegalStateException("Sod off!"))) { accessToken =>
        Future.successful(accessToken)
      }
    }
  }
}

object OAuth2Facebook extends Controller {
  lazy val oauth2 = new OAuth2Facebook(Play.current)

  def callback(codeOpt: Option[String] = None, stateOpt: Option[String] = None) = Action.async { implicit request =>
    (for {
      code <- codeOpt
      state <- stateOpt
      oauthState <- request.session.get("oauth-state")
    } yield {
        if (state == oauthState) {
          oauth2.getToken(code).map { accessToken =>
            Redirect(util.routes.OAuth2Facebook.success()).withSession("oauth-token" -> accessToken)
          }.recover {
            case ex: IllegalStateException => Unauthorized(ex.getMessage)
          }
        }
        else {
          Future.successful(BadRequest("Invalid facebook login"))
        }
      }).getOrElse(Future.successful(BadRequest("No parameters supplied")))
  }

  def success() = Action.async { request =>
    implicit val app = Play.current
    request.session.get("oauth-token").fold(Future.successful(Unauthorized("oauth-token not found"))) { authToken =>
      WS.url("https://graph.facebook.com/v2.3/me").
        withQueryString("access_token" -> authToken).
        get().map { response =>

        response.json.validate[FacebookProfile] match {
          case s: JsSuccess[FacebookProfile] => {
            val profile: FacebookProfile = s.get
            // do something with place
            Ok("my name is " + profile.name)
          }
          case e: JsError => {
            // error handling flow
            BadRequest("json read error")
          }
        }

        // Future.successful(Ok(authToken))
      }
    }
  }
}