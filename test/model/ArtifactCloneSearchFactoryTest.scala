/*
 * Copyright (c) 2013 Scott Abernethy.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package model

import org.specs2.mutable.Specification
import org.specs2.mock.Mockito

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
