package freezing

import scalaz.std.anyVal.*
import scalaz.std.list.*
import scalaz.std.string.*
import scalaz.syntax.foldable.*
import scalaz.syntax.std.boolean.*
import scaloi.syntax.foldable.*

import scala.annotation.tailrec

/** Assignment of athletes among a set of teams. */
final case class Assignment(
  size: Int,      // target team size
  points: Double, // target team points
  teams: List[Team],
):

  /** Number of zero pointers. */
  private val zeroes = teams.foldMap(_.zeroes)

  /** Minimum zero pointers allowed on a team. */
  private val minZeroes = Math.floorDiv(zeroes, teams.length)

  /** Maximum zero pointers allowed on a team. */
  private val maxZeroes = Math.floorDiv(zeroes + teams.length - 1, teams.length)

  /** The weakest team with smaller than the target team size. */
  def weakest: Team = teams.filter(_.size < size).minBy(_.points)

  /** The smallest team, for accepting zeroes. */
  def smallest: Team = teams.minBy(_.size)

  /** Standard deviation of this assignment from the ideal team distribution. */
  def standardDeviation: Double = Math.sqrt(teams.map(_.variance(points)).average)

  def locality(using zipCodes: ZipCodes): Double = Math.sqrt(teams.map(team => Math.pow(team.locality, 2)).average)

  def standardDeviationPlus(using args: Args, antagonists: Antagonists, zipCodes: ZipCodes): Double =
    Math.sqrt(
      teams
        .map(team =>
          team.variance(points) + team.antagonism + Math
            .pow(team.locality * args.localityWeight, 3)
        )
        .average
    )

  /** Construct a new assignment by adding an athlete to the appropriate team. */
  def +:(athlete: Athlete): Assignment = this + ((athlete.zero ? smallest | weakest) + athlete)

  /** Construct a new assignment by replacing one team with an alternate. */
  def +(team: Team): Assignment = copy(teams = team :: teams.filterNot(_.captain == team.captain))

  /** Find all possible alternate assignments created by exchanging just a single pair of athletes. */
  private def liaisons: Iterator[Assignment] = for
    tail    <- teams.tails if tail.nonEmpty
    aTeam    = tail.head     // for all possible A teams
    bTeam   <- tail.tail     // for all subsequent B teams
    aPlayer <- aTeam.players // for all A players
    if !aPlayer.zero || (bTeam.zeroes < maxZeroes && aTeam.zeroes > minZeroes) // no violation of zero limits
    bPlayer <- bTeam.players // for all B players
    if !bPlayer.zero || (aTeam.zeroes < maxZeroes && bTeam.zeroes > minZeroes) // no violation of zero limits
  yield this + (aTeam - aPlayer + bPlayer) + (bTeam - bPlayer + aPlayer) // exchange the players

  /** Return rows of the team assignments. */
  def asRows: List[List[String]] = for
    (team, index) <- teams.zipWithIndex
    athlete       <- team.athletes.sortBy(_.id != team.captain)
    captain        = (athlete.id == team.captain) ?? "Yes"
  yield (1 + index).toString :: athlete.id.toString :: athlete.name :: athlete.email :: captain :: Nil

  /** Return roms of the map output. */
  def mapRows(using zipCodes: ZipCodes): List[List[String]] =
    for
      team     <- teams
      captain  <- team.athletes.find(a => a.id == team.captain).toList
      athlete  <- team.athletes
      zip      <- zipCodes.get(athlete.zipCode)
      r         = Math.random * Math.PI * 2
      d         = Math.random * .005 // small random to avoid pin overlap
      latitude  = zip.latitude + Math.sin(r) * d
      longitude = zip.longitude + Math.cos(r) * d
    yield captain.name.replaceAll("(\\S)\\S*$", "$1") :: latitude.toString :: longitude.toString :: Nil
end Assignment

object Assignment:
  final val Headers    = "Team" :: "Strava ID" :: "Name" :: "Email" :: "Captain" :: Nil
  final val MapHeaders = "Captain" :: "Latitude" :: "Longitude" :: Nil

  /** Generate a locally optimal team assignment. */
  @tailrec def optimise(
    current: Assignment
  )(using args: Args, antagonists: Antagonists, zipCodes: ZipCodes): Assignment =
    // This is super inefficient; could be done much better with a priority queue,
    // updating standard deviation only as players are exchanged.

    println(
      s"\u001b[FRMS: ${current.standardDeviationPlus} – ${current.standardDeviation}pt, ${current.locality}mi"
    ) // so side effect
    // Find the alternate team with the least standard deviation
    val alternate = current.liaisons.minBy(_.standardDeviationPlus)
    if alternate.standardDeviationPlus < current.standardDeviationPlus then
      // If it's better than the current assignment, try to optimise it more
      optimise(alternate)
    else
      // Else stick with what we have
      current
  end optimise
end Assignment
