package code.model

import org.squeryl._
import org.squeryl.PrimitiveTypeMode._
import code.gate.{Warn, Wake}
import code.comet.{ArtifactServer, ArtifactUpdated}

trait MythosObject extends KeyedEntity[Long] {
  var id: Long = 0
}

object Mythos extends Schema {
  val cultists = table[Cultist]
  val gateways = table[Gateway]
  val artifacts = table[Artifact]
  val clones = table[Clone]

  on(artifacts)(a => declare(
    a.path is(indexed),
    columns(a.gatewayId, a.path) are(unique, indexed)
  ))
  on(clones)(c => declare(
    c.artifactId is(indexed),
    c.forCultistId is(indexed),
    columns(c.artifactId, c.forCultistId) are(unique, indexed)
  ))

  val cultistToGateways = oneToManyRelation(cultists, gateways).via((c,g) => c.id === g.cultistId)
  val gatewayToArtifacts = oneToManyRelation(gateways, artifacts).via((g,a) => g.id === a.gatewayId)
  val artifactToClones = oneToManyRelation(artifacts, clones).via((a,cl) => a.id === cl.artifactId)
  val cultistToClones = oneToManyRelation(cultists, clones).via((c,cl) => c.id === cl.forCultistId)

  override def drop = super.drop
}
