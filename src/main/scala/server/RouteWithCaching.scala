package server

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, Uri}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.CachingDirectives.{cache, routeCache}
import akka.http.scaladsl.server.{RequestContext, Route}
import akka.stream.scaladsl.Flow
import client.Authentication.GitHubAuthenticator
import client.ContributorsStatCalc
import client.DomainDefinitions.JsonSupport._

import scala.concurrent.{ExecutionContext, Future}

object RouteWithCaching {

  def getRoute()(implicit ec: ExecutionContext,
                 system: ActorSystem,
                 connection: Flow[HttpRequest, HttpResponse, Future[Http.OutgoingConnection]],
                 gitHubAuthenticator: GitHubAuthenticator)
  : Route ={

    val myCache = routeCache[Uri]
    val keyerFunction: PartialFunction[RequestContext, Uri] = {
      case r: RequestContext => r.request.uri
    }

    get {
      path("org" / Segment / "contributors") { (orgName: String) =>
        system.log.info(s"Extracting contributors stats for organization: $orgName.")
        validate(orgName.length < 25 && orgName.length > 1, s"Not valid organization name: $orgName" ) {
          cache(myCache, keyerFunction) {
            val finalStatsCalc = new ContributorsStatCalc(orgName)
            complete(finalStatsCalc.calcFinalStats())
          }
        }
      }
    }
  }



}
