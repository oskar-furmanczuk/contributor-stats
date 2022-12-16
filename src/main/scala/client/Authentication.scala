package client

import akka.http.scaladsl.model.HttpHeader
import akka.http.scaladsl.model.headers.{Authorization, OAuth2BearerToken}
import scala.collection.immutable.Seq


object Authentication {

  class GitHubAuthenticator(githubToken: Option[String]){
    def getAuthenticationHeaders: Option[Seq[HttpHeader]] = {
      githubToken.map(token => Seq(Authorization(OAuth2BearerToken(token))))
    }
  }
}
