package freezing

import scaloi.syntax.collection.*

extension (self: CsvRows)
  def toZipCodes: ZipCodes =
    self
      .map: row =>
        ZipCode(row("Zip Code"), row.double("Latitude"), row.double("Longitude"))
      .groupUniqBy(_.zipCode)

  def toAntagonists: Antagonists =
    self.map: row =>
      row.values.map(_.toLong).toSet

  def toAthletePoints: AthletePoints =
    self.map2: row =>
      row.long("Athlete") -> row.double("Points")

  def toAthletes(points: AthletePoints, prior: AthletePoints)(using args: Args): List[Athlete] =
    self.map: row =>
      val id = row.long("Strava user ID")
      Athlete(
        id,
        row("First Name").trim + " " + row("Last Name").trim,
        row("E-mail"),
        row("Zip Code"),
        row("Willing to be a team captain?").startsWith("Y"),
        points.getOrElse(id, 0),
        prior.get(id),
      )

end extension

extension (self: Map[String, String])
  def long(key: String): Long     = self(key).toLong
  def double(key: String): Double = self(key).toDouble
