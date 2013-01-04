package util

import play.api.libs.iteratee.{Enumerator, Input, Done}
import play.api.libs.json.{Json, JsValue}

object ConcurrentUtil {
  def errorSocket(errorMessage: String) = {
    val iteratee = Done[JsValue,Unit]((),Input.EOF)
    val enumerator = Enumerator[JsValue](Json.obj("type" -> "error", "message" -> errorMessage)).andThen(Enumerator.enumInput(Input.EOF))
    (iteratee, enumerator)
  }
}
