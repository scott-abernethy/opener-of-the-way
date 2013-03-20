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

package views

import model.{CloneState, Clone, PresenceState, Presence}
import scala.xml.NodeSeq
import java.sql.Timestamp
import gate.Millis
import util.DatePresentation

object CloneHelpers {

  def presenceStatus(presence: Option[Presence]): NodeSeq = {
    val state = presence.map(_.state).getOrElse(PresenceState.called)
    val attempts = presence.map(_.attempts).getOrElse(0L)
    val (icon, clazz, warning) = (state, attempts) match {
      case (PresenceState.presenting, _) => ("icon-cog icon-large icon-spin", "s-cloning", NodeSeq.Empty)
      case (PresenceState.present, _) => ("icon-gift icon-large", "s-ok", NodeSeq.Empty)
      case (_, attempts) => ("icon-gift", "muted", warningLabel(attempts, 1, "icon-cog"))
    }
    <span class={clazz}><i class={icon} title={state.toString}></i> {warning}</span>
  }

  def cloneStatus(clone: Clone): NodeSeq = {
    val (icon, clazz, warning) = (clone.state, clone.attempts) match {
      case (CloneState.cloning, _) => ("icon-cog icon-large icon-spin", "s-cloning", NodeSeq.Empty)
      case (CloneState.cloned, _) => ("icon-asterisk icon-large", "s-ok", NodeSeq.Empty)
      case (_, attempts) => ("icon-asterisk", "muted", warningLabel(attempts, 1, "icon-cog"))
    }
    <span class={clazz}><i class={icon} title={clone.state.toString}></i> {warning}</span>
  }

  def agoThreshold(at: Long, now: Long, thresholdMsec: Long): NodeSeq = {
    val duration: Long = now - at
    val warning = warningLabel(duration, thresholdMsec, "icon-time")
    <span>{DatePresentation.ago(at, now)} {warning}</span>
  }

  def durationThreshold(duration: Long, thresholdMsec: Long): NodeSeq = {
    val warning = warningLabel(duration, thresholdMsec, "icon-time")
    <span>{DatePresentation.duration(duration)} {warning}</span>
  }

  def countThreshold(count: Long, threshold: Long): NodeSeq = {
    countThreshold(count, threshold, _.toString)
  }

  def countThreshold(count: Long, threshold: Long, renderer: Long => String): NodeSeq = {
    val warning = warningLabel(count, threshold, "icon-fire")
    <span>{renderer(count)} {warning}</span>
  }

  private def warningLabel(number: Long, threshold: Long, warningIcon: String): NodeSeq = {
    if (number > (9 * threshold)) {
      <span class="label label-important"><i class={warningIcon}></i></span>
    }
    else if (number > (3 * threshold)) {
      <span class="label label-warning"><i class={warningIcon}></i></span>
    }
    else if (number > threshold) {
      <span class="label label-info"><i class={warningIcon}></i></span>
    }
    else {
      NodeSeq.Empty
    }
  }
}
