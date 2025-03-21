package freezing

import scalaz.Foldable
import scalaz.syntax.std.option.*
import scaloi.misc.Monoids.numericMonoid

import scala.math.Fractional.Implicits.infixFractionalOps

/** That optional transformation you always wished you had. */
extension [A](self: Option[A]) def transform[B](b: B)(f: (B, A) => B): B = self.cata(f(b, _), b)

type AthletePoints = Map[Long, Double]

type Antagonists = List[Set[Long]]

extension (self: Antagonists)
  /** Returns whether the antagonists preclude a given team. */
  def precludes(ids: Set[Long]): Boolean =
    self.exists: antagonists =>
      (ids & antagonists).size > 1

extension (self: Int)
  /** Divide, rounding any fractional amount down. */
  def floorDiv(a: Int): Int = self / a

  /** Divide, rounding any fractional amount up. */
  def ceilDiv(a: Int): Int = (self + a - 1) / a

extension (self: Double)
  /** Square root. */
  def sqrt: Double = Math.sqrt(self)

  /** Raise to the power. */
  def ^(pow: Double): Double = Math.pow(self, pow)

extension [F[_], A](self: F[A])
  /** Square root of average of the elements. */
  def rootMean(using F: Foldable[F], A: Fractional[A]): Double =
    A.toDouble(F.suml(self) / A.fromInt(F.length(self))).sqrt

  /** Square root of the average of the elements squared. */
  def rms(using F: Foldable[F], A: Fractional[A]): Double =
    A.toDouble(F.foldMap(self)(a => a * a) / A.fromInt(F.length(self))).sqrt
