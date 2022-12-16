package client

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

object DomainDefinitions {

  sealed trait GithubObject
  case class Author(login: String) extends GithubObject
  case class ContributorStat(author: Author, total: Int) extends GithubObject
  case class Repo(name: String) extends GithubObject

  case class CompleteStat(name: String, contributions: Int)


  object JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
    implicit val userFormat: RootJsonFormat[Repo] = jsonFormat1(Repo)
    implicit val authorFormat: RootJsonFormat[Author] = jsonFormat1(Author)
    implicit val contributorStatFormat: RootJsonFormat[ContributorStat] = jsonFormat2(ContributorStat)
    implicit val completeStatFormat: RootJsonFormat[CompleteStat] = jsonFormat2(CompleteStat)
  }
}
