package controllers

import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import model.{ApproachSuccess, ApproachExpired, Cultist}
import play.api.libs.json.Json
import util.Permission
import org.squeryl.PrimitiveTypeMode._

object Cultists extends Controller with Permission {

  val approachForm = Form(
    tuple(
      "email" -> nonEmptyText,
      "password" -> nonEmptyText
    )
  )

  def become(id: Long) = Action { request =>
    Ok("Became " + id).withSession(
      request.session + ("cultist" -> id.toString)
    )
  }

  def approach = Action { request =>
    Ok(views.html.approach(approachForm))
  }

  def approachSubmit = Action { implicit request =>
    val form: Form[(String, String)] = approachForm.bindFromRequest
    form.fold(
      errors => BadRequest(views.html.approach(errors)),
      ok => ok match {
        case (email, password) => {
          Cultist.forEmail(email).map(c => (c,c.approach(password))) match {
            case Some((c, ApproachSuccess)) => Redirect(routes.Application.index()).withSession(session + ("cultist" -> c.id.toString))
            case Some((c, ApproachExpired)) => BadRequest("Expired") // TODO
            case _ => BadRequest(views.html.approach(form))
          }
        }
      }
    )
  }

  def withdraw = Action { request =>
    Redirect(routes.Cultists.approach()).withNewSession
  }

  def me = PermittedAction { request =>
    transaction( Cultist.find(request.cultistId) ).map{ c =>
      Ok(c.toJson)
    }.getOrElse{
      BadRequest
    }
  }

  def who(id: Long) = Action { request =>
    transaction( Cultist.find(id) ).map{ c =>
      Ok(c.toJson)
    }.getOrElse{
      BadRequest
    }
  }
}
