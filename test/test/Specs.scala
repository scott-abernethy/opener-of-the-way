package test

import play.api.test.{FakeApplication, WithApplication}
import org.specs2.execute.{AsResult, Result}

abstract class WithTestApplication extends WithApplication(FakeApplication()) {
}