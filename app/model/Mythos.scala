/*
 * Copyright (c) 2013 Scott Abernethy.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package model

import org.squeryl._
import org.squeryl.PrimitiveTypeMode._

trait MythosObject extends KeyedEntity[Long] {
  var id: Long = 0
}

object Mythos extends Schema {
  val cultists = table[Cultist]
  val gateways = table[Gateway]
  val artifacts = table[Artifact]
  val clones = table[Clone]
  val presences = table[Presence]
  val pseudonyms = table[Pseudonym]
  val babbles = table[Babble]

  on(cultists)(c => declare(
    c.email is(indexed, unique)
  ))
  on(artifacts)(a => declare(
    a.path is(indexed, dbType("varchar(255)")),
    columns(a.gatewayId, a.path) are(unique, indexed)
  ))
  on(clones)(c => declare(
    c.artifactId is(indexed),
    c.forCultistId is(indexed),
    columns(c.artifactId, c.forCultistId) are(unique, indexed)
  ))
  on(presences)(p => declare(
    p.artifactId is(indexed, unique)
  ))
  on(babbles)(b => declare(
    b.at is (indexed)
  ))

  val cultistToGateways = oneToManyRelation(cultists, gateways).via((c,g) => c.id === g.cultistId)
  val gatewayToArtifacts = oneToManyRelation(gateways, artifacts).via((g,a) => g.id === a.gatewayId)
  val artifactToClones = oneToManyRelation(artifacts, clones).via((a,cl) => a.id === cl.artifactId)
  val cultistToClones = oneToManyRelation(cultists, clones).via((c,cl) => c.id === cl.forCultistId)
  val artifactToPresences = oneToManyRelation(artifacts, presences).via((a,p) => a.id === p.artifactId)

  override def drop = super.drop
}
