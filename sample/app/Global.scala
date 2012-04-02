import play.api.mvc.RequestHeader
import play.api.{Logger, GlobalSettings, Application}
import play.plugin.routerdsl.RouterDslPlugin

object Global extends GlobalSettings {

  override def onStart(app: Application) {
    Logger.warn("initializing plugin:"+ app.plugin(classOf[RouterDslPlugin]))
  }

  override def onRouteRequest(request: RequestHeader) = {
    DslRoutes.handlerFor(request) match {
      case Some(found) => Some(found)
      case None =>
        super.onRouteRequest(request)
    }
  }
}