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
      case (_, 0) => ("icon-gift", "muted", NodeSeq.Empty)
      case (_, attempts) if (attempts < 5) => ("icon-gift", "muted", <span class="label label-warning"><i class="icon-cog"></i></span>)
      case (_, attempts) => ("icon-gift", "muted", <span class="label label-important"><i class="icon-cog"></i></span>)
    }
    <span class={clazz}><i class={icon} title={state.toString}></i> {warning}</span>
  }

  def cloneStatus(clone: Clone): NodeSeq = {
    val (icon, clazz, warning) = (clone.state, clone.attempts) match {
      case (CloneState.cloning, _) => ("icon-cog icon-large icon-spin", "s-cloning", NodeSeq.Empty)
      case (CloneState.cloned, _) => ("icon-asterisk icon-large", "s-ok", NodeSeq.Empty)
      case (_, 0) => ("icon-asterisk", "muted", NodeSeq.Empty)
      case (_, attempts) if (attempts < 5) => ("icon-asterisk", "muted", <span class="label label-warning"><i class="icon-cog"></i></span>)
      case (_, attempts) => ("icon-asterisk", "muted", <span class="label label-important"><i class="icon-cog"></i></span>)
    }
    <span class={clazz}><i class={icon} title={clone.state.toString}></i> {warning}</span>
  }

  def agoThreshold(at: Long, now: Long, thresholdMsec: Long): NodeSeq = {
    val duration: Long = now - at
    val warning = if (duration > (3 * thresholdMsec)) {
      <span class="label label-important"><i class="icon-time"></i></span>
    }
    else if (duration > thresholdMsec) {
      <span class="label label-warning"><i class="icon-time"></i></span>
    }
    else {
      NodeSeq.Empty
    }
    <span>{DatePresentation.ago(at, now)} {warning}</span>
  }

  def durationThreshold(duration: Long, thresholdMsec: Long): NodeSeq = {
    val warning = if (duration > (3 * thresholdMsec)) {
      <span class="label label-important"><i class="icon-time"></i></span>
    }
    else if (duration > thresholdMsec) {
      <span class="label label-warning"><i class="icon-time"></i></span>
    }
    else {
      NodeSeq.Empty
    }
    <span>{DatePresentation.duration(duration)} {warning}</span>
  }

  def countThreshold(count: Long, threshold: Long): NodeSeq = {
    countThreshold(count, threshold, _.toString)
  }

  def countThreshold(count: Long, threshold: Long, renderer: Long => String): NodeSeq = {
    val warning = if (count > (3 * threshold)) {
      <span class="label label-important"><i class="icon-fire"></i></span>
    }
    else if (count > threshold) {
      <span class="label label-warning"><i class="icon-fire"></i></span>
    }
    else {
      NodeSeq.Empty
    }
    <span>{renderer(count)} {warning}</span>
  }
}
