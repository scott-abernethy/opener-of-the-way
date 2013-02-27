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