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
