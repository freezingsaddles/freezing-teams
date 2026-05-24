package freezing

import scalaz.std.list.*
import scalaz.std.map.*
import scalaz.std.option.*
import scalaz.syntax.traverse.*
import scaloi.misc.Monoids.*
import scaloi.misc.TryInstances.*
import scaloi.syntax.foldable.*
import scaloi.syntax.option.*

import scala.util.Try

/** Main entry point into generating freezing team assignments.
  */
@main def main(args: String*): Unit = attempt(args).get

def attempt(args: Seq[String]): Try[Unit] = for
  args      <- Args(args) <@~* Fatality("Syntax error")
  given Args = args

  zipRows       <- args.zipCodesCsv.traverse(_.readCsvWithHeader)
  given ZipCodes = zipRows.foldZ(_.toZipCodes)

  antagonistRows <- args.antagonistsCsv.traverse(_.readCsvWithHeader)
  antagonists     = antagonistRows.foldZ(_.toListSetLong)

  protagonistRows <- args.couplesCsv.traverse(_.readCsvWithHeader)
  couples          = protagonistRows.foldZ(_.toListSetLong)

  given Polarities = (antagonists, couples)

  pointsRows <- args.pointsCsv.readCsvWithHeader
  pointsMap   = pointsRows.toAthletePoints

  priorRows  <- args.priorCsv.traverse(_.readCsvWithHeader)
  priorPoints = priorRows.foldZ(_.toAthletePoints)

  registrationRows <- args.registrationsCsv.readCsvWithHeader
  registrants       = registrationRows.toAthletes(pointsMap, priorPoints).distinctBy(_.id)

  assignment = allocate(registrants)

  _ <- args.outputCsv.writeRows(assignment.asRows, Assignment.Headers)

  _ <- args.outputMap.traverse(_.writeRows(assignment.mapRows, Assignment.MapHeaders))
yield
  val locality = assignment.teams.map(_.locality).average
  println(
    s"Wrote ${args.outputCsv} (standard deviation ${assignment.standardDeviation}, locality ${locality}mi)"
  )
  println(
    assignment.teams
      .map(team => s"${team.points} / ${team.athletes.count(_.points == 0)} / ${team.locality.toInt}mi")
      .zipWithIndex
      .mkString("\n")
  )
end attempt

def allocate(
  registrants: List[Athlete]
)(using args: Args, polarities: Polarities, zipCodes: ZipCodes): Assignment =
  val dc        = zipCodes("20500")
  val locations = registrants.flatMap(r => zipCodes.get(r.zipCode)).filter(z => (z - dc) < 50).map(_.toCartesian)
  val centroid  = (locations.suml / locations.size).toLatLong
  println(s"Centroid $centroid")

  // We will discard all non-riders except those needed to make the team sizes even
  val riderCount = registrants.count(_.nonZero) // Folks have ridden this year
  val teamCount  = registrants.count(_.possibleCaptain).min(riderCount / args.minTeamSize)
  val teamSize   = (riderCount).min(registrants.size) / (teamCount)

  // Take players and captains in registration order as long as they have some points by now
  val (athletes, stragglers) = registrants.sortBy(_.zero).splitAt(teamCount * teamSize)
  val captains               = athletes.filter(_.possibleCaptain).take(teamCount)
  val players                = athletes.filterNot(captains.contains)

  val points = athletes.foldMap(_.points) / teamCount
  println(s"$teamCount teams, $teamSize athletes per team, target team points: $points")
  if stragglers.nonEmpty then println(s"${stragglers.size} stragglers, ${stragglers.count(_.zero)} with no points")

  // Form initial teams from just the captains
  val captainTeams      = captains.map(captain => Team(captain.id, captain :: Nil))
  val captainAssignment = Assignment(teamSize, points, captainTeams, stragglers)

  val (zeroes, heroes) = players.partition(_.zero)

  // Allocate zeroes evenly across the teams
  val zeroAssignment = zeroes.foldRight(captainAssignment)(_ +: _)

  // Allocate heroes across the teams, strongest player to the weakest team
  // Were this a perfect optimizer this initial allocation would not matter; as is, it has a random effect
  val initialAssignment = heroes.sortBy(_.points).foldRight(zeroAssignment)(_ +: _)

  // Then engage in some optimising liaisons
  Assignment.optimise(initialAssignment)
end allocate
