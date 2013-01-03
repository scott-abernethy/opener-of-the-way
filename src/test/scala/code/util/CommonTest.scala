package code.util

import org.specs.Specification
import org.specs.mock.Mockito
import java.sql.Timestamp
import gate.{T, Millis}

object CommonTest extends Specification with Mockito  {

  "Millis" should {
    "compute days" >> {
      Millis.days(4) must be_==(4 * 24 * 60 * 60 * 1000L)
    }
    "compute hours" >> {
      Millis.hours(13) must be_==(13 * 60 * 60 * 1000L)
    }
    "compute minutes" >> {
      Millis.minutes(23) must be_==(23 * 60 * 1000L)
    }
    "compute seconds" >> {
      Millis.seconds(59) must be_==(59 * 1000L)
    }
  }
}