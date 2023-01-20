import dfhdl.*
import scala.io.Source

object Utils {
  def binaryFileToIntVector(
      fileName: String,
      width: Int,
      depth: Int
  ): Vector[Int] = {
    if (fileName.isEmpty) return Vector.fill(depth)(0)

    val lines = Source.fromFile(fileName).getLines.toVector
    lines
      .filter(!_.startsWith("//"))
      .flatMap { line =>
        line
          .split(" ")
          .filter(_ != "")
          .map(_.toInt)
          .grouped(width)
          .map(_.foldLeft(0)((acc, x) => (acc << 1) + x))
      }
      .take(depth)
  }

  def hexFileToIntVector(
      fileName: String,
      width: Int,
      depth: Int
  ): Vector[Int] = {
    if (fileName.isEmpty) return Vector.fill(depth)(0)

    val lines = Source.fromFile(fileName).getLines.toVector
    lines
      .filter(!_.startsWith("//"))
      .flatMap { line =>
        line
          .takeWhile(_ != '/')
          .split("")
          .filter(_.nonEmpty)
          .filter(_ != " ")
          .map(Integer.parseInt(_, 16))
          .grouped(width / 4)
          .map(_.foldLeft(0)((acc, x) => (acc << 4) + x))
      }
      .take(depth)
  }

  // class PixelWrapper(val CORDW: Int):
  //   case class Pixel(
  //       x: SInt[CORDW.type] <> VAL,
  //       y: SInt[CORDW.type] <> VAL
  //   ) extends Struct

  // class ColorWrapper(val CHANW: Int):
  //   case class Color(
  //       red: UInt[CHANW.type] <> VAL,
  //       green: UInt[CHANW.type] <> VAL,
  //       blue: UInt[CHANW.type] <> VAL
  //   ) extends Struct

}

// case class Pixel[T](
//     x: T,
//     y: T
// ) extends Struct

// def isInBounds[T](pixel: Pixel[T], maxX: Int, maxY: Int): Boolean = {
//     pixel.x >= 0 && pixel.x < maxX && pixel.y >= 0 && pixel.y < maxY
// }
// class PixelWrapper(val CORDW: Int):
//     case class Pixel(
//         x: SInt[CORDW.type] <> VAL,
//         y: SInt[CORDW.type] <> VAL
//     ) extends Struct
// def isInBounds(pixel: PixelWrapper.Pixel, maxX: Int, maxY: Int): Boolean = {
//     pixel.x >= 0 && pixel.x < maxX && pixel.y >= 0 && pixel.y < maxY
// }
// extension (pixel: Pixel)
//     def isInBounds(maxX: Int, maxY: Int): Boolean = {
//         pixel.x >= 0 && pixel.x < maxX && pixel.y >= 0 && pixel.y < maxY
// }
