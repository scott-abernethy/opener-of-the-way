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

import model.Clone

object Shifting {
  def durationOf(clone: Clone): Long = {
    clone.waitPlusDuration()
  }

  def calculateMedian(in: List[Clone]): Long = {
    val durations = in.map(durationOf(_))
    if (durations.size > 0) {
      durations.sorted.apply(durations.size / 2)
    } else {
      0L
    }
  }

  def calculateMean(in: List[Clone]): Long = {
    val total = in.foldLeft(0L){ (sum, clone) =>
      sum + durationOf(clone)
    }

    if (in.size > 0) total / in.size else 0L;
  }
}