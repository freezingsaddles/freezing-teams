package freezing

import scala.util.control.NoStackTrace

/** An error. */
final case class Fatality(message: String) extends Exception(message) with NoStackTrace
