package code

import code.model.Mythos
import org.squeryl.PrimitiveTypeMode._

object TestDb extends Db {
  override def init {
    super.init
    transaction {
      Mythos.drop
      Mythos.create
    }
  }
}
