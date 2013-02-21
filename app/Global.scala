import play.api.{Play, Application, GlobalSettings, Logger}
import play.api.Play.current
import model.Environment

object Global extends GlobalSettings {

  var db: Option[Db] = None

  override def beforeStart(app: Application) {
    super.beforeStart(app)

    Logger.info("It is coming")
  }

  override def onStart(app: Application) {
    super.onStart(app)

    Logger.info("It is here")

    val data = new Db{}
    data.init
    if (Play.isDev) {
      data.clear
      data.populate
    }
    db = Some(data)

    Environment.start
  }

  override def onStop(app: Application) {
    super.onStop(app)

    Logger.info("It has gone")

    Environment.dispose

    db.foreach{_.close}
    db = None
  }
}
