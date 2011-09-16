package code.util

import actors.Actor
import net.liftweb.common.Loggable

trait ExceptionLoggingActor extends Actor with Loggable
{
  override def exceptionHandler =
  {
    case e: Exception =>
    {
      logger.error("Actor exception " + e.getMessage, e)
    }
  }
}