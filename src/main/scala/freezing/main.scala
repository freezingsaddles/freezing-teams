package freezing

import scalaz.std.list.*
import scalaz.std.map.*
import scalaz.std.option.*
import scalaz.syntax.std.option.*
import scalaz.syntax.traverse.*
import scaloi.misc.Monoids.*
import scaloi.misc.TryInstances.*
import scaloi.syntax.foldable.*
import scaloi.syntax.option.*

import scala.util.{Success, Try}

/** Main entry point into generating freezing team assignments.
  */
@main def main(args: String*): Unit = attempt(args).get

def attempt(args: Seq[String]): Try[Unit] = for
  argo      <- Args(args) <@~* Fatality("Syntax error")
  given Args = argo

  regRows <- argo.registrationsCsv.readCsvWithHeader

  pointsRows <- argo.pointsCsv.readCsvWithHeader
  pointsMap   = pointsRows.toAthletePoints

  priorRows  <- argo.priorCsv.traverse(_.readCsvWithHeader)
  priorPoints = priorRows.foldZ(_.toAthletePoints)

  zipRows       <- argo.zipCodesCsv.cata(_.readCsvWithHeader, Try(Nil))
  given ZipCodes = zipRows.toZipCodes

  antagonistRows   <- argo.antagonistsCsv.traverse(_.readCsvWithHeader)
  given Antagonists = antagonistRows.foldZ(_.toAntagonists)

  allAthletes = regRows.toAthletes(pointsMap, priorPoints)

  (finalAssignment, stragglers) = solve(allAthletes)

  stragglerRows = stragglers.map: athlete =>
                    "" :: athlete.id.toString :: athlete.name :: athlete.email :: "" :: Nil

  _ <- argo.outputCsv.writeRows(finalAssignment.asRows ::: stragglerRows, Assignment.Headers)

  _ <- argo.outputMap.cata(_.writeRows(finalAssignment.mapRows, Assignment.MapHeaders), Success(()))
yield
  val locality = finalAssignment.teams.map(_.locality).average
  println(
    s"Wrote ${argo.outputCsv} (standard deviation ${finalAssignment.standardDeviation}, locality ${locality}mi)"
  )
  println(
    finalAssignment.teams
      .map(team => s"${team.points} / ${team.athletes.count(_.points == 0)} / ${team.locality.toInt}mi")
      .zipWithIndex
      .mkString("\n")
  )
end attempt

def solve(
  allAthletes: List[Athlete]
)(using args: Args, antagonists: Antagonists, zipCodes: ZipCodes): (Assignment, List[Athlete]) =
  // We will discard all non-riders except those needed to make the team sizes even
  val riderCount = allAthletes.count(_.nonZero) // Folks have ridden this year
  val teamCount  = allAthletes.count(_.possibleCaptain).min(riderCount / args.minTeamSize)
  val teamSize   = (riderCount + teamCount - 1).min(allAthletes.size) / teamCount

  // Take players and captains in registration order as long as they have some points by now
  val (athletes, stragglers) = allAthletes.sortBy(_.zero).splitAt(teamCount * teamSize)
  val (captains, players)    = athletes.sortBy(_.nonCaptain).splitAt(teamCount)

  val points = athletes.foldMap(_.points) / teamCount
  println(s"$teamCount teams, $teamSize athletes per team, target team points: $points")
  if stragglers.nonEmpty then println(s"${stragglers.size} stragglers, ${stragglers.count(_.zero)} with no points")

  // Form initial teams from just the captains
  val captainAssignment = Assignment(teamSize, points, captains.map(captain => Team(captain.id, captain :: Nil)))

  val (zeroes, heroes) = players.partition(_.zero)

  // Allocate zeroes evenly across the teams
  val zeroAssignment = zeroes.foldRight(captainAssignment)(_ +: _)

  // Allocate heroes across the teams, strongest player to the weakest team
  // Were this a perfect optimizer this initial allocation would not matter; as is, it has a random effect
  val initialAssignment = heroes.sortBy(_.points).foldRight(zeroAssignment)(_ +: _)

  // Then engage in some optimising liaisons
  val assignment = Assignment.optimise(initialAssignment)

  (assignment, stragglers)
end solve
