package util

import play.api.mvc.{Controller, RequestHeader, Action}
import model.Cultist
import org.squeryl.PrimitiveTypeMode._

trait Permission extends Controller {

  def Permitted[A](action: Long => Action[A]): Action[A] = {

    def getCultistId(request: RequestHeader): Option[Long] = {
      request.session.get("cultist").flatMap{c =>
        try {
          Some(c.toLong)
        } catch {
          case _: NumberFormatException => None
        }
      }
    }

    // Wrap the original BodyParser with authentication
    val authenticatedBodyParser = parse.using { request =>
      getCultistId(request).map(c => action(c).parser).getOrElse {
        parse.error(Unauthorized("Unauthorized"))
      }
    }

    // Now let's define the new Action
    Action(authenticatedBodyParser) { request =>
      getCultistId(request).map(u => action(u)(request)).getOrElse {
        Unauthorized("Unauthorized")
      }
    }

  }
}
