package freezing

import scalaz.syntax.std.option.*

/** That optional transformation you always wished you had. */
extension [A](self: Option[A]) def transform[B](b: B)(f: (B, A) => B): B = self.cata(f(b, _), b)

type AthletePoints = Map[Long, Double]

type Antagonists = List[Set[Long]]

extension (self: Antagonists)
  def precludes(ids: Set[Long]): Boolean =
    self.exists: antagonists =>
      (ids & antagonists).size > 1
