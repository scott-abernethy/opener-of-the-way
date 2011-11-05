package code.html

import net.liftweb.http.SHtml
import net.liftweb.http.js.JE.JsRaw
import xml.NodeSeq
import net.liftweb.http.js.JsCmds.Script
import net.liftweb.http.js.{JsCmd, JsExp}

object JqUi {

  implicit def jsExpToJsCmd(in: JsExp) = in.cmd
  
  class Button private (val text: Option[String], val f: (String) => JsCmd, val icon: Option[String]) {

    lazy val aCall = SHtml.ajaxCall(JsRaw(""), f)
    lazy val id = aCall._1

    def js : NodeSeq = {
      <span>{
          Script(
            JsRaw("""
      $(function() {
          $( '#""" + id + """' ).button(
          {
              icons: {
                  primary: """" + icon.get + """"
              },
              text: """ + text.isDefined + """
          }).
          click(function()
              {
                    """ + aCall._2.toJsCmd + """
              }
          );
      })
      """))
        }<button id={ id } class="small-ui-button">{ text }</button>
      </span>
    }
  }

  object Button {
    def apply(text: Option[String], f: (String) => JsCmd,
  icon: Option[String]) = {
      new Button(text, f, icon)
    }
  }
}