package client

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, ResponseEntity, Uri}
import akka.http.scaladsl.unmarshalling.Unmarshaller
import akka.stream.scaladsl.Flow
import client.Authentication.GitHubAuthenticator
import client.DomainDefinitions.{Author, CompleteStat, ContributorStat, Repo}

import scala.concurrent.{ExecutionContext, Future}

class ContributorsStatCalc(orgName: String) {

  def calcFinalStats()(implicit ec: ExecutionContext,
                     as: ActorSystem,
                     unmarshallerContr: Unmarshaller[ResponseEntity, List[ContributorStat]],
                     unmarshallerRepo: Unmarshaller[ResponseEntity, List[Repo]],
                     connection: Flow[HttpRequest, HttpResponse, Future[Http.OutgoingConnection]],
                     gitHubAuthenticator: GitHubAuthenticator)
  : Future[List[CompleteStat]] = {

    getContributorStatsInEveryRepo()
      .map(_.groupBy(_.author)
        .mapValues(_.map(_.total).sum)
        .map{
          case (k: Author, v: Int) => CompleteStat(name = k.login, contributions = v)
        }).map(_.toList.sortBy(stat => stat.contributions)(Ordering[Int].reverse))
  }
  private def getContributorStatsInEveryRepo()(implicit ec: ExecutionContext,
                                               as: ActorSystem,
                                               unmarshallerContr: Unmarshaller[ResponseEntity, List[ContributorStat]],
                                               unmarshallerRepo: Unmarshaller[ResponseEntity, List[Repo]],
                                               connection: Flow[HttpRequest, HttpResponse, Future[Http.OutgoingConnection]],
                                               gitHubAuthenticator: GitHubAuthenticator)
  : Future[List[ContributorStat]] ={

    for{
      repos <-  DataCollector.collectData[Repo](Uri(s"https://api.github.com/orgs/$orgName/repos"))
      contributorStats <- Future.sequence(repos.map(
        (repo: Repo) => DataCollector.collectData[ContributorStat](Uri(s"https://api.github.com/repos/$orgName/${repo.name}/stats/contributors"))))
        .map(_.flatten)
    } yield contributorStats

  }
}
