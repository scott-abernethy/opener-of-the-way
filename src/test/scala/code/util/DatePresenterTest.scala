package code.util

import org.specs.runner.{ConsoleRunner, JUnit4}
import org.specs.Specification
import org.specs.mock.Mockito
import java.sql.Timestamp

class DatePresenterTestSpecsAsTest extends JUnit4(DatePresenterTestSpecs)
object DatePresenterTestSpecsRunner extends ConsoleRunner(DatePresenterTestSpecs)
object DatePresenterTestSpecs extends Specification with Mockito {
  "DatePresenter" should {
    "format minutes ago" >> {
      DatePresentation.ago(0, 4 * 1000) must be_==("<1 min ago")
      DatePresentation.ago(0, 60 * 1000) must be_==("1 min ago")
      DatePresentation.ago(0, 39 * 60 * 1000) must be_==("39 mins ago")
      DatePresentation.ago(1234567, (59 * 60 * 1000) + 1234567) must be_==("59 mins ago")
    }
    "format hours ago" >> {
      DatePresentation.ago(0, 1 * 60 * 60 * 1000) must be_==("1 hour ago")
      DatePresentation.ago(456789, (23 * 60 * 60 * 1000) + 456789 + 123) must be_==("23 hours ago")
    }
    "format days ago" >> {
      DatePresentation.ago(0, 24 * 60 * 60 * 1000L) must be_==("1 day ago")
      DatePresentation.ago(0, 25 * 60 * 60 * 1000L) must be_==("1 day ago")
      DatePresentation.ago(0, 13 * 24 * 60 * 60 * 1000L) must be_==("13 days ago")
      DatePresentation.ago(0, 350 * 24 * 60 * 60 * 1000L) must be_==("350 days ago")
    }
    "format years ago" >> {
      DatePresentation.ago(0, 370 * 24 * 60 * 60 * 1000L) must be_==("1 year ago")
    }
    "take current time if not specified" >> {
      DatePresentation.ago(System.currentTimeMillis - (15 * 60 * 60 * 1000L)) must be_==("15 hours ago")
      DatePresentation.ago(new Timestamp(System.currentTimeMillis).getTime - (15 * 60 * 60 * 1000L)) must be_==("15 hours ago")
    }
  }
}
