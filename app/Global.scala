import play.api.{Application, GlobalSettings}
import play.api.Logger
import model.Environment

object Global extends GlobalSettings {

  var db: Option[Db] = None

  override def beforeStart(app: Application) {
    super.beforeStart(app)

    Logger.info("It is coming")

    val data = new Db{}
    data.init
    data.clear
    data.populate
    db = Some(data)

    Environment.start
  }

  override def onStart(app: Application) {
    super.onStart(app)

    Logger.info("It is here")
  }

  override def onStop(app: Application) {
    super.onStop(app)

    Logger.info("It has gone")

    Environment.dispose

    db.foreach{_.close}
    db = None
  }
}
