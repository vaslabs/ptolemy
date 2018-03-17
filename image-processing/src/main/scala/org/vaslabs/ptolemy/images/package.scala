package org.vaslabs.ptolemy

import java.nio.ByteBuffer

import scala.collection.mutable.ListBuffer

package object images {

  object tiff {
    case class ImageFraction(rows: List[ImageRow])
    case class ImageRow(pixels: List[RGB])
    case class RGB(r: Short, g: Short, b: Short)

    object syntax {
      implicit final class RGBSyntax(val byteBuffer: ByteBuffer) extends AnyVal {
        private def unsignedByte(byte: Byte): Short =
          (0x00ff & byte).toShort

        def getRGB(): Option[RGB] = {
          if (byteBuffer.remaining() < 3) {
            byteBuffer.get()
            None
          }
          else
            Some(RGB(unsignedByte(byteBuffer.get()), unsignedByte(byteBuffer.get()), unsignedByte(byteBuffer.get())))
        }
        def getPixels(): List[RGB] = {
          var listBuffer: ListBuffer[RGB] = ListBuffer.empty
          while (byteBuffer.remaining() > 0) {
            getRGB().foreach( rgb => listBuffer += rgb)
          }
          listBuffer.toList
        }

      }
    }
  }


  object eitherUtils {
    implicit final class EitherConcat[A, B](val listOfEithers: List[Either[A, B]]) extends AnyVal {
      def concat: Either[A, List[B]] = {
        val correctResults = listOfEithers.takeWhile(_.isRight).map(_.right.get)
        if (correctResults.size == listOfEithers.size)
          Right(correctResults)
        else {
          Left(listOfEithers.find(_.isLeft).get.left.get)
        }
      }
    }
  }

}
