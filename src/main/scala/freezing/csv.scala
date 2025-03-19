package freezing

import kantan.csv.*
import kantan.csv.ops.*
import scalaz.std.either.*
import scalaz.std.list.*
import scalaz.syntax.traverse.*
import scaloi.syntax.collection.*

import java.io.File
import scala.util.Try

type CsvRows = List[Map[String, String]]

extension (self: File)
  /** Read a CSV with a header. */
  def readCsvWithHeader: Try[CsvRows] =
    Try:
      self.readCsv[List, Map[String, String]](rfc.withHeader).sequence.toTry
    .flatten

  /** Write a CSV with a header. */
  def writeRows(rows: List[List[String]], headers: List[String]): Try[Unit] =
    Try:
      self.writeCsv(rows, rfc.withHeader(headers*))
end extension

given HeaderDecoder[Map[String, String]] with
  override def fromHeader(header: Seq[String]): DecodeResult[RowDecoder[Map[String, String]]] =
    Right: (e: Seq[String]) =>
      Right(header.zip(e).toMap)

  override def noHeader: RowDecoder[Map[String, String]] = (e: Seq[String]) =>
    Right:
      e.zipWithIndex.map2:
        case (value, index) => s"$index" -> value
