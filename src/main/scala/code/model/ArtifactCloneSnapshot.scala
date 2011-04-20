package code.model

import collection.immutable.TreeMap
import org.squeryl.PrimitiveTypeMode._
import code.model.Mythos._
import java.text.SimpleDateFormat
import java.util.{TimeZone, Date}

/*
There are probably more than one flavor of the N+1 problem, but a very
common one is :

for(p <- school.professors;
    c <- p.classes;
    s <- c.students)
  doSomething(p, s)

here the number of calls trips to the database will be :

 numberOfProcessorsInSchool * avg(sizeOfClassRoom)

to avoid this in Squeryl, you would do :

from(school.professors, classes, classes2students, students)((p,c,c2s,s) =>
  where(p.id === c.professorId and
        c2s.studentId === s.id and
        c2s.classId === c.id)
  select((p,s))
).foreach(t => doSomething(t._1, t._2))

which does it's thing with a single DB round trip.
 */

class ArtifactCloneSnapshot {
  val dateF = {
    val f = new SimpleDateFormat("yyyy-MM-dd',' EEEE")
    f.setTimeZone(TimeZone.getDefault)
    f
  }
  var currentCultistId: Long = -1L
  var items: TreeMap[String, List[Artifact]] = new TreeMap[String, List[Artifact]]
  var states: Map[Long, ArtifactState.Value] = _
  def reload(cultistId: Long) {
    currentCultistId = cultistId
    items = new TreeMap[String, List[Artifact]]
    val results: Seq[(Artifact, Long, Option[Clone])] = inTransaction(join(artifacts, gateways, clones.leftOuter)((a, g, c) =>
      select((a, g.cultistId, c))
      orderBy(a.discovered desc, a.path desc)
      on(a.gatewayId === g.id, a.id === c.map(_.artifactId))
    ).toSeq)
    val combined: Seq[(Artifact, Long, List[Clone])] = results.foldRight(List.empty[(Artifact, Long, List[Clone])]){ (in: (Artifact, Long, Option[Clone]), out: List[(Artifact, Long, List[Clone])]) =>
      out match {
        case head :: tail if (head._1 == in._1) =>
          ((in._1, in._2, in._3.toList ::: head._3)) :: tail
        case list =>
          ((in._1, in._2, in._3.toList)) :: list
      }
    }
    combined.foreach(i => items = insertItem(items, i._1))
    states = combined.map( a => (a._1.id, parseState(cultistId, a._2, a._3)) ).toMap
  }
  def add(artifact: Artifact) {
    
  }
  def update(artifact: Long) {
    inTransaction(Artifact.find(artifact).flatMap(a => Cultist.find(currentCultistId).map((a, _))) match {
      case Some((a, c)) =>
        a.stateFor(c).foreach(state => states = states + ((a.id, state)))
      case _ =>
    })
  }
  private def insertItem(into: TreeMap[String, List[Artifact]], a: Artifact): TreeMap[String, List[Artifact]] = {
    Option(a.discovered).map(timestamp => new Date(timestamp.getTime)).map(dateF format _) match {
      case Some(key) =>
        val as = into.getOrElse(key, Nil)
        if (!as.contains(a)) into + ((key, a :: as)) else into
      case other => into
    }
  }
  private def parseState(cultistId: Long, owner: Long, clones: Seq[Clone]) = {
    if (owner == cultistId) {
      ArtifactState.mine
    } else {
      val cloneState = clones.find(_.forCultistId == cultistId).map(_.state)
      cloneState match {
        case None => ArtifactState.available
        case Some(CloneState.queued) => ArtifactState.queued
        case Some(CloneState.progressing) => ArtifactState.progressing
        case Some(CloneState.done) => ArtifactState.done
        case _ => ArtifactState.failed
      }
    }
  }
}
