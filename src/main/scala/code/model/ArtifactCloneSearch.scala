package code.model

import collection.immutable.{HashMap, TreeMap}
import java.util.{Date, TimeZone}
import code.gate.T
import org.squeryl.PrimitiveTypeMode._
import code.model.Mythos._

class ArtifactCloneSearchFactory {
  def create(cultistId: Long, search: String): Seq[(Artifact, Option[ArtifactState.Value], Option[Int])] = {
    val formattedSearch = formatSearch(search)
    val results: List[(Artifact, Long, Option[Clone], Option[Presence])] = inTransaction(join(artifacts, gateways, clones.leftOuter, presences.leftOuter)((a, g, c, p) =>
      where((a.witnessed > T.ago(Artifact.goneAfter)) and (a.path like formattedSearch))
      select((a, g.cultistId, c, p))
      orderBy(a.path asc)
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
    for {
      (artifact, ownerId, clones, presence) <- combined
      state = parseState(artifact, cultistId, ownerId, clones, presence)
      count = Some(clones.size)
    } yield (artifact, state, count)
  }
  
  private def parseState(artifact: Artifact, cultistId: Long, ownerId: Long, clones: Seq[Clone], presence: Option[Presence]) = {
    val clone: Option[Clone] = clones.find(_.forCultistId == cultistId)
    artifact.stateFor(cultistId, ownerId, clone, T.now, presence)
  }

  def formatSearch(in: String): String = {
    val out: Option[String] = for {
      s <- Option(in)
      surrounded = "%" + s + "%"
    } yield surrounded.replaceAll("[ *]", "%").replaceAll("[%]+", "%")

    out.getOrElse("%")
  }
}