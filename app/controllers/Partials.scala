package controllers

import play.api.mvc._

object Partials extends Controller {

  def home() = Action { request =>
    Ok(views.html.home())
  }

  def tome() = Action { request =>
    Ok(views.html.tome())
  }

}
