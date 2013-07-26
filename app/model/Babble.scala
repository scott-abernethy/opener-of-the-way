package model

import java.sql.Timestamp
import gate.T
import org.squeryl.PrimitiveTypeMode._
import play.api.libs.json.Json
import concurrent.duration._

class Babble extends MythosObject {
  var at: Timestamp = T.now
  var who: String = ""
  var text: String = ""

  override def toString = "Babble[" + who + " -> " + text + "]"
  
  def toJson() = Json.obj("who" -> who, "text" -> text)
}

object Babble {
  
  lazy val purgeAfter = 30.days.toMillis
  
  def recent(size: Int): List[Babble] = {
    transaction( from(Mythos.babbles)(b => select(b) orderBy(b.at desc)).page(0, size).toList )
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