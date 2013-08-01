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

package state

import akka.actor.{ActorRef, Actor}
import model.Babble

case class NewBabble(who: String, text: String)

class BabbleServer extends Actor {

  val maxBabbles = 10
  lazy val stream: ActorRef = context.system.actorFor("/user/StateStream")
  var items: List[Babble] = List()

  override def preStart() {
    items = Babble.recent(maxBabbles)
  }
  
  def receive = {
    case 'List => {
      sender ! items
    }
    case NewBabble(who, text) if (text.trim.size > 0) => {
      Babble.add(who, text).foreach { b =>
        items = b :: items.take(maxBabbles - 1)
        stream ! b
      }
    }
  }
}