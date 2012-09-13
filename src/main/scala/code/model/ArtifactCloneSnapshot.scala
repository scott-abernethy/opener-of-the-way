package code.model

import org.squeryl.PrimitiveTypeMode._
import code.model.Mythos._
import java.util.{TimeZone, Date}
import code.gate.T
import collection.immutable.{HashMap, TreeMap}
import java.text.{SimpleDateFormat}

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

case class ArtifactCloneInfo(state: ArtifactState.Value, clones: Int)

class ArtifactCloneSnapshot(val notNewsAfter: Long) {
  val dateF = {
    val f = new SimpleDateFormat("yyyy-MM-dd',' EEEE")
    f.setTimeZone(TimeZone.getDefault)
    f
  }
  var currentCultistId: Long = -1L
  var items: TreeMap[String, List[Artifact]] = new TreeMap[String, List[Artifact]]
  var states: Map[Long, ArtifactCloneInfo] = _

  def stateFor(artifactId: Long): Option[ArtifactState.Value] = {
    states.get(artifactId).map(_.state)
  }

  def clonesFor(artifactId: Long): Option[Int] = {
    states.get(artifactId).map(_.clones)
  }

  def reload(cultistId: Long) {
    currentCultistId = cultistId
    items = new TreeMap[String, List[Artifact]]
    states = new HashMap[Long, ArtifactCloneInfo]
    val results: List[(Artifact, Long, Option[Clone], Option[Presence])] = inTransaction(join(artifacts, gateways, clones.leftOuter, presences.leftOuter)((a, g, c, p) =>
      where(a.witnessed > T.ago(Artifact.goneAfter) and (a.discovered > T.ago(notNewsAfter)))
      select((a, g.cultistId, c, p))
      orderBy(a.discovered desc, a.path desc)
      on(a.gatewayId === g.id, a.id === c.map(_.artifactId), a.id === p.map(_.artifactId))
    ).toList)
    val combined: Seq[(Artifact, Long, List[Clone], Option[Presence])] = results.foldRight(List.empty[(Artifact, Long, List[Clone], Option[Presence])]){ (in: (Artifact, Long, Option[Clone], Option[Presence]), out: List[(Artifact, Long, List[Clone], Option[Presence])]) =>
      out match {
        case head :: tail if (head._1 == in._1) =>
          ((in._1, in._2, in._3.toList ::: head._3, in._4)) :: tail
        case list =>
          ((in._1, in._2, in._3.toList, in._4)) :: list
      }
    }
    combined.foreach{ i =>
      val artifact: Artifact = i._1
      items = insertItem(items, artifact)
      parseState(artifact, cultistId, i._2, i._3, i._4).foreach(s =>
        states = states + ((artifact.id, ArtifactCloneInfo(s, i._3.size)))
      )
    }
  }

  def add(artifact: Artifact) {
    
  }

  def update(artifact: Long) {
    inTransaction(Artifact.find(artifact).flatMap(a => Cultist.find(currentCultistId).map((a, _))) match {
      case Some((a, c)) =>
        a.stateFor(c) match {
          case Some(state) =>
            val cloneCount = from(clones)(c => where(c.artifactId === artifact) select(c)).toList.size
            states = states + ((a.id, ArtifactCloneInfo(state, cloneCount.toInt)))
          case None => states = states - a.id
        }
      case _ =>
    })
  }

  private def insertItem(into: TreeMap[String, List[Artifact]], a: Artifact): TreeMap[String, List[Artifact]] = {
    discoveredGroup(a) match {
      case Some(key) =>
        val as = into.getOrElse(key.toString, Nil)
        if (!as.contains(a)) into + ((key.toString, a :: as)) else into
      case other => into
    }
  }

  private def parseState(artifact: Artifact, cultistId: Long, ownerId: Long, clones: Seq[Clone], presence: Option[Presence]) = {
    val clone: Option[Clone] = clones.find(_.forCultistId == cultistId)
    artifact.stateFor(cultistId, ownerId, clone, T.now, presence)
  }

  def discoveredGroup(a: Artifact): Option[String] = {
    for {
      timestamp <- Option(a.discovered)
      time = new Date(timestamp.getTime)
    }
    yield dateF.format(time)
  }

  def latestDayGroup(): String = {
    items.lastOption.map(_._1).getOrElse("-")
  }

  def indexForGroup(group: String): String = {
    group.substring(0, group.indexOf(','))
  }
}
