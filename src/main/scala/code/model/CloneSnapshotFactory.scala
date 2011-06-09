package code.model

import org.squeryl.PrimitiveTypeMode._
import code.model.Mythos._
import code.gate.T

class CloneSnapshot(
  val awaiting: List[(Artifact, Option[ArtifactState.Value])],
  val cloned: List[(Artifact, Option[ArtifactState.Value])]
)
class CloneSnapshotFactory {
  def create(cultistId: Long): CloneSnapshot = {
    // todo compute in parallel...
    val (as, cs) = inTransaction((awaiting(cultistId), cloned(cultistId)))
    new CloneSnapshot(stateOf(cultistId, as), stateOf(cultistId, cs))
  }
  def awaiting(cultistId: Long) = {
    join(clones, artifacts)((c, a) =>
      where(c.forCultistId === cultistId and (c.state === CloneState.awaiting or c.state === CloneState.cloning))
        select((c, a))
        orderBy(c.requested desc)
        on(c.artifactId === a.id)
    ).toList
  }
  def cloned(cultistId: Long) = {
    join(clones, artifacts)((c, a) =>
      where(c.forCultistId === cultistId and c.state === CloneState.cloned)
        select((c, a))
        orderBy(c.attempted desc)
        on(c.artifactId === a.id)
    ).toList
  }
  def stateOf(cultistId: Long, in: List[(Clone, Artifact)]): List[(Artifact, Option[ArtifactState.Value])] = {
    for {
      (clone, artifact) <- in
      state = artifact.stateFor(cultistId, cultistId - 1, Some(clone), T.now)
    } yield (artifact, state)
  }
}

/*
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
    states = new HashMap[Long, ArtifactState.Value]
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
    combined.foreach{i =>
      val artifact: Artifact = i._1
      items = insertItem(items, artifact)
      parseState(artifact, cultistId, i._2, i._3).foreach(s =>
        states = states + ((artifact.id, s))
      )
    }
  }
  def add(artifact: Artifact) {
    
  }
  def update(artifact: Long) {
    inTransaction(Artifact.find(artifact).flatMap(a => Cultist.find(currentCultistId).map((a, _))) match {
      case Some((a, c)) =>
        a.stateFor(c) match {
          case Some(state) => states = states + ((a.id, state))
          case None => states = states - a.id
        }
      case _ =>
    })
  }
  private def insertItem(into: TreeMap[String, List[Artifact]], a: Artifact): TreeMap[String, List[Artifact]] = {
    def discoveredGroup(a: Artifact): Option[String] = {
      for {
        timestamp <- Option(a.discovered)
        time = new Date(timestamp.getTime)
      }
      yield dateF.format(time)
    }
    discoveredGroup(a) match {
      case Some(key) =>
        val as = into.getOrElse(key.toString, Nil)
        if (!as.contains(a)) into + ((key.toString, a :: as)) else into
      case other => into
    }
  }
  private def parseState(artifact: Artifact, cultistId: Long, ownerId: Long, clones: Seq[Clone]) = {
    val clone: Option[Clone] = clones.find(_.forCultistId == cultistId)
    artifact.stateFor(cultistId, ownerId, clone, T.now)
  }
}
*/
