package freezing

import kantan.csv._
import kantan.csv.ops._
import scalaz.std.either._
import scalaz.std.list._
import scalaz.syntax.traverse._
import scaloi.syntax.boolean._

import java.io.File
import scala.util.Try

/**
 * Main entry point into generating freezing team assignments.
 */
//noinspection ZeroIndexToHead
object FreezingTeams extends App {

  /** Read a CSV with a header. */
  def readRows(path: String): Try[List[List[String]]] =
    Try(new File(path).readCsv[List, List[String]](rfc.withHeader).sequence.toTry).flatten

  /** Write a CSV with a header. */
  def writeRows(path: String, rows: List[List[Long]], headers: List[String]): Try[Unit] =
    Try(new File(path).writeCsv(rows, rfc.withHeader(headers: _*)))

  /** The main line. */
  def tryIt(): Try[Unit] = // I wish I was a real IO
    for {
      _ <- (args.length == 3) <@~* Fatality("Syntax: run <captains.csv> <points.csv> <assignments.csv>")

      Array(captainsCsv, pointsCsv, assignmentsCsv) = args

      _ <- new File(captainsCsv).exists <@~* Fatality(s"Not found: $captainsCsv")
      _ <- new File(pointsCsv).exists <@~* Fatality(s"Not found: $pointsCsv")

      captainRows <- readRows(captainsCsv)
      captainIds   = captainRows.map(row => row(0).toLong).toSet

      athleteRows <- readRows(pointsCsv)
      athletes     = athleteRows.map(row => Athlete(row(0).toLong, row(1).toDouble))

      (captains, players) = athletes.partition(athlete => captainIds.contains(athlete.id))

      _ <- (captainIds.size == captains.size) <@~* Fatality("Missing captains")
      _ <- (athletes.size % captains.size == 0) <@~* Fatality(s"Uneven teams ${athletes.size}/${captains.size}")

      teams    = captains.size
      teamSize = athletes.size / teams
      points   = athletes.map(_.points).sum / teams
      _        = println(s"$teams teams, ${athletes.size} athletes, target team points: $points")

      // Form initial teams from just the captains
      captainAssignment = Assignment(teamSize, points, captains.map(captain => Team(captain.id, captain :: Nil)))

      // Allocate players across the teams, strongest player to the weakest team
      initialAssignment = players.sortBy(athlete => athlete.points).foldr(captainAssignment)(athlete => _ + athlete)

      // Then engage in some optimising liaisons
      finalAssignment = Assignment.optimise(initialAssignment)

      _ <- writeRows(assignmentsCsv, finalAssignment.asRows, Assignment.Headers)

    } yield {
      println(s"Wrote $assignmentsCsv (standard deviation ${finalAssignment.standardDeviation})")
    }

  tryIt().get
}
