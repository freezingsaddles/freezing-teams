package freezing

import scalaz.std.anyVal.*
import scalaz.std.list.*
import scalaz.std.string.*
import scalaz.syntax.foldable.*
import scalaz.syntax.std.boolean.*

import scala.annotation.tailrec
import scala.collection.parallel.CollectionConverters.*

/** Assignment of athletes among a set of teams. */
case class Assignment(
  size: Int,      // target team size
  points: Double, // target team points
  teams: List[Team],
  stragglers: List[Athlete],
):

  /** Number of zero pointers. */
  private val zeroes = teams.foldMap(_.zeroes)

  /** Minimum zero pointers allowed on a team. */
  private val minZeroes = zeroes.floorDiv(teams.length)

  /** Maximum zero pointers allowed on a team. */
  private val maxZeroes = zeroes.ceilDiv(teams.length)

  /** The weakest team with smaller than the target team size. */
  def weakest: Team = teams.filter(_.size < size).minBy(_.points)

  /** The smallest team, for accepting zeroes. */
  def smallest: Team = teams.minBy(_.size)

  /** Measure of locality of the teams. RMS of the deviation from captain's location. */
  def locality(using zipCodes: ZipCodes): Double = teams.map(_.locality).rms

  /** Standard deviation of this assignment from the ideal team distribution. */
  def standardDeviation: Double = teams.map(_.points - points).rms

  /** Standard deviation plus antagonism and locality penalties. */
  def standardDeviationPlus(using args: Args, polarities: Polarities, zipCodes: ZipCodes): Double =
    teams
      .map: team =>
        team.variance(points) + team.antagonism + ((team.locality * args.localityWeight) ^ 3)
      .rootMean

  /** Construct a new assignment by adding an athlete to the appropriate team. */
  def +:(athlete: Athlete)(using polarities: Polarities): Assignment =
    val coupleTeam = polarities.couples.find(_(athlete.id)).flatMap(couple => teams.find(_.ids.exists(couple)))
    this + (coupleTeam.getOrElse(athlete.zero ? smallest | weakest) + athlete)

  /** Construct a new assignment by replacing one team with an alternate. */
  def +(team: Team): Assignment = copy(teams = team :: teams.filterNot(_.captain == team.captain))

  /** Find all possible alternate assignments created by exchanging just a single pair of athletes. */
  private def liaisons(using polarities: Polarities): Iterator[Assignment] = for
    tail    <- teams.tails if tail.nonEmpty
    aTeam    = tail.head     // for all possible A teams
    bTeam   <- tail.tail     // for all subsequent B teams
    aPlayer <- aTeam.players // for all A players
    if !aPlayer.zero || (bTeam.zeroes < maxZeroes && aTeam.zeroes > minZeroes) // no violation of zero limits
    if !polarities.couples.exists(_(aPlayer.id)) // don't split couples
    bPlayer <- bTeam.players // for all B players
    if !bPlayer.zero || (aTeam.zeroes < maxZeroes && bTeam.zeroes > minZeroes) // no violation of zero limits
    if !polarities.couples.exists(_(bPlayer.id)) // don't split couples
  yield this + (aTeam - aPlayer + bPlayer) + (bTeam - bPlayer + aPlayer) // exchange the players

  /** Return rows of the team assignments and stragglers. */
  def asRows: List[List[String]] =
    val rows  = for
      (team, index) <- teams.zipWithIndex
      athlete       <- team.athletes.sortBy(_.id != team.captain)
      captain        = (athlete.id == team.captain) ?? "Yes"
    yield (1 + index).toString :: athlete.id.toString :: athlete.name :: athlete.forumId :: athlete.email :: captain :: Nil
    val dregs = stragglers.map: athlete =>
      "" :: athlete.id.toString :: athlete.name :: athlete.forumId :: athlete.email :: "" :: Nil
    rows ++ dregs

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
  final val Headers    = "Team" :: "Strava ID" :: "Name" :: "Forum ID" :: "Email" :: "Captain" :: Nil
  final val MapHeaders = "Captain" :: "Latitude" :: "Longitude" :: Nil

  /** Generate a locally optimal team assignment. */
  @tailrec def optimise(
    current: Assignment
  )(using args: Args, pol: Polarities, zipCodes: ZipCodes): Assignment =
    // This is super inefficient; could be done much better with a priority queue,
    // updating standard deviation only as players are exchanged.

    println(
      s"\u001b[FRMS: ${current.standardDeviationPlus} – ${current.standardDeviation}pt, ${current.locality}mi"
    ) // so side effect
    // Find the alternate team with the least standard deviation
    val alternate = current.liaisons.toVector.par.minBy(_.standardDeviationPlus)
    // If it's better than the current assignment, try to optimise it more else stick with what we have
    if alternate.standardDeviationPlus < current.standardDeviationPlus then optimise(alternate) else current
  end optimise
end Assignment
