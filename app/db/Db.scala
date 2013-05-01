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

package db

import gate.{Millis, T}

import model._

import org.squeryl._
import internals.DatabaseAdapter
import org.squeryl.PrimitiveTypeMode._
import play.api.Play
import play.api.Play.current
import java.sql.Timestamp

trait Db {
  def init {
    val adapter = Play.configuration.getString("db.default.adapter").getOrElse("org.squeryl.adapters.H2Adapter")
    val adapterInstance: DatabaseAdapter = Class.forName(adapter).newInstance.asInstanceOf[DatabaseAdapter]
    SessionFactory.concreteFactory = Some(() => Session.create(play.api.db.DB.getConnection(), adapterInstance))
  }

  def clear {
    transaction {
      Mythos.drop
      Mythos.create
    }
  }

  def close {
    // do nothing, I guess play handles it.
  }

  def describe {
    transaction {
      Mythos.printDdl
    }
  }

  def populate {
        transaction {

          val p1 = new Pseudonym
          p1.name = "Oneman"
          val p2 = new Pseudonym
          p2.name = "Twofoo"
          val p3 = new Pseudonym
          p3.name = "Threepeet"
          val p4 = new Pseudonym
          p4.name = "Fournith"
          val p5 = new Pseudonym
          p5.name = "Fivv"
          Mythos.pseudonyms.insert(List(p1, p2, p3, p4, p5))

          val c1 = new Cultist
          c1.email = "foo@bar.com"
          c1.password = "foo"
          c1.insane = true
          val c2 = new Cultist
          c2.email = "two@bar.com"
          c2.password = "two"
          c2.expired = false
          val c3 = new Cultist
          c3.email = "fee@bar.com"
          c3.password = "fi"
          c3.expired = false
          val c4 = new Cultist
          c4.email = "four@bar.com"
          c4.password = "c4"
          c4.expired = true

          val foo = Mythos.cultists.insert(c1)
          val two = Mythos.cultists.insert(c2)
          val fee = Mythos.cultists.insert(c3)
          Mythos.cultists.insert(c4)

          var g1: Gateway = new Gateway
          g1.cultistId = foo.id
          g1.location = "smb://10.16.15.43/public"
          g1.path = "foobar"
          g1.localPath = ""
          g1.password = "treesaregreen"
          g1.source = true
          g1.sink = false
          g1.seen = T.ago(Millis.days(9))
          g1.scoured = T.ago(Millis.days(11))
          g1 = Mythos.gateways.insert(g1)
          var g2: Gateway = new Gateway
          g2.cultistId = foo.id
          g2.location = "smb://10.16.15.43/public"
          g2.path = "foobar-sink"
          g2.localPath = ""
          g2.password = "treesaregreen"
          g2.source = false
          g2.sink = true
          g2 = Mythos.gateways.insert(g2)
          var g3: Gateway = new Gateway
          g3.cultistId = two.id
          g3.location = "smb://10.16.15.43/public"
          g3.path = "frog/sheep/cow"
          g3.localPath = ""
          g3.password = "cowsaregreen"
          g3.source = true
          g3.sink = true
          g3 = Mythos.gateways.insert(g3)

          def insertArtifact(gatewayId: Long)(path: String, discovered: Timestamp) {
            val artifact = new Artifact
            artifact.gatewayId = gatewayId
            artifact.path = path
            artifact.discovered = discovered
            artifact.witnessed = T.now
            artifact.length = 9999L
            Mythos.artifacts.insert(artifact)
          }

          val g3Artifact = insertArtifact(g3.id) _

          g3Artifact("stuff/one.txt", T.ago(Millis.days(89)))
          g3Artifact("stuff/two.txt", T.ago(Millis.days(89)))
          g3Artifact("stuff/three.txt", T.ago(Millis.days(89)))
          g3Artifact("file.ext.ext2", T.ago(Millis.days(85)))
          g3Artifact("asdfasfsdafasdf", T.ago(Millis.days(85)))
          g3Artifact("dfefefefef", T.ago(Millis.days(85)))
          g3Artifact("fffeffffefffffff", T.ago(Millis.days(85)))
          g3Artifact("3322323", T.ago(Millis.days(85)))
          g3Artifact("rarararararar", T.ago(Millis.days(85)))
          g3Artifact("raar/aradrs/arasssrasr", T.ago(Millis.days(85)))
          g3Artifact("raar/aradrs/r", T.ago(Millis.days(85)))
          g3Artifact("raar/aradrs/7777.txt", T.ago(Millis.days(85)))
          g3Artifact("2323", T.ago(Millis.days(81)))
          g3Artifact("2322", T.ago(Millis.days(81)))
          g3Artifact("2321", T.ago(Millis.days(81)))
          g3Artifact("2320", T.ago(Millis.days(81)))

          var h = new Artifact
          h.gatewayId = g3.id
          h.path = "other/one/foo.giz"
          h.witnessed = T.ago(5 * 24 * 60 * 60 * 1000L)
          h.discovered = T.ago(79 * 24 * 60 * 60 * 1000L)
          h.length = 25235454L
          h = Mythos.artifacts.insert(h)
          var a = new Artifact
          a.gatewayId = g3.id
          a.path = "la/lo/lah"
          a.length = 8976L
          a.discovered = T.startOfSevenDayPeriod()
          a = Mythos.artifacts.insert(a)
          var b = new Artifact
          b.gatewayId = g3.id
          b.path = "la/foyhyyyyyyyy22"
          b.length = 98512376L
          b.witnessed = T.ago(13 * 24 * 60 * 60 * 1000L)
          b.discovered = T.startOfSevenDayPeriod()
          b = Mythos.artifacts.insert(b)
          var c = new Artifact
          c.gatewayId = g3.id
          c.path = "la/lo/ppppp55"
          c.length = 8;
          c.discovered = T.startOfSevenDayPeriod()
          c = Mythos.artifacts.insert(c)
          var d = new Artifact
          d.gatewayId = g3.id
          d.path = "la/lo/913913.try0"
          d.witnessed = T.ago(12 * 24 * 60 * 60 * 1000L)
          d.length = 48395434523543L
          d.discovered = T.startOfSevenDayPeriod()
          d = Mythos.artifacts.insert(d)
          var e = new Artifact
          e.gatewayId = g1.id
          e.path = "mee/neigh"
          e.length = 3453456L
          e.discovered = T.startOfSevenDayPeriod()
          e = Mythos.artifacts.insert(e)
          var f = new Artifact
          f.gatewayId = g1.id
          f.path = "mee/oink"
          f.witnessed = T.ago(12 * 24 * 60 * 60 * 1000L)
          f.length = 23954345235437L
          f.discovered = T.startOfSevenDayPeriod()
          f = Mythos.artifacts.insert(f)
          var g = new Artifact
          g.gatewayId = g3.id
          g.path = "/var/cache/mv/outgoing/A.Really.Super.Dooper.Long.File-name.Which.Could.Cause.Issues.On.Screen.archive.foo.bar.baz.mp3"
          g.witnessed = T.now
          g.length = 843562723L
          g.discovered = T.startOfSevenDayPeriod()
          g = Mythos.artifacts.insert(g)
          var i = new Artifact
          i.gatewayId = g1.id
          i.path = "mee/nurfnurf"
          i.witnessed = T.ago(5 * 24 * 60 * 60 * 1000L)
          i.discovered = T.ago(5 * 24 * 60 * 60 * 1000L)
          i.length = 252345L
          i = Mythos.artifacts.insert(i)
          var m = new Artifact
          m.gatewayId = g3.id
          m.path = "/88888"
          m.discovered = T.ago(Millis.days(3))
          m.witnessed = T.now
          m.length = 12345467L
          m = Mythos.artifacts.insert(m)
          var n = new Artifact
          n.gatewayId = g1.id
          n.path = "/ploiun/ploiun.p"
          n.discovered = T.ago(Millis.days(3))
          n.witnessed = T.now
          n.length = 54647L
          n = Mythos.artifacts.insert(n)
          var j = new Artifact
          j.gatewayId = g3.id
          j.path = "/fake"
          j.discovered = T.yesterday
          j.witnessed = T.now
          j.length = 34;
          j = Mythos.artifacts.insert(j)
          var k = new Artifact
          k.gatewayId = g3.id
          k.path = "/big.file"
          k.discovered = T.yesterday
          k.witnessed = T.now
          k.length = 43582342343L
          k = Mythos.artifacts.insert(k)
          var l = new Artifact
          l.gatewayId = g1.id
          l.path = "/give.it.er"
          l.discovered = T.yesterday
          l.witnessed = T.now
          l.length = 435892343L
          l = Mythos.artifacts.insert(l)

          val clone1 = new Clone
          clone1.artifactId = a.id
          clone1.forCultistId = foo.id
          clone1.state = CloneState.awaiting
          clone1.requested = T.yesterday
          clone1.attempted = T.yesterday
          clone1.attempts = 5
          Mythos.clones.insert(clone1)
          val clone4 = new Clone
          clone4.artifactId = k.id
          clone4.forCultistId = foo.id
          clone4.state = CloneState.awaiting
          clone4.requested = T.ago(Millis.days(4))
          clone4.attempted = T.yesterday
          clone4.attempts = 0
          Mythos.clones.insert(clone4)

          val clone3 = new Clone
          clone3.artifactId = b.id
          clone3.forCultistId = foo.id
          clone3.state = CloneState.awaiting
          clone3.requested = T.ago(Millis.days(9))
          clone3.attempted = T.now
          clone3.attempts = 1
          Mythos.clones.insert(clone3)

          val clone2 = new Clone
          clone2.artifactId = c.id
          clone2.forCultistId = foo.id
          clone2.state = CloneState.cloned
          clone2.requested = T.ago(Millis.days(4))
          clone2.attempted = T.ago(456789)
          clone2.attempts = 2
          clone2.duration = 123456
          Mythos.clones.insert(clone2)
        }
  }
}
