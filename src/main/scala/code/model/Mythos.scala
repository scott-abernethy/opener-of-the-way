package code.model

import org.squeryl.Schema
import net.liftweb.squerylrecord.RecordTypeMode._

object Mythos extends Schema {
  val cultists = table[Cultist]
  val tomes = table[Tome]
  val spells = table[Spell]

  val cultistToTomes = oneToManyRelation(cultists, tomes).via((c,t) => c.id === t.cultistId)
  val tomeToSpells = oneToManyRelation(tomes, spells).via((t,s) => t.id === s.tomeId)
}
