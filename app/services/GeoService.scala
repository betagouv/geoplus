package services

import javax.inject.Inject
import play.api.Configuration
import play.api.libs.json._
import play.api.libs.ws.{WSAuthScheme, WSClient, WSResponse}

import scala.concurrent.{ExecutionContext, Future}

@javax.inject.Singleton
class GeoService @Inject()(configuration: Configuration, ws: WSClient)(implicit ec: ExecutionContext) {

  private val host = configuration.underlying.getString("es.host")
  private val username = configuration.underlying.getString("es.username")
  private val password = configuration.underlying.getString("es.password")

  def getNameFromCode(code: String): Future[String] = {
    def extract(r: WSResponse): JsResult[JsString] = {
      val reader: Reads[JsString] = (__ \ Symbol("hits") \ Symbol("hits") \ 0 \ Symbol("_source") \ Symbol("name"))
        .json
        .pick[JsString]

      Json
        .parse(r.body)
        .transform(reader)
    }

    ws.url(s"$host/_search?pretty")
      .withAuth(username, password, WSAuthScheme.BASIC)
      .addHttpHeaders("Content-Type" -> "application/json")
      .withBody(s"""{"query" : {"match" : {"code" : {"query" : "$code"}}}}""")
      .get()
      .map(extract).map(_.asOpt.map(_.value).getOrElse(""))
  }

  def search(query: String): Future[JsResult[JsArray]] = {
    def extract(r: WSResponse): JsResult[JsArray] = {
      val reader: Reads[JsArray] = (__ \ Symbol("hits") \ Symbol("hits"))
        .json
        .pick[JsArray]
        .map({ jsArray: JsArray => JsArray(jsArray.\\("_source")) })

      Json
        .parse(r.body)
        .transform(reader)
    }

    val request =
      s"""
         |{ "query" : {
         |       "function_score" : {
         |           "query" : {
         |               "multi_match" : {
         |                   "query": "$query",
         |                   "fields" : ["code^1", "name^1"],
         |                   "fuzziness" : "10",
         |                   "prefix_length" : 2
         |               }
         |           },
         |           "functions": [{
         |               "filter" : { "match" : { "type" : "Commune" } },
         |               "weight" : 1.05
         |           }, {
         |               "filter" : { "match" : { "type" : "Region" } },
         |               "weight" : 1.20
         |           }, {
         |               "filter" : { "match" : { "type" : "Arrondissement" } },
         |               "weight" : 1.15
         |           }, {
         |               "filter" : { "match" : { "type" : "Departement" } },
         |               "weight" : 1.10
         |           }, {
         |               "filter" : { "match" : { "type" : "CantonOuVille2015" } },
         |               "weight" : 0
         |           }, {
         |               "filter" : { "match" : { "type" : "Canton2015" } },
         |               "weight" : 0
         |           }, {
         |               "filter" : { "match" : { "type" : "CommunauteCommunes" } },
         |               "weight" : 0
         |           }, {
         |               "filter" : { "match" : { "type" : "CantonOuVille" } },
         |               "weight" : 0
         |           }, {
         |               "filter" : { "match" : { "type" : "Canton" } },
         |               "weight" : 0
         |           }, {
         |               "filter" : { "match" : { "type" : "CommuneDeleguee" } },
         |               "weight" : 0
         |           }, {
         |               "filter" : { "match" : { "type" : "MetropoleOuAssimilee" } },
         |               "weight" : 0
         |           }, {
         |               "filter" : { "match" : { "type" : "CommuneEnDouble" } },
         |               "weight" : 0
         |           }, {
         |               "filter" : { "match" : { "type" : "CommuneFusionnee" } },
         |               "weight" : 0
         |           }, {
         |               "filter" : { "match" : { "type" : "CollectiviteDepartementaleOuCollectiviteTerritorialeEquivalente" } },
         |               "weight" : 0
         |           }, {
         |               "filter" : { "match" : { "type" : "SubdivisionPolynesieFrancaise" } },
         |               "weight" : 0
         |           }, {
         |               "filter" : { "match" : { "type" : "SubdivisionNouvelleCaledonie" } },
         |               "weight" : 0
         |           }, {
         |               "filter" : { "match" : { "type" : "CommunauteCommunes" } },
         |               "weight" : 0
         |           }, {
         |               "filter" : { "match" : { "type" : "CommunauteAgglomeration" } },
         |               "weight" : 0
         |           }, {
         |               "filter" : { "match" : { "type" : "CirconscriptionLegislative" } },
         |               "weight" : 0
         |           }, {
         |               "filter" : { "match" : { "type" : "CirconscriptionWallisFutuna" } },
         |               "weight" : 0
         |           }]
         |       }
         |}}
         |""".stripMargin

    // CommunauteAgglomeration

    ws.url(s"$host/_search?pretty")
      .addHttpHeaders("Content-Type" -> "application/json")
      .withAuth(username, password, WSAuthScheme.BASIC)
      .withBody(request)
      .get()
      .map(extract)
  }
}
