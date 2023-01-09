package freezing

/** Model of athlete with points normalized to per day. */
final case class Athlete(id: Long, name: String, email: String, points: Double) {
  /** Is a zero pointer. */
  def zero: Boolean = points == 0.0

  /** Returns a repointing this athlete by mixing in points-per-day from a prior competition. */
  def repoint(priorPoints: Double, priorWeight: Double): Athlete =
    copy(points = points * (1.0 - priorWeight) + priorPoints * priorWeight)
}
