package code.comet

import net.liftweb.http.{CometActor, CometListener}
import xml.NodeSeq
import net.liftweb.http.js.JsCmds
import net.liftweb.common.Full
import java.util.Calendar
import code.model.Mythos
import code.gate.T
import java.sql.Timestamp
import org.squeryl.PrimitiveTypeMode._
import net.liftweb.widgets.flot._

class Spurt extends CometActor with CometListener with StreamGraphComet {

  var options: FlotOptions = new FlotOptions {
    override def xaxis = Full(new FlotAxisOptions {
      override def min = Full(1.0)
      override def max = Full(24.0)
    })
    override def legend = Full(legendOptions)
  }
  val idPlaceholder = "spurtgid"

  def registerWith = ArtifactServer

  override def lowPriority = {
    case _ => reRender
  }

  def render = {
    ("#" + idPlaceholder) #> ((in: NodeSeq) => (in ++ Flot.render(idPlaceholder, series(), options, JsCmds.Noop)))
  }

  def series(): List[FlotSerie] = {
    createSeries(discovered(), requested())
  }

  def discovered(): List[(Double, Double)] = {
    val startDate = agoStartOfDay()

    val cs = transaction(
      from(Mythos.artifacts)(c =>
        where(c.discovered > startDate)
        select(c)
      ).toList
    )

    var range: Map[Double, Sample] = lastDay()

    val cal = Calendar.getInstance()
    for (c <- cs)
    {
      cal.setTime(c.discovered)
      val d: Double = cal.get(Calendar.HOUR_OF_DAY)
      range.get(d) match {
        case Some(sample) =>
          range = range + (d -> sample.incr)
        case _ =>
      }
    }

    range.values.toList.sortWith(Sample.order).map(sample => (sample.index, sample.count))
  }

  def requested(): List[(Double, Double)] = {
    val startDate = agoStartOfDay()

    val cs = transaction(
      from(Mythos.clones)(c =>
        where(c.requested > startDate)
        select(c)
      ).toList
    )

    var range: Map[Double, Sample] = lastDay()

    val cal = Calendar.getInstance()
    for (c <- cs)
    {
      cal.setTime(c.requested)
      val d: Double = cal.get(Calendar.HOUR_OF_DAY)
      range.get(d) match {
        case Some(sample) =>
          range = range + (d -> sample.incr)
        case _ =>
      }
    }

    range.values.toList.sortWith(Sample.order).map(sample => (sample.index, sample.count))
  }

  def lastDay(): Map[Double, Sample] = {
    val now: Calendar = Calendar.getInstance()
    var range = Map.empty[Double, Sample]
    for (i <- List.range(0, now.get(Calendar.HOUR_OF_DAY))) {
      val index: Double = i + 1
      range = range + (index -> Sample(index, 0))
    }
    range
  }

  def agoStartOfDay(): Timestamp = {
    val cal: Calendar = Calendar.getInstance()
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.HOUR_OF_DAY, 1)
    cal.getTime
    new Timestamp(cal.getTime.getTime)
  }
}
