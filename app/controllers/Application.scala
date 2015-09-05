package controllers

import java.util.UUID

import play.api._
import play.api.mvc._
import util.{OAuth2Google, OAuth2Facebook, OAuth2GitHub}

object Application extends Controller {

  def index = Action { implicit request =>
    val state = UUID.randomUUID().toString // random confirmation string

    //GITHUB
    val github_oauth2 = new OAuth2GitHub(Play.current)
    val github_callback = util.routes.OAuth2GitHub.callback(None, None).absoluteURL()
    val github_scope = "repo" // github scope - request repo access
    val github_redirect = github_oauth2.getAuthorizationUrl(github_callback, github_scope, state)

    //FACEBOOK
    val facebook_oauth2 = new OAuth2Facebook(Play.current)
    val facebook_callbackUrl = util.routes.OAuth2Facebook.callback(None, None).absoluteURL()
    val facebook_scope = "email"
    val facebook_redirect = facebook_oauth2.getAuthorizationUrl(facebook_callbackUrl, facebook_scope, state)

    //GOOGLE
    val google_oauth2 = new OAuth2Google(Play.current)
    val google_callbackUrl = util.routes.OAuth2Google.callback(None, None).absoluteURL()
    val google_scope = "https://www.googleapis.com/auth/plus.login https://www.googleapis.com/auth/userinfo.email"
    val google_redirect = google_oauth2.getAuthorizationUrl(google_callbackUrl, google_scope, state)



    Ok(views.html.index("Your new application is ready.", github_redirect, facebook_redirect, google_redirect)).
      withSession("oauth-state" -> state)
  }


}