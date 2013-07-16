package model

import java.sql.Timestamp
import gate.T
import org.squeryl.PrimitiveTypeMode._
import play.api.libs.json.Json

class Babble extends MythosObject {
  var when: Timestamp = T.now
  var who: String = ""
  var text: String = ""

  override def toString = "Babble[" + who + " -> " + text + "]"
  
  def toJson() = Json.obj("who" -> who, "text" -> text)
}

object Babble {
  def recent(size: Int): List[Babble] = {
    transaction( from(Mythos.babbles)(b => select(b) orderBy(b.when desc)).page(0, size).toList )
  }
  
  def add(who: String, text: String): Option[Babble] = {
    val b = new Babble
    b.who = who
    b.text = text
    val added = transaction {
      Mythos.babbles.insert(b)
    }
    Some(added)
  }
}