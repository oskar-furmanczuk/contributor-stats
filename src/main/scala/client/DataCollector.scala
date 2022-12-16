package client

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model.headers.Link
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.{Unmarshal, Unmarshaller}
import akka.stream.scaladsl.{Flow, Sink, Source}
import client.Authentication.GitHubAuthenticator
import spray.json.DeserializationException

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.language.postfixOps
import scala.collection.immutable.Seq
import client.DomainDefinitions.GithubObject

object DataCollector {
  def collectData[T <: GithubObject](uri: Uri )(implicit actorSystem: ActorSystem,
                                                 unmarshaller: Unmarshaller[ResponseEntity, List[T]],
                                                 connection: Flow[HttpRequest, HttpResponse, Future[Http.OutgoingConnection]],
                                                 ec: ExecutionContext,
                                                 gitHubAuthenticator: GitHubAuthenticator)
  : Future[List[T]] = {

    extractData(getUris(uri))
  }

  private def extractData[T <: GithubObject](uris: List[Uri])(implicit actorSystem: ActorSystem,
                                                              unmarshaller: Unmarshaller[ResponseEntity, List[T]],
                                                              connection: Flow[HttpRequest, HttpResponse, Future[Http.OutgoingConnection]],
                                                              ec: ExecutionContext,
                                                              gitHubAuthenticator: GitHubAuthenticator)
  : Future[List[T]] = {
    val headers = gitHubAuthenticator.getAuthenticationHeaders.getOrElse(Seq.empty[HttpHeader])
    val requests = uris.map(u => HttpRequest(uri = u, method = HttpMethods.GET, headers = headers))

    Source(requests)
      .via(connection)
      .map(resp => {
        Unmarshal(resp.entity).to[List[T]].recoverWith {
          case _: DeserializationException =>
            resp.entity.discardBytes()
            Future.successful(List[T]())
        }
      }).async
      .runWith(Sink.seq)
      .map(seq => Future.sequence(seq))
      .flatten
      .map(_.flatten.toList)
  }

  private def getUris(uriWithoutParams: Uri, perPage: Int = 10)(implicit actorSystem: ActorSystem,
                                                                ec: ExecutionContext,
                                                                gitHubAuthenticator: GitHubAuthenticator)
  : List[Uri] = {
    val numOfPages: Int = Await.result(
      getNumOfPages(uriWithoutParams.withQuery(Query(("per_page", s"$perPage")))), 10 second)
      .getOrElse(1)

    List.tabulate(numOfPages + 1)(nPage =>
      uriWithoutParams.withQuery(Query(("page", s"$nPage"), ("per_page", s"$perPage")))).drop(1)
  }

  private def getNumOfPages(uri: Uri)(implicit actorSystem: ActorSystem,
                                      ec: ExecutionContext,
                                      gitHubAuthenticator: GitHubAuthenticator)
  :  Future[Option[Int]] = {

    val response = Http().singleRequest(HttpRequest(uri = uri,
      headers = gitHubAuthenticator.getAuthenticationHeaders.getOrElse(Seq.empty[HttpHeader])))
    response.map(resp => {
      resp.entity.discardBytes()
      resp.header[Link]}
      .filter({ _.toString.contains("last")
      })
      .map(_.values.last.getUri().toString.split("=").last.toInt))
  }

}
