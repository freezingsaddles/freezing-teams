package freezing

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
end ZipCode

object ZipCode:
  private final val EarthRadius = 3958.8
