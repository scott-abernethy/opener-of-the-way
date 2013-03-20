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

import org.specs2.mutable._
import org.specs2.mock.Mockito

object GatewayTest extends Specification with Mockito {

  "Gateway" should {

    "validate smb locations" >> {
      import model.Gateway._
      SmbProtocol.findFirstIn("smb://host/share") must beSome
      SmbProtocol.findFirstIn("smb://long-HOST/ShareName-FooBar") must beSome
      SmbProtocol.findFirstIn("smbz://host/share") must beNone
      SmbProtocol.findFirstIn("smb:/host/share") must beNone
      SmbProtocol.findFirstIn("smb://host:/share") must beNone
    }

    "validate nfs locations" >> {
      import model.Gateway._
      NfsProtocol.findFirstIn("nfs://host:/path/to/share") must beSome
      NfsProtocol.findFirstIn("nfs://long-HOST:/ShareName-FooBar/And/MORE/MORE/MORE") must beSome
      NfsProtocol.findFirstIn("nfsz://host:/share") must beNone
      NfsProtocol.findFirstIn("nfs:/host:/share") must beNone
      NfsProtocol.findFirstIn("nfs://host/share") must beNone
    }
  }

}
