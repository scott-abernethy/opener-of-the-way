package code.snippet

import net.liftweb.http.js.JsCmds.RedirectTo._
import net.liftweb.util.Helpers._
import code.comet.{BabbleItem, BabblingsServer}
import net.liftweb.http.js.JsCmds.{SetValById, RedirectTo}
import net.liftweb.http.js.JsCmds
import net.liftweb.http.{RequestVar, SHtml}
import code.model.Cultist

class Babble
{
  object babble extends RequestVar("")

  def input =
  {
    "#babbling" #> JsCmds.FocusOnLoad(SHtml.text(babble.is, x => babble(x), "class" -> "span-7")) &
    ".send" #> SHtml.submit("Send", () => {
      BabblingsServer ! BabbleItem(Cultist.attending.is.toOption, babble.is)
      babble("")
    })
  }
}
