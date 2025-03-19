package freezing

/** Model of athlete with points normalized to per day. */
final case class Athlete(
  id: Long,
  name: String,
  email: String,
  zipCode: String,
  willingCaptain: Boolean,
  ytdPoints: Double,
  priorPoints: Option[Double],
)(using args: Args):
  /** Effective daily points factoring in prior data if available */
  val points: Double = priorPoints match
    case Some(prior) =>
      (ytdPoints / args.pointsDays) * (1.0 - args.priorWeight) + (prior / args.priorDays) * args.priorWeight
    case None        =>
      ytdPoints / args.pointsDays

  def possibleCaptain: Boolean = willingCaptain && nonZero

  def nonCaptain: Boolean = !possibleCaptain

  /** Is a zero pointer. */
  def zero: Boolean = ytdPoints == 0

  /** Is not a zero pointer. */
  def nonZero: Boolean = !zero
end Athlete
