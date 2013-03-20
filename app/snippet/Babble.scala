/*
 * Copyright (c) 2013 Scott Abernethy.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

//package snippet
//
//import net.liftweb.http.js.JsCmds.RedirectTo._
//import net.liftweb.util.Helpers._
//import comet.{BabbleItem, BabblingsServer}
//import net.liftweb.http.js.JsCmds.{SetValById, RedirectTo}
//import net.liftweb.http.{RequestVar, SHtml}
//import net.liftweb.http.js.{JsCmd, JsCmds}
//
//class Babble
//{
//  def input =
//  {
//    var babble = ""
//
//    def process(): JsCmd = {
//      BabblingsServer ! BabbleItem(code.model.Cultist.attending.is.toOption, babble)
//      babble = ""
//      JsCmds.SetValById("#babbling", "")
//    }
//
//    "#babbling" #> (SHtml.text(babble, babble = _, "placeholder" -> "Yaji Ash-Shuthath!", "class" -> "span-7") ++ SHtml.hidden(process))
//  }
//}
