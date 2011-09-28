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

class Flow extends CometActor with CometListener {

  var options: FlotOptions = new FlotOptions {}
//  var series: List[FlotSerie] = foo :: Nil
  val idPlaceholder = "flowgid"

  def registerWith = ArtifactServer

  override def lowPriority = {
    case _ => reRender
  }

  def render = {
    ("#" + idPlaceholder) #> ((in: NodeSeq) => (in ++ Flot.render(idPlaceholder, series(), options, JsCmds.Noop)))
  }

//    private def graph(in: NodeSeq, data_to_plot: FlotSerie): NodeSeq =
//  {
//    val graphDiv = in \\ "div" filter (_.attribute("class").exists(_.text contains "graph")) headOption
//    val graphDivId = graphDiv.flatMap(_.attribute("id").map(_.text))
//
//    in ++ Flot.render(graphDivId.getOrElse("foo"), List(data_to_plot), new FlotOptions {}, Flot.script(in))
//  }

  def series(): List[FlotSerie] = {
    val a = new FlotSerie {
      override def data = discoveredByDay()
      override def lines = Full(new FlotLinesOptions {
        override def show = Full(true)
        override def fill = Full(true)
      })
    }
    val b = new FlotSerie {
      override def data = requestedByDay()
      override def lines = Full(new FlotLinesOptions {
        override def show = Full(true)
        override def fill = Full(true)
      })
    }
    List(a,b)
  }

  def discoveredByDay(): List[(Double, Double)] = {
    var cal: Calendar = Calendar.getInstance()
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.HOUR_OF_DAY, 1)
    cal.getTime
    cal.add(Calendar.DAY_OF_YEAR, -30)
    val startDate = new Timestamp(cal.getTime.getTime)

    val cs = transaction(
      from(Mythos.artifacts)(c =>
        where(c.discovered > startDate)
        select(c)
      ).toList
    )

    val now: Calendar = Calendar.getInstance()
    var range = Map.empty[(Double,Double), Sample]
    for (i <- List.range(0, 30))
    {
      val d: Double = now.get(Calendar.DAY_OF_YEAR)
      val label = now.get(Calendar.DAY_OF_MONTH)
      val y: Double = now.get(Calendar.YEAR)
      val index: Int = 0 - i
      range = range + ((d,y) -> Sample(index, 0))
      now.add(Calendar.DAY_OF_YEAR, -1)
    }

    cal = Calendar.getInstance()
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

  def requestedByDay(): List[(Double, Double)] = {
    var cal: Calendar = Calendar.getInstance()
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.HOUR_OF_DAY, 1)
    cal.getTime
    cal.add(Calendar.DAY_OF_YEAR, -30)
    val startDate = new Timestamp(cal.getTime.getTime)

    val cs = transaction(
      from(Mythos.clones)(c =>
        where(c.requested > startDate)
        select(c)
      ).toList
    )

    val now: Calendar = Calendar.getInstance()
    var range = Map.empty[(Double,Double), Sample]
    for (i <- List.range(0, 30))
    {
      val d: Double = now.get(Calendar.DAY_OF_YEAR)
      val label = now.get(Calendar.DAY_OF_MONTH)
      val y: Double = now.get(Calendar.YEAR)
      val index: Int = 0 - i
      range = range + ((d,y) -> Sample(index, 0))
      now.add(Calendar.DAY_OF_YEAR, -1)
    }

    cal = Calendar.getInstance()
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

}
