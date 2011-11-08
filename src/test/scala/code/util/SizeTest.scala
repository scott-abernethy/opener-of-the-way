package code.util

import org.specs.Specification
import org.specs.mock.Mockito

object SizeTest extends Specification with Mockito {
  "Size" should {
    "display bytes" >> {
      Size.short(0L) must be_==("0")
      Size.short(1L) must be_==("1")
      Size.short(999L) must be_==("999")
    }

    "display kilobytes" >> {
      Size.short(1024L) must be_==("1K")
      Size.short(2048L) must be_==("2K")
    }

    "display megabytes" >> {
      Size.short(62121446L) must be_==("59M")
    }

    "display gigabytes" >> {
      Size.short(5424283648L) must be_==("5G")
    }
  }
}