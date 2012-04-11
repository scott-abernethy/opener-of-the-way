package code.snippet

import net.liftweb.http.js.JsCmds.RedirectTo._
import net.liftweb.util.Helpers._
import code.comet.{BabbleItem, BabblingsServer}
import net.liftweb.http.js.JsCmds.{SetValById, RedirectTo}
import net.liftweb.http.js.JsCmds
import net.liftweb.http.{RequestVar, SHtml}

class Babble
{
  object babble extends RequestVar("")

  def input =
  {
    "#babbling" #> SHtml.text(babble.is, x => babble(x)) &
    ".send" #> SHtml.submit("Send", () => {
      BabblingsServer ! BabbleItem(babble.is)
      babble("")
    })
  }
}
