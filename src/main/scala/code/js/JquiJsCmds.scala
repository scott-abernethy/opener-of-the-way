package code.js

import net.liftweb.http.js.JsCmds.After
import net.liftweb.http.js.jquery.JqJE
import net.liftweb.http.js.JE.JsRaw
import net.liftweb.util.Helpers._
import net.liftweb.http.js.{JsExp, JsMember, JsCmd, JsRules}

object JquiJsCmds {
  implicit def jsExpToJsCmd(in: JsExp) = in.cmd

  object BlindIn {
    def apply(id: String) = new BlindIn(id, JsRules.prefadeDuration, JsRules.fadeTime)
  }

  object BlindInFast {
    def apply(id: String) = new BlindIn(id, 0 seconds, 1 seconds)
  }

  case class BlindIn(id: String, duration: TimeSpan, fadeTime: TimeSpan) extends JsCmd {
    def toJsCmd = (After(duration, JqJE.JqId(id) ~> (new JsRaw("show( \"blind\" )" /*+ fadeTime.millis + ")"*/) with JsMember))).toJsCmd
  }
  
  object BlindOut {
    def apply(id: String) = new BlindOut(id, JsRules.prefadeDuration, JsRules.fadeTime)
  }

  object BlindOutFast {
    def apply(id: String) = new BlindOut(id, 0 seconds, 1 seconds)
  }

  case class BlindOut(id: String, duration: TimeSpan, fadeTime: TimeSpan) extends JsCmd {
    def toJsCmd = (After(duration, JqJE.JqId(id) ~> (new JsRaw("hide( \"blind\" )" /*+ fadeTime.millis + ")"*/) with JsMember))).toJsCmd
  }
  
  object Highlight {
    def apply(id: String) = new Highlight(id, JsRules.prefadeDuration, JsRules.fadeTime)
  }

  object HighlightFast {
    def apply(id: String) = new Highlight(id, 0 seconds, 1 seconds)
  }

  case class Highlight(id: String, duration: TimeSpan, fadeTime: TimeSpan) extends JsCmd {
    def toJsCmd = (After(duration, JqJE.JqId(id) ~> (new JsRaw("effect( \"highlight\" )" /*+ fadeTime.millis + ")"*/) with JsMember))).toJsCmd
  }
}
