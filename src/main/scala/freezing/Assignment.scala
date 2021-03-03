package freezing

import scala.annotation.tailrec

/** Assignment of athletes among a set of teams. */
final case class Assignment(size: Int, points: Double, teams: List[Team]) {

  /** The weakest team with smaller than the target team size. */
  def weakest: Team = teams.filter(_.size < size).minBy(_.points)

  /** Standard deviation of this assignment from the ideal team distribution. */
  def standardDeviation: Double = Math.sqrt(teams.map(_.variance(points)).sum / teams.size)

  /** Construct a new assignment by adding an athlete to the weakest team. */
  def +(athlete: Athlete): Assignment = this + (weakest + athlete)

  /** Construct a new assignment by replacing one team with an alternate. */
  def +(team: Team): Assignment = copy(teams = team :: teams.filterNot(_.captain == team.captain))

  /** Find all possible alternate assignments created by exchanging just a single pair of athletes. */
  private def liaisons: Iterator[Assignment] = for {
    tail    <- teams.tails if tail.nonEmpty
    aTeam    = tail.head // for all possible A teams
    bTeam   <- tail.tail // for all subsequent B teams
    aPlayer <- aTeam.players // for all A players
    bPlayer <- bTeam.players // for all B players
  } yield this + (aTeam - aPlayer + bPlayer) + (bTeam - bPlayer + aPlayer) // exchange the players

  /** Return rows of the team assignments. */
  def asRows: List[List[Long]] = for {
    (team, index) <- teams.zipWithIndex
    athlete       <- team.athletes
  } yield 1 + index :: athlete.id :: Nil
}

object Assignment {
  final val Headers = "Team" :: "Athlete" :: Nil

  /** Generate a locally optimal team assignment. */
  @tailrec def optimise(current: Assignment): Assignment = {
    // This is super inefficient; could be done much better with a priority queue,
    // updating standard deviation only as players are exchanged.

    print(s"RMS: ${current.standardDeviation}\r") // so side effect
    // Find the alternate team with the least standard deviation
    val alternate = current.liaisons.minBy(_.standardDeviation)
    if (alternate.standardDeviation < current.standardDeviation) {
      // If it's better than the current assignment, try to optimise it more
      optimise(alternate)
    } else {
      // Else stick with what we have
      current
    }
  }
}
