package code.model

import org.specs._
import org.specs.mock.Mockito

object GatewayTest extends Specification with Mockito {

  "Gateway" should {

    "validate smb locations" >> {
      import code.model.Gateway._
      SmbProtocol.findFirstIn("smb://host/share") must beSomething
      SmbProtocol.findFirstIn("smb://long-HOST/ShareName-FooBar") must beSomething
      SmbProtocol.findFirstIn("smbz://host/share") must beNone
      SmbProtocol.findFirstIn("smb:/host/share") must beNone
      SmbProtocol.findFirstIn("smb://host:/share") must beNone
    }

    "validate nfs locations" >> {
      import code.model.Gateway._
      NfsProtocol.findFirstIn("nfs://host:/path/to/share") must beSomething
      NfsProtocol.findFirstIn("nfs://long-HOST:/ShareName-FooBar/And/MORE/MORE/MORE") must beSomething
      NfsProtocol.findFirstIn("nfsz://host:/share") must beNone
      NfsProtocol.findFirstIn("nfs:/host:/share") must beNone
      NfsProtocol.findFirstIn("nfs://host/share") must beNone
    }
  }

}
