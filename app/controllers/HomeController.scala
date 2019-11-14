package controllers

import javax.inject._
import play.api.libs.json.{JsArray, JsResult}
import play.api.mvc.{Action, _}
import services.GeoService

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class HomeController @Inject()(cc: ControllerComponents, geoService: GeoService)(implicit ec: ExecutionContext) extends AbstractController(cc) {

  def search: Action[AnyContent] = Action.async { implicit request =>
    request.getQueryString("query").fold({
      Future.apply {
        BadRequest("Le paramètre query est indéfini.")
      }
    })({ query =>
      geoService.search(query).map({ jsResult: JsResult[JsArray] =>
        jsResult.fold({ _ =>
          InternalServerError("Erreur inattendue.")
        }, { jsArray => Ok(jsArray).withHeaders("Access-Control-Allow-Origin" -> "*")
        })
      })
    })
  }

  def searchOption: Action[AnyContent] = Action { implicit request =>
    Ok.withHeaders("Access-Control-Allow-Origin" -> request.headers.get("Origin").getOrElse("*"),
      "Access-Control-Allow-Headers" -> request.headers.get("Access-Control-Request-Headers").getOrElse("*"),
      "Access-Control-Allow-Methods" -> request.headers.get("Access-Control-Request-Method").getOrElse("GET")
    )
  }

  def byCode: Action[AnyContent] = Action.async { implicit request =>
    request.getQueryString("code").fold({
      Future.apply {
        BadRequest("Le paramètre code est indéfini.")
      }
    })({ code =>
      geoService.getNameFromCode(code).map(name => Ok(name).withHeaders("Access-Control-Allow-Origin" -> "*"))
    })
  }
}
