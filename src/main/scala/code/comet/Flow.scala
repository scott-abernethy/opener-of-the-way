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

case class Sample(index: Double, count: Double)
{
  def incr: Sample = Sample(index, count + 1)
}

object Sample
{
  def order(a: Sample, b: Sample): Boolean =
  {
    a.index < b.index
  }
}

trait StreamGraphComet {
  val legendOptions: FlotLegendOptions = new FlotLegendOptions {
      override def position = Full("nw")
      override def backgroundOpacity = Full(0.0)
    }

  private def filterOutZeroSamples(in: List[(Double, Double)]): List[(Double, Double)] = {
    in.filter(_._2 > 0)
  }

  def createSeries(discovered: List[(Double, Double)], requested: List[(Double, Double)]): List[FlotSerie] = {
    val glimpsedSeries = new FlotSerie {
      override def data = discovered
      override def color = Full(Right(4))
      override def lines = Full(new FlotLinesOptions {
        override def show = Full(true)
        override def fill = Full(true)
        override def lineWidth = Full(1)
      })
      override def label = Full("Glimpsed")
    }
    val requestedSeries = new FlotSerie {
      override def data = filterOutZeroSamples(requested)
      override def color = Full(Right(5))
      override def points = Full(new FlotPointsOptions {
        override def show = Full(true)
      })
      override def label = Full("Requested")
    }
    List(glimpsedSeries, requestedSeries)
  }
}

class Flow extends CometActor with CometListener with StreamGraphComet {

  val options: FlotOptions = new FlotOptions {
    override def xaxis = Full(new FlotAxisOptions {
      override def min = Full(-29.0)
      override def max = Full(0)
    })
    override def legend = Full(legendOptions)
  }
  val idPlaceholder = "flowgid"

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
    val startDate = ago30Days()

    val cs = transaction(
      from(Mythos.artifacts)(c =>
        where(c.discovered > startDate)
        select(c)
      ).toList
    )

    var range: Map[(Double, Double), Sample] = last30Days()

    val cal = Calendar.getInstance()
    for (c <- cs)
    {
      cal.setTime(c.discovered)
      val d: Double = cal.get(Calendar.DAY_OF_YEAR)
      val y: Double = cal.get(Calendar.YEAR)
      range.get((d,y)) match {
        case Some(sample) =>
          range = range + ((d,y) -> sample.incr)
        case _ =>
      }
    }

    range.values.toList.sortWith(Sample.order).map(sample => (sample.index, sample.count))
  }

  def requested(): List[(Double, Double)] = {
    val startDate = ago30Days()

    val cs = transaction(
      from(Mythos.clones)(c =>
        where(c.requested > startDate)
        select(c)
      ).toList
    )

    var range: Map[(Double, Double), Sample] = last30Days()

    val cal = Calendar.getInstance()
    for (c <- cs)
    {
      cal.setTime(c.requested)
      val d: Double = cal.get(Calendar.DAY_OF_YEAR)
      val y: Double = cal.get(Calendar.YEAR)
      range.get((d,y)) match {
        case Some(sample) =>
          range = range + ((d,y) -> sample.incr)
        case _ =>
      }
    }

    range.values.toList.sortWith(Sample.order).map(sample => (sample.index, sample.count))
  }

  def last30Days(): Map[(Double, Double), Sample] = {
    val now: Calendar = Calendar.getInstance()
    var range = Map.empty[(Double, Double), Sample]
    for (i <- List.range(0, 30)) {
      val d: Double = now.get(Calendar.DAY_OF_YEAR)
      val label = now.get(Calendar.DAY_OF_MONTH)
      val y: Double = now.get(Calendar.YEAR)
      val index: Int = 0 - i
      range = range + ((d, y) -> Sample(index, 0))
      now.add(Calendar.DAY_OF_YEAR, -1)
    }
    range
  }

  def ago30Days(): Timestamp = {
    val cal: Calendar = Calendar.getInstance()
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.HOUR_OF_DAY, 1)
    cal.getTime
    cal.add(Calendar.DAY_OF_YEAR, -30)
    new Timestamp(cal.getTime.getTime)
  }
}
