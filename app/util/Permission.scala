package util

import play.api.mvc._
import model.Cultist
import org.squeryl.PrimitiveTypeMode._
import scala.Some
import controllers.routes
import play.api.Play

case class PermittedRequest[A](cultistId: Long, private val request: Request[A]) extends WrappedRequest(request)

trait Permission extends Controller {

  def getCultistId(request: RequestHeader): Option[Long] = {
    request.session.get("cultist").flatMap{ c =>
      try {
        Some(c.toLong)
      }
      catch {
        case _: NumberFormatException => None
      }
    }
  }

  def PermittedAction[A](p: BodyParser[A])(f: PermittedRequest[A] => Result) = {
    Action(p) { request =>
      getCultistId(request).map { cultistId =>
        f(PermittedRequest(cultistId, request))
      }.getOrElse(Redirect(routes.Cultists.approach()))
    }
  }

  def PermittedAction(f: PermittedRequest[AnyContent] => Result): Action[AnyContent]  = {
    PermittedAction(parse.anyContent)(f)
  }

  def InsaneAction[A](p: BodyParser[A])(f: PermittedRequest[A] => Result) = {
    Action(p) { request =>
      getCultistId(request).map { cultistId =>
        if (Cultist.insane_?(cultistId)) {
          f(PermittedRequest(cultistId, request))
        }
        else {
          Forbidden("The sane have no business here, begone!")
        }
      }.getOrElse(Redirect(routes.Cultists.approach()))
    }
  }

  def InsaneAction(f: PermittedRequest[AnyContent] => Result): Action[AnyContent]  = {
    InsaneAction(parse.anyContent)(f)
  }

  def NonProductionAction[A](p: BodyParser[A])(f: Request[A] => Result) = {
    Action(p) { request =>
      import Play.current
      if (!Play.isProd) {
        f(request)
      }
      else {
        NotImplemented("Not implemented in production!")
      }
    }
  }

  def NonProductionAction(f: Request[AnyContent] => Result): Action[AnyContent]  = {
    NonProductionAction(parse.anyContent)(f)
  }

}
