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
import scaloi.syntax.foldable._
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
  def writeRows(file: File, rows: List[List[String]], headers: List[String]): Try[Unit] =
    Try(file.writeCsv(rows, rfc.withHeader(headers: _*)))

  /** The main line. */
  def tryIt(): Try[Unit] = // I wish I was a real IO
    for {
      argo <- Args(args) <@~* Fatality("Syntax error")

      captainRows <- readRows(argo.captainsCsv)
      captainIds   = captainRows.map(row => row(0).toLong).toSet

      pointsRows <- readRows(argo.pointsCsv)
      pointsMap   = pointsRows.map2(row => row(0) -> row(1).toDouble / argo.pointsDays)

      zipRows <- argo.zipCodesCsv.cata(readRows, Try(Nil))
      zipCodes = zipRows.map(row => ZipCode(row(1), row(9).toDouble, row(10).toDouble)).groupUniqBy(_.zipCode)

      athleteRows <- readRows(argo.athletesCsv)
      stragglern   = athleteRows.length % captainIds.size
      _            = if (stragglern > 0) println(s"Dropping $stragglern stragglers")
      allAthletes  = athleteRows
                       .map(row => Athlete(row(0).toLong, row(1), row(2), pointsMap.get(row(0)).orZ, row(16)))
      baseAthletes = allAthletes
                       .dropRight(stragglern) // drop stragglers by registration time (assuming that's the order)
                       .sortBy(_.id) // then sort by id for some sense of order

      priorRows   <- argo.priorCsv.traverse(readRows)
      priorPoints  = priorRows.orZ.map2(row => row(0).toLong -> row(1).toDouble / argo.priorDays)

      athletes =
        baseAthletes.map(athlete => priorPoints.get(athlete.id).transform(athlete)(_.repoint(_, argo.priorWeight)))

      (captains, players) = athletes.partition(athlete => captainIds.contains(athlete.id))

      _ <- (captainIds.size == captains.size) <@~* Fatality("Missing captains")
      _ <- (athletes.size % captains.size == 0) <@~* Fatality(s"Uneven teams ${athletes.size}/${captains.size}")

      teams            = captains.size
      teamSize         = athletes.size / teams
      points           = athletes.map(_.points).sum / teams
      (zeroes, heroes) = players.partition(_.zero)
      _                = println(s"$teams teams, ${athletes.size} athletes (${zeroes.length} zeroes), target team points: $points")

      // Form initial teams from just the captains
      captainAssignment =
        Assignment(teamSize, points, captains.map(captain => Team(captain.id, captain :: Nil)), zipCodes)

      // Allocate zeroes evenly across the teams
      zeroAssignment    = zeroes.foldr(captainAssignment)(athlete => _ + athlete)

      // Allocate heroes across the teams, strongest player to the weakest team
      // Were this a perfect optimizer this initial allocation would not matter; as is, it has a random effect
      initialAssignment = heroes.sortBy(athlete => athlete.points).foldr(zeroAssignment)(athlete => _ + athlete)

      // Then engage in some optimising liaisons
      finalAssignment = Assignment.optimise(initialAssignment)

      stragglers = allAthletes
                     .takeRight(stragglern)
                     .map(athlete => "" :: athlete.id.toString :: athlete.name :: athlete.email :: "" :: Nil)

      _ <- writeRows(argo.outputCsv, finalAssignment.asRows ::: stragglers, Assignment.Headers)

    } yield {
      val locality = finalAssignment.teams.map(_.locality(zipCodes)).average
      println(
        s"Wrote ${argo.outputCsv} (standard deviation ${finalAssignment.standardDeviation}, locality ${locality}mi)"
      )
      println(
        finalAssignment.teams
          .map(team =>
            team.points + " / " + team.athletes.count(_.points == 0) + " / " + team.locality(zipCodes).toInt + "mi"
          )
          .zipWithIndex
          .mkString("\n")
      )
    }

  /** That optional transformation you always wished you had. */
  implicit class OptionalTransform[A](val self: Option[A]) {
    def transform[B](b: B)(f: (B, A) => B): B = self.cata(f(b, _), b)
  }

  tryIt().get
}
