package freezing

import scalaz.syntax.std.boolean.*
import java.util.Calendar

/** Date things. */
object Dates:

  /** Number of days in last year's competition. */
  def competitionDaysLastYear: Int = competitionDays(lastYear)

  /** Expected number of days in a year's competition. */
  private def competitionDays(year: Int): Int =
    31 + (wasLeapYear(year) ? 29 | 28) + equinoxDates.getOrElse(year, throw Fatality(s"Unknown year: $year")) - 1

  /** Last year. */
  private def lastYear: Int = Calendar.getInstance.get(Calendar.YEAR) - 1

  /** Was last year a leap year. */
  private def wasLeapYear(year: Int): Boolean = (year % 4 == 0) && ((year % 100 != 0) || (year % 400 == 0))

  /** Equinox dates through the years. TODO: predict using astrology science. */
  private val equinoxDates = Map(2020 -> 19, 2021 -> 20, 2022 -> 20, 2023 -> 20, 2024 -> 19, 2025 -> 19)
end Dates
