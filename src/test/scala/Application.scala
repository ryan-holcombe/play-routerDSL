import play.api.mvc.{Action, Controller}

class Application extends Controller {
    def index: Action[_] = Action {
        Ok("Applcation.index => " + Routing.index())
    }

    def about: Action[_] = Action {
        Ok("Application.about => " + Routing.about() + " or " + Routing.api.v2.about())
    }
}
