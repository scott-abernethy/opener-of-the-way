package controllers

import play.api.mvc._

object Partials extends Controller {

  def home() = Action { request =>
    Ok(views.html.home())
  }

  def tome() = Action { request =>
    Ok(views.html.tome())
  }

  def addGateway() = Action { request =>
    Ok(views.html.addGateway("Add Gateway"))
  }

  def editGateway() = Action { request =>
    Ok(views.html.addGateway("Edit Gateway"))
  }

}
