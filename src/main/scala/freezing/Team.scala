package freezing

import scalaz.std.list._
import scaloi.syntax.foldable._

/** Team of athletes. */
final case class Team(captain: Long, athletes: List[Athlete]) {

  /** Total team points. */
  lazy val points: Double = athletes.map(_.points).sum

  /** All ids. */
  lazy val ids: Set[Long] = athletes.map(_.id).toSet + captain

  /** Number of team athletes. */
  def size: Int = athletes.size

  /** Just the player athletes. */
  def players: List[Athlete] = athletes.filterNot(_.id == captain)

  /** Variance from the ideal team points. */
  def variance(mean: Double): Double = (points - mean) * (points - mean)

  /** Number of zero pointers. */
  def zeroes: Int = athletes.count(_.zero)

  /** Construct a new team by adding an athlete to this team. */
  def +(athlete: Athlete): Team = copy(athletes = athlete :: athletes)

  /** Construct a new team by removing an athlete from this team. */
  def -(athlete: Athlete): Team = copy(athletes = athletes.filterNot(_.id == athlete.id))

  /** Add an antagonistic penalty for some pairings. */
  def antagonism(antagonists: List[Set[Long]]): Double =
    if (antagonists.map(_ & ids).exists(_.size > 1)) 1000 else 0

  /** Compute the RMS distance of all athletes from the captain. */
  def locality(zipCodes: Map[String, ZipCode]): Double = {
    // some captains don't have zips but they are first and so tend to be selected
    val captainZipOpt = athletes.reverse.findMap(a => zipCodes.get(a.zipCode))
    val distances     = for {
      player     <- players
      playerZip  <- zipCodes.get(player.zipCode)
      captainZip <- captainZipOpt
      distance    = playerZip - captainZip
      if distance < 50 // distance above 50 miles suggests bogus zip code
    } yield distance
    if (distances.isEmpty) 0.0 else Math.sqrt(distances.map(d => d * d).average)
  }
}
