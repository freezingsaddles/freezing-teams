package freezing

import kantan.csv._
import kantan.csv.ops._
import scalaz.std.either._
import scalaz.std.list._
import scalaz.std.option._
import scalaz.syntax.std.option._
import scalaz.syntax.traverse._
import scaloi.misc.TryInstances._
import scaloi.syntax.boolean._
import scaloi.syntax.collection._
import scaloi.syntax.option._

import java.io.File
import scala.util.Try

/** Main entry point into generating freezing team assignments.
  */
//noinspection ZeroIndexToHead
object FreezingTeams extends App {

  /** Read a CSV with a header. */
  def readRows(file: File): Try[List[List[String]]] =
    Try(file.readCsv[List, List[String]](rfc.withHeader).sequence.toTry).flatten

  /** Write a CSV with a header. */
  def writeRows(file: File, rows: List[List[Long]], headers: List[String]): Try[Unit] =
    Try(file.writeCsv(rows, rfc.withHeader(headers: _*)))

  /** The main line. */
  def tryIt(): Try[Unit] = // I wish I was a real IO
    for {
      argo <- Args(args) <@~* Fatality("Syntax error")

      captainRows <- readRows(argo.captainsCsv)
      captainIds   = captainRows.map(row => row(0).toLong).toSet

      athleteRows <- readRows(argo.pointsCsv)
      baseAthletes = athleteRows.map(row => Athlete(row(0).toLong, row(1).toDouble / argo.pointsDays))

      priorRows  <- argo.priorCsv.traverse(readRows)
      priorPoints = priorRows.orZ.map2(row => row(0).toLong -> row(1).toDouble / argo.priorDays)

      athletes =
        baseAthletes.map(athlete => priorPoints.get(athlete.id).transform(athlete)(_.repoint(_, argo.priorWeight)))

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

      _ <- writeRows(argo.outputCsv, finalAssignment.asRows, Assignment.Headers)

    } yield {
      println(s"Wrote ${argo.outputCsv} (standard deviation ${finalAssignment.standardDeviation})")
    }

  /** That optional transformation you always wished you had. */
  implicit class OptionalTransform[A](val self: Option[A]) {
    def transform[B](b: B)(f: (B, A) => B): B = self.cata(f(b, _), b)
  }

  tryIt().get
}
