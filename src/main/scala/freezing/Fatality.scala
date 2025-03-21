package freezing

import scala.util.control.NoStackTrace

/** An error. */
case class Fatality(message: String) extends Exception(message) with NoStackTrace
