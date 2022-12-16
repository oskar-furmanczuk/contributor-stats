import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Flow
import client.Authentication.GitHubAuthenticator
import server.RouteWithCaching

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}

object Main extends App {

  val githubToken: Option[String] = args.headOption

  implicit val system: ActorSystem = ActorSystem("task-actor-sys")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContextExecutor = ExecutionContext.global
  implicit val connection: Flow[HttpRequest, HttpResponse, Future[Http.OutgoingConnection]] =
    Http().outgoingConnectionHttps(host = "api.github.com")
  implicit val gitHubAuthenticator: GitHubAuthenticator = new GitHubAuthenticator(githubToken)

  Http().newServerAt("localhost", 8080)
    .bind(RouteWithCaching.getRoute())

}
