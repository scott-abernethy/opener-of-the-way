package util

object FileUtil {
  def abbr(in: String): String = {
    val pathPart = in.lastIndexOf('/')
    val filePart = if (pathPart > 0) in.substring(pathPart + 1) else in
    filePart.take(3).toString.toLowerCase
  }

  //  val zeroWidthSpace = "&#8203;"
  val zeroWidthSpace = " "

  def splitable(in: String): String = {
    in.replaceAll("\\.", "." + zeroWidthSpace).replaceAll("_", "_" + zeroWidthSpace).replaceAll("/", "/" + zeroWidthSpace)
  }
}
