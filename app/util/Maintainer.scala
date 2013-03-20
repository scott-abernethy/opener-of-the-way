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

//package util
//
//import gate.{Deactivate, Activate}
//import akka.actor.{Actor, ActorRef}
//
//case object Maintain
//
//class Maintainer(subject: ActorRef, maintainDelay: Long) extends Actor {
//  case object Pulse
//  var active = false
//
//
//  def receive = {
//    case Activate =>
//      if (!active) {
//        active = true
//        self ! Pulse
//      }
//    case Deactivate =>
//      active = false
//    case Pulse =>
//      if (active) {
//        val maintainer = self
//        Schedule.schedule(() => maintainer ! Pulse, maintainDelay)
//        subject ! Maintain
//      }
//    case msg => {
//      unhandled(msg)
//    }
//  }
//}
