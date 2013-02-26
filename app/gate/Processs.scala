package gate

import concurrent.Future

case class Exit(exitValue: Int, lines: Seq[String], duration: Long)

trait Destroyable {
  def destroy
}

class NonDestroyable extends Destroyable {
  def destroy {}
}

trait Processs {
  def start(cmds: Seq[String]): (Future[Exit], Destroyable)
}

object ProcesssImpl extends Processs {
  import sys.process._
  def start(cmds: Seq[String]): (Future[Exit], Destroyable) = {
    val startMsec = System.currentTimeMillis
    var lines: Seq[String] = Nil
    val process = cmds.run(ProcessLogger(line => lines = lines :+ line), false)
    val future = Future {
      val value = process.exitValue() // blocks until result
      Exit(value, lines, System.currentTimeMillis - startMsec)
    }(util.Context.ioOperations)
    val destroyable = new Destroyable {
      def destroy { process.destroy() }
    }
    (future, destroyable)
  }
}