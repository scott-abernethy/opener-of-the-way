package controllers

import play.api._
import play.api.mvc._
import util.Permission

object Application extends Controller with Permission {
  
  def index = PermittedAction { request =>
    Ok(views.html.index())
  }
  
}