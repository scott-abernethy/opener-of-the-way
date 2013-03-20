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
    //in.replaceAll("\\.", "." + zeroWidthSpace).replaceAll("_", "_" + zeroWidthSpace).replaceAll("/", "/" + zeroWidthSpace)
    in
  }
}
