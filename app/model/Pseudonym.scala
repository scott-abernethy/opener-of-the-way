package model

import model.Mythos._
import org.squeryl.PrimitiveTypeMode._

class Pseudonym extends MythosObject {
  var name: String = ""
}

object Pseudonym {
  def of(cultistId: Long): String = {
    inTransaction( pseudonyms.lookup(cultistId).map(_.name).getOrElse("xxx") )
  }
}