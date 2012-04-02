package controllers

import play.api.mvc._

class Application extends Controller {

  def index = Action {
    Ok("Injected Application.index => ")
  }

  def about = Action {
    Ok("Injected Application.about => ")
  }
  
  def user(id: Int) = Action {
    Ok("Injected Applcation.user(" + id + ")")
  }

}

object Application extends Controller {

  def index = Action {
    Ok("Static Application.index => ")
  }
}