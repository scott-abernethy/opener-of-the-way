package code.util

import org.specs.Specification
import org.specs.mock.Mockito

object SizeTest extends Specification with Mockito {
  "Size" should {
    "display bytes" >> {
      Size.short(0L) must be_==("0B")
      Size.short(1L) must be_==("1B")
      Size.short(999L) must be_==("999B")
      Size.short(1000L) must be_==("1.0K")
      Size.short(1023L) must be_==("1.0K")
    }

    "display kilobytes" >> {
      Size.short(1024L) must be_==("1.0K")
      Size.short(2048L) must be_==("2.0K")
      Size.short(10249L) must be_==("10K")
      Size.short(740699L) must be_==("723K")
      Size.short(1024L * 999) must be_==("999K")
      Size.short(1024L * 1023) must be_==("1.0M")
    }

    "display megabytes" >> {
      Size.short(62121446L) must be_==("59M")
      Size.short(1024L * 1024 * 999) must be_==("999M")
      Size.short(1024L * 1024 * 1023) must be_==("1.0G")
    }

    "display gigabytes" >> {
      Size.short(5424283648L) must be_==("5.1G")
      Size.short(1024L * 1024 * 1024 * 999) must be_==("999G")
      Size.short(1024L * 1024 * 1024 * 1023) must be_==("1.0T")
    }

    "display points for items with only 1sd above the point" >> {
       Size.short(1229L) must be_==("1.2K")
       Size.short((1024L * 1024 * 3.7).toLong) must be_==("3.7M")
       Size.short((1024L * 1024 * 11.4).toLong) must be_==("11M")
       Size.short((1024L * 1024 * 11.6).toLong) must be_==("12M")
    }
  }
}