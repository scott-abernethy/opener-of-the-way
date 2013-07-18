package util

import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import org.apache.commons.codec.binary.Hex
import org.apache.commons.codec.digest.DigestUtils
import org.mindrot.jbcrypt.BCrypt

object PasswordHash {

  def generate(password: String, serverWidePasswordSecret: String): String = {
    if (password.length == 0)
      throw new IllegalArgumentException("Password may not be zero-length")

    // We add the server's secret key to the password,
    // the idea is to require stealing both the server
    // key and the database, which might raise the bar
    // a bit.
    val intermediate = signature(password, serverWidePasswordSecret)

    BCrypt.hashpw(intermediate, BCrypt.gensalt())
  }

  def check(password: String, passwordHash: String, serverWidePasswordSecret: String): Boolean = {
    try {
      val intermediate = signature(password, serverWidePasswordSecret)
      BCrypt.checkpw(intermediate, passwordHash)
    }
    catch {
      case ex: IllegalArgumentException => false
    }
  }
  
  private def signature(message: String, key: String): String = {
    require(key.length > 0)

    val mac = Mac.getInstance("HmacSHA512");
    val signingKey = new SecretKeySpec(key.getBytes("utf-8"), "HmacSHA512");
    mac.init(signingKey);
    val messageBytes = message.getBytes("utf-8");
    val resultBytes = mac.doFinal(messageBytes);

    new String(Hex.encodeHex(resultBytes))
  }
  
}