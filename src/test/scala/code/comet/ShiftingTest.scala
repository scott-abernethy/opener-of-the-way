package code.comet

import org.specs.Specification
import org.specs.mock.Mockito
import java.sql.Timestamp
import gate.T
import model.{CloneState, Clone}

object ShiftingTest extends Specification with Mockito  {

  "Shifting" should {
    "calculate median duration for zero samples" >> {
      val in = List.empty[Clone]
      Shifting.calculateMedian(in) must be_==(0L)
    }
    
    "calculate median duration for one sample" >> {
      val start = T.yesterday.getTime
      val in = List(
        Clone.fake(-1, -1, CloneState.cloned, new Timestamp(start), new Timestamp(start+500), 45) // 545
      )
      Shifting.calculateMedian(in) must be_==(545L)
    }

    "calculate median duration for eight samples" >> {
      val start = T.yesterday.getTime
      val in = List(
        Clone.fake(-1, -1, CloneState.cloned, new Timestamp(start), new Timestamp(start+500), 5045), // 5545
        Clone.fake(-1, -1, CloneState.cloned, new Timestamp(start+300), new Timestamp(start+300+500), 144), // 644
        Clone.fake(-1, -1, CloneState.cloned, new Timestamp(start), new Timestamp(start), -2), // 0
        Clone.fake(-1, -1, CloneState.cloned, new Timestamp(start), new Timestamp(start), 345), // 345
        Clone.fake(-1, -1, CloneState.cloned, new Timestamp(start+33), new Timestamp(start+33), 1), // 1
        Clone.fake(-1, -1, CloneState.cloned, new Timestamp(start), new Timestamp(start+8800), 0), // 8800
        Clone.fake(-1, -1, CloneState.cloned, new Timestamp(start+500+100), new Timestamp(start+500), 579), // 579
        Clone.fake(-1, -1, CloneState.cloned, new Timestamp(start+1), new Timestamp(start+1+500), 9000) // 9500
      )
      Shifting.calculateMedian(in) must be_==(644L)
    }

    "calculate median duration for nine samples" >> {
      val start = T.yesterday.getTime
      val in = List(
        Clone.fake(-1, -1, CloneState.cloned, new Timestamp(start), new Timestamp(start+500), 5045), // 5545
        Clone.fake(-1, -1, CloneState.cloned, new Timestamp(start+300), new Timestamp(start+300+500), 144), // 644
        Clone.fake(-1, -1, CloneState.cloned, new Timestamp(start), new Timestamp(start), -2), // 0
        Clone.fake(-1, -1, CloneState.cloned, new Timestamp(start+500+100), new Timestamp(start+500), 579), // 579
        Clone.fake(-1, -1, CloneState.cloned, new Timestamp(start), new Timestamp(start), 345), // 345
        Clone.fake(-1, -1, CloneState.cloned, new Timestamp(start), new Timestamp(start), 9), // 9
        Clone.fake(-1, -1, CloneState.cloned, new Timestamp(start+33), new Timestamp(start+33), 1), // 1
        Clone.fake(-1, -1, CloneState.cloned, new Timestamp(start), new Timestamp(start+8800), 0), // 8800
        Clone.fake(-1, -1, CloneState.cloned, new Timestamp(start+1), new Timestamp(start+1+500), 9000) // 9500
      )
      Shifting.calculateMedian(in) must be_==(579L)
    }
  }
}