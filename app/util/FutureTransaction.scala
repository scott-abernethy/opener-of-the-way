package util

import concurrent.Future
import org.squeryl.PrimitiveTypeMode._

object FutureTransaction {

  def futureTransaction[A](a: => A): Future[A] = {
    Future(
      transaction(a)
    )(Context.dbOperations)
  }

  def inFutureTransaction[A](a: => A): Future[A] = {
    Future(
      inTransaction(a)
    )(Context.dbOperations)
  }

}
