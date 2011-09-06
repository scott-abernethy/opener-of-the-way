package code.model

import collection.immutable.{HashMap, TreeMap}
import java.util.{Date, TimeZone}
import code.gate.T
import org.squeryl.PrimitiveTypeMode._
import code.model.Mythos._
import com.sun.jmx.trace.TraceImplementation

class ArtifactCloneSearchFactory {
  def create(cultistId: Long, search: String): Seq[(Artifact, Option[ArtifactState.Value])] = {
    val formattedSearch = formatSearch(search)
    val results: List[(Artifact, Long, Option[Clone])] = inTransaction(join(artifacts, gateways, clones.leftOuter)((a, g, c) =>
      where(a.path like formattedSearch)
      select((a, g.cultistId, c))
      orderBy(a.path asc)
      on(a.gatewayId === g.id, a.id === c.map(_.artifactId))
    ).toList)
    val combined: Seq[(Artifact, Long, List[Clone])] = results.foldRight(List.empty[(Artifact, Long, List[Clone])]){ (in: (Artifact, Long, Option[Clone]), out: List[(Artifact, Long, List[Clone])]) =>
      out match {
        case head :: tail if (head._1 == in._1) =>
          ((in._1, in._2, in._3.toList ::: head._3)) :: tail
        case list =>
          ((in._1, in._2, in._3.toList)) :: list
      }
    }
    for {
      (artifact, ownerId, clones) <- combined
      state = parseState(artifact, cultistId, ownerId, clones)
    } yield (artifact, state)
  }
  private def parseState(artifact: Artifact, cultistId: Long, ownerId: Long, clones: Seq[Clone]) = {
    val clone: Option[Clone] = clones.find(_.forCultistId == cultistId)
    artifact.stateFor(cultistId, ownerId, clone, T.now)
  }

  def formatSearch(in: String): String = {
    val out: Option[String] = for {
      s <- Option(in)
      surrounded = "%" + s + "%"
    } yield surrounded.replaceAll("[ *]", "%").replaceAll("[%]+", "%")

    out.getOrElse("%")
  }
}