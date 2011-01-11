package code.model

import org.squeryl.Schema
import net.liftweb.squerylrecord.RecordTypeMode._

object Mythos extends Schema {
  val cultists = table[Cultist]
  val gateways = table[Gateway]
  val artifacts = table[Artifact]
  val clones = table[Clone]

  on(artifacts)(a => declare(
    //a.path is(indexed),
    columns(a.gatewayId, a.path) are(unique, indexed)
  ))
  on(clones)(c => declare(
    columns(c.artifactId, c.forCultistId) are(unique, indexed)
  ))

  val cultistToGateways = oneToManyRelation(cultists, gateways).via((c,g) => c.id === g.cultistId.is)
  val gatewayToArtifacts = oneToManyRelation(gateways, artifacts).via((g,a) => g.id === a.gatewayId.is)
  val artifactToClones = oneToManyRelation(artifacts, clones).via((a,cl) => a.id === cl.artifactId.is)
  val cultistToClones = oneToManyRelation(cultists, clones).via((c,cl) => c.id === cl.forCultistId.is)
}
