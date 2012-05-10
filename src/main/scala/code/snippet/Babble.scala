package code.snippet

import net.liftweb.http.js.JsCmds.RedirectTo._
import net.liftweb.util.Helpers._
import code.comet.{BabbleItem, BabblingsServer}
import net.liftweb.http.js.JsCmds.{SetValById, RedirectTo}
import net.liftweb.http.{RequestVar, SHtml}
import net.liftweb.http.js.{JsCmd, JsCmds}

class Babble
{
  def input =
  {
    var babble = ""

    def process(): JsCmd = {
      BabblingsServer ! BabbleItem(code.model.Cultist.attending.is.toOption, babble)
      babble = ""
      JsCmds.SetValById("#babbling", "")
    }

    "#babbling" #> (SHtml.text(babble, babble = _, "class" -> "span-7") ++ SHtml.hidden(process))
  }
}
