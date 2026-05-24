package freezing

import scalaz.Monoid

type ZipCodes = Map[String, ZipCode]

case class ZipCode(zipCode: String, latitude: Double, longitude: Double):
  // https://en.wikipedia.org/wiki/Versine#hav
  def -(elsewhere: ZipCode): Double =
    val latDistance = Math.toRadians(latitude - elsewhere.latitude)
    val lngDistance = Math.toRadians(longitude - elsewhere.longitude)
    val sinLat      = Math.sin(latDistance / 2)
    val sinLng      = Math.sin(lngDistance / 2)
    val a           = sinLat * sinLat +
      (Math.cos(Math.toRadians(latitude)) *
        Math.cos(Math.toRadians(latitude)) *
        sinLng * sinLng)
    ZipCode.EarthRadius * 2 * Math.atan2(a.sqrt, (1 - a).sqrt)
  end -

  def toCartesian: Cartesian =
    val latRad = Math.toRadians(latitude)
    val lonRad = Math.toRadians(longitude)
    (x = Math.cos(latRad) * Math.cos(lonRad), y = Math.cos(latRad) * Math.sin(lonRad), z = Math.sin(latRad))
end ZipCode

object ZipCode:
  private final val EarthRadius = 3958.8

type Cartesian = (x: Double, y: Double, z: Double)

extension (self: Cartesian)
  def toLatLong: (latitude: Double, longitude: Double) =
    val r = Math.sqrt(self.x * self.x + self.y * self.y)
    (latitude = Math.toDegrees(Math.atan2(self.z, r)), longitude = Math.toDegrees(Math.atan2(self.y, self.x)))
  def +(other: Cartesian): Cartesian                   = (self.x + other.x, self.y + other.y, self.z + other.z)
  def /(scalar: Double): Cartesian                     = (self.x / scalar, self.y / scalar, self.z / scalar)

given Monoid[Cartesian] = Monoid.instance(_ + _, (0.0, 0.0, 0.0))
