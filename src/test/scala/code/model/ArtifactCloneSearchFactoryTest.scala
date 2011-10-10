package code.model

import org.specs.Specification
import org.specs.mock.Mockito

object ArtifactCloneSearchFactoryTest extends Specification with Mockito {
  "ArtifactCloneSearchFactory" should {
    "format search input" >> {
      val x = new ArtifactCloneSearchFactory
      x.formatSearch(null) must be_==("%")
      x.formatSearch("") must be_==("%")
      x.formatSearch("  ") must be_==("%")
      x.formatSearch("goat") must be_==("%goat%")
      x.formatSearch(" hairy  goat   ") must be_==("%hairy%goat%")
      x.formatSearch("hai*y b* goat") must be_==("%hai%y%b%goat%")
      x.formatSearch("%b*i %in* ") must be_==("%b%i%in%")
      x.formatSearch("%*%*%%%%") must be_==("%")
    }
  }
}
