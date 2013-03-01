package views

import model.{Clone, PresenceState, Presence}
import scala.xml.NodeSeq

object CloneHelpers {
  def cloneStatus(clone: Clone, presence: Option[Presence]): NodeSeq = presence match {
    case Some(p) if (p.state == PresenceState.present) => {
      if (clone.attempts > 0) {
        <span><span class="label label-success">Present</span> <span class="label label-warning">Clone</span> <span class="label">{clone.attempts}x</span></span>
      }
      else {
        <span><span class="label label-success">Present</span> <span class="label">Clone</span></span>
      }
    }
    case Some(p) if (p.attempts > 0) => {
      <span><span class="label label-warning">Presence</span> <span class="label">{p.attempts}x</span></span>
    }
    case _ => {
      <span><span class="label">Presence</span></span>
    }
  }
}
