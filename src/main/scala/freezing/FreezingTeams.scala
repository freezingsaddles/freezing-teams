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
import scala.util.{Success, Try}

/** Main entry point into generating freezing team assignments.
  */
//noinspection ZeroIndexToHead
object FreezingTeams extends App {

  /** Read a CSV with a header. */
  def readRows(file: File): Try[List[Map[String, String]]] =
    Try(file.readCsv[List, Map[String, String]](rfc.withHeader).sequence.toTry).flatten

  implicit val MapDecoder: HeaderDecoder[Map[String, String]] = new HeaderDecoder[Map[String, String]] {
    override def fromHeader(header: Seq[String]): DecodeResult[RowDecoder[Map[String, String]]] =
      Right((e: Seq[String]) => Right(header.zip(e).toMap))

    override def noHeader: RowDecoder[Map[String, String]] = (e: Seq[String]) =>
      Right(e.zipWithIndex.map(t => t._2.toString -> t._1).toMap)
  }

  /** Write a CSV with a header. */
  def writeRows(file: File, rows: List[List[String]], headers: List[String]): Try[Unit] =
    Try(file.writeCsv(rows, rfc.withHeader(headers: _*)))

  /** The main line. */
  def tryIt(): Try[Unit] = // I wish I was a real IO
    for {
      argo <- Args(args) <@~* Fatality("Syntax error")

      pointsRows <- readRows(argo.pointsCsv)
      pointsMap   = pointsRows.map2(row => row("Athlete").toLong -> row("Points").toDouble / argo.pointsDays)

      regRows <- readRows(argo.registrationsCsv)

      captainLimit = regRows.length / 10 // Min team size 10

      captainIds = regRows
                     .filter(row => row("Willing to be a team captain?").startsWith("Y"))
                     .map(row => row("Strava user ID").toLong)
                     .filter(id => pointsMap.get(id).exists(_ > 0))
                     .take(captainLimit)
                     .toSet

      zipRows <- argo.zipCodesCsv.cata(readRows, Try(Nil))
      zipCodes = zipRows
                   .map(row => ZipCode(row("Zip Code"), row("Latitude").toDouble, row("Longitude").toDouble))
                   .groupUniqBy(_.zipCode)

      allAthletes = regRows
                      .map(row =>
                        Athlete(
                          row("Strava user ID").toLong,
                          row("First Name").trim + " " + row("Last Name").trim,
                          row("E-mail"),
                          pointsMap.get(row("Strava user ID").toLong).orZ,
                          row("Zip Code")
                        )
                      )
                      .sortBy(a => (!captainIds.contains(a.id), a.points == 0))

      stragglern = regRows.length % captainIds.size + captainIds.size
      stragglerz = allAthletes.takeRight(stragglern).count(_.points == 0)
      _          = if (stragglern > 0) println(s"Dropping $stragglern stragglers, $stragglerz with no points")

      baseAthletes =
        allAthletes
          .dropRight(stragglern) // drop stragglers by 0 points then registration time (assuming that's the order)
          .sortBy(_.id) // then sort by id for some sense of order

      pointless = baseAthletes.count(_.points == 0)
      _         = println(s"$pointless 0-point competitors")

      priorRows  <- argo.priorCsv.traverse(readRows)
      priorPoints = priorRows.orZ.map2(row => row("Athlete").toLong -> row("Points").toDouble / argo.priorDays)

      antagonistRows <- argo.antagonistsCsv.traverse(readRows)
      antagonists     = antagonistRows.orZ.map(row => row.values.map(_.toLong).toSet)

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
        Assignment(teamSize, points, captains.map(captain => Team(captain.id, captain :: Nil)), zipCodes, antagonists)

      // Allocate zeroes evenly across the teams
      zeroAssignment = zeroes.foldr(captainAssignment)(athlete => _ + athlete)

      // Allocate heroes across the teams, strongest player to the weakest team
      // Were this a perfect optimizer this initial allocation would not matter; as is, it has a random effect
      initialAssignment = heroes.sortBy(athlete => athlete.points).foldr(zeroAssignment)(athlete => _ + athlete)

      // Then engage in some optimising liaisons
      finalAssignment = Assignment.optimise(initialAssignment)(argo)

      stragglers = allAthletes
                     .takeRight(stragglern)
                     .map(athlete => "" :: athlete.id.toString :: athlete.name :: athlete.email :: "" :: Nil)

      _ <- writeRows(argo.outputCsv, finalAssignment.asRows ::: stragglers, Assignment.Headers)

      _ <- argo.outputMap.cata(writeRows(_, finalAssignment.mapRows, Assignment.MapHeaders), Success())
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
