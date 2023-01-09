package freezing

/** Team of athletes. */
final case class Team(captain: Long, athletes: List[Athlete]) {

  /** Total team points. */
  lazy val points: Double = athletes.map(_.points).sum

  /** Number of team athletes. */
  def size: Int = athletes.size

  /** Just the player athletes. */
  def players: List[Athlete] = athletes.filterNot(_.id == captain)

  /** Variance from the ideal team points. */
  def variance(mean: Double): Double = (points - mean) * (points - mean)

  /** Number of zero pointers. */
  def zeroes: Int = athletes.count(_.zero)

  /** Construct a new team by adding an athlete to this team. */
  def +(athlete: Athlete): Team = copy(athletes = athlete :: athletes)

  /** Construct a new team by removing an athlete from this team. */
  def -(athlete: Athlete): Team = copy(athletes = athletes.filterNot(_.id == athlete.id))
}
