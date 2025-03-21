package freezing

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import scalaz.std.list.*

class FreezingTest extends AnyFreeSpec with Matchers:
  "Assignment" - {
    "should optimise" in {
      given Args        = Args(minTeamSize = 2)
      given ZipCodes    = Map.empty
      given Antagonists = List.empty

      val kris    = Athlete(1, "Kris", "@", "20009", true, 20.0, None)
      val chris   = Athlete(2, "Chris", "@", "20912", true, 10.0, None)
      val krystal = Athlete(3, "Krystal", "@", "20910", false, 5.0, None)
      val cristle = Athlete(4, "Cristle", "@", "20005", false, 18.0, None)

      val athletes = List(chris, cristle, kris, krystal)

      val assignment = allocate(athletes)

      assignment.size shouldBe 2
      assignment.points shouldBe 3.78 +- .01
      assignment.standardDeviationPlus shouldBe .21 +- .01
      assignment.teams should contain theSameElementsAs List(
        Team(kris.id, List(krystal, kris)),
        Team(chris.id, List(cristle, chris)),
      )
      assignment.stragglers shouldBe empty
    }

    "should optimise with locality" in {
      given Args        = Args(minTeamSize = 2)
      given ZipCodes    =
        Map(
          "20005" -> ZipCode("20005", 38.9067, -77.0312),
          "20009" -> ZipCode("20009", 38.9202, -77.0375),
          "20910" -> ZipCode("20910", 38.9982, -77.0338),
          "20912" -> ZipCode("20912", 38.9832, -77.0007),
        )
      given Antagonists = List.empty

      val kris    = Athlete(1, "Kris", "@", "20009", true, 20.0, None)
      val chris   = Athlete(2, "Chris", "@", "20912", true, 10.0, None)
      val krystal = Athlete(3, "Krystal", "@", "20910", false, 5.0, None)
      val cristle = Athlete(4, "Cristle", "@", "20005", false, 18.0, None)

      val athletes = List(chris, cristle, kris, krystal)

      val assignment = allocate(athletes)

      assignment.size shouldBe 2
      assignment.points shouldBe 3.78 +- .01
      assignment.standardDeviationPlus shouldBe 2.74 +- .01
      assignment.teams should contain theSameElementsAs List(
        Team(kris.id, List(cristle, kris)),
        Team(chris.id, List(krystal, chris)),
      )
      assignment.stragglers shouldBe empty
    }
  }
  "Freezing Teams" - {
    "should math" in {
      9.0.sqrt shouldBe 3.0
      3.0 ^ 3 shouldBe 27.0
      List(1.0, 2.0, 3.0).rms shouldBe (14.0 / 3).sqrt
      List(1.0, 2.0, 3.0).rootMean shouldBe (6.0 / 3).sqrt
      1.floorDiv(2) shouldBe 0
      2.floorDiv(2) shouldBe 1
      1.ceilDiv(2) shouldBe 1
      2.ceilDiv(2) shouldBe 1
    }

    "should preclude antagonists" in {
      List(Set(1L, 2L), Set(1L, 3L)).precludes(Set(2L, 3L)) shouldBe false
      List(Set(1L, 2L), Set(1L, 3L)).precludes(Set(1L, 2L)) shouldBe true
    }
  }
end FreezingTest
