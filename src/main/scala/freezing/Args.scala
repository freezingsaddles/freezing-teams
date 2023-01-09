package freezing

import scalaz.syntax.std.boolean._
import scopt.OParser

import java.io.File

/** Team allocation arguments. */
final case class Args(
  captainsCsv: File = new File("."),
  athletesCsv: File = new File("."),
  pointsCsv: File = new File("."),
  outputCsv: File = new File("."),
  priorCsv: Option[File] = None,
  pointsDays: Int = 7,
  priorDays: Int = Dates.competitionDaysLastYear,
  priorWeight: Double = 0.5
)

/** Argument parsing. */
object Args {

  /** Parse arguments. */
  def apply(args: Array[String]): Option[Args] = OParser.parse(parser, args, defaults)

  private val defaults = Args()

  private val builder = OParser.builder[Args]

  private val parser = {
    import builder._
    OParser.sequence(
      programName("FreezingTeams"),
      head("Freezing teams", "0.2"),
      opt[File]("captains")
        .action((x, c) => c.copy(captainsCsv = x))
        .text("captains CSV file")
        .validate(fileExists)
        .required(),
      opt[File]("athletes")
        .action((x, c) => c.copy(athletesCsv = x))
        .text("current year athletes CSV file")
        .validate(fileExists)
        .required(),
      opt[File]("points")
        .action((x, c) => c.copy(pointsCsv = x))
        .text("current year points CSV file")
        .validate(fileExists)
        .required(),
      opt[File]("out")
        .action((x, c) => c.copy(outputCsv = x))
        .text("output CSV file")
        .required(),
      opt[File]("prior")
        .action((x, c) => c.copy(priorCsv = Some(x)))
        .text("prior year points CSV file")
        .validate(fileExists),
      opt[Int]("pointsDays")
        .action((x, c) => c.copy(pointsDays = x))
        .text(s"current year competition days (default ${defaults.pointsDays})")
        .optional(),
      opt[Int]("priorDays")
        .action((x, c) => c.copy(priorDays = x))
        .text(s"prior year competition days (default ${defaults.priorDays})")
        .optional(),
      opt[Double]("priorWeight")
        .action((x, c) => c.copy(priorWeight = x))
        .text(s"prior year weighting [0.0, 1.0] (default ${defaults.priorWeight})")
        .validate(x => ((x >= 0.0) && (x <= 1.0) either (()) or "Must be between 0.0 and 1.0").toEither)
        .optional()
    )
  }

  /** Validate that an input file exists. */
  def fileExists(file: File): Either[String, Unit] =
    (file.exists either (()) or s"Not found: $file").toEither
}
