package controllers

import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import model._
import play.api.libs.json.Json
import util.Permission
import org.squeryl.PrimitiveTypeMode._
import gate.T
import scala.Some

object Cultists extends Controller with Permission {

  val approachForm = Form(
    tuple(
      "email" -> nonEmptyText,
      "password" -> nonEmptyText
    )
  )

  def become(id: Long) = NonProductionAction { request =>
    Ok("Became " + id).withSession(cultistSessionData(id) : _*)
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
            case Some((c, ApproachSuccess)) => Redirect(routes.Application.index()).withSession(cultistSessionData(c.id) : _*)
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

  def who(id: Long) = NonProductionAction { request =>
    transaction( Cultist.find(id) ).map{ c =>
      Ok(c.toJson)
    }.getOrElse{
      BadRequest
    }
  }

  def activity() = InsaneAction { request =>
    val at = T.now.getTime
    val futureCultists = Cultist.all
    val futureRequests = Clone.lastRequest
    val futureClone = Clone.lastClone
    val futureProffer = Artifact.lastProffer
    val futureProffers = Artifact.proffers(T.startOfSevenDayPeriod())
    import util.Context.playDefault
    val futureReport = for {
      cultists <- futureCultists
      requests <- futureRequests
      clone <- futureClone
      proffer <- futureProffer
      proffers <- futureProffers
    } yield {
      cultists.map( x =>
        {
          val cid: Long = x._1.id
          (x._1,
            x._2,
            requests.getOrElse(cid, None).map(_.getTime),
            clone.getOrElse(cid, None).map(_.getTime),
            None,
            proffer.getOrElse(cid, None).map(_.getTime),
            None)
//            proffers.getOrElse(cid, None))
        }
      )
    }

    Async {
      futureReport.map( report =>
        Ok(views.html.report.activity(at, report))
      )
    }
  }
}
