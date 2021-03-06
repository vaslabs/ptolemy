package org.vaslabs.ptolemy.images
import java.io.RandomAccessFile
import java.nio.ByteBuffer

import model.fieldTypes._
import model.errors._
import model.tags._
import model.image._
import org.vaslabs.ptolemy.images.model.planarConfiguration.{Chunky, Configuration, Planar}
import org.vaslabs.ptolemy.images.model.tiffDataReader.Strip
import org.vaslabs.ptolemy.images.tiff.{ImageFraction, ImageRow, RGB}
object TiffImplicits {

  private object FieldTypeOrder {
    val order = Array(
      TiffByte, TiffAscii, TiffShort,
      TiffLong, TiffRational,
      TiffSignedByte, TiffUndefined,
      TiffSignedShort, TiffSignedLong,
      TiffSignedRational, TiffFloat,
      TiffDouble)
  }

  trait TiffEncoder[A] {
    def encode(byteBuffer: ByteBuffer): Either[TiffEncodingError, A]
  }

  object implicits {
    implicit val shortEncoder: TiffEncoder[Short] = (byteBuffer) =>
      if (byteBuffer.remaining() < 2)
        Left(ShortUnderflow)
      else
        Right(byteBuffer.getShort())

    implicit val fieldTagEncoder: TiffEncoder[FieldTag] = (byteBuffer) =>
      shortEncoder.encode(byteBuffer).left.map(_ => FieldTypeUnderflow).flatMap(short => {
        val value = Integer.toUnsignedLong(0x0000ffff & short).toInt
        Right(FieldTag.encoding.get(value).getOrElse(GenericTag(value)))
      })

    implicit val fieldTypeEncoder: TiffEncoder[TiffFieldType] = (byteBuffer) =>
      if (byteBuffer.remaining() < 2)
        Left(FieldTypeUnderflow)
      else {
        val value = byteBuffer.getShort()
        if (value >= FieldTypeOrder.order.size || value <= 0)
          Left(InvalidFieldType(value))
        else
          Right(FieldTypeOrder.order(value - 1))
      }
    implicit val intEncoder: TiffEncoder[Int] = (byteBuffer) =>
      if (byteBuffer.remaining() < 4)
        Left(IntUnderflow)
      else
        Right(byteBuffer.getInt())

    implicit val valueOffsetEncoder: TiffEncoder[ValueOffset] =
      (byteBuffer) => intEncoder.encode(byteBuffer).map(ValueOffset)

    implicit val imageFileDirectoryEncoder: TiffEncoder[ImageFileDirectory] = (byteBuffer) => {
      import syntax._
      for {
        fieldTag <- byteBuffer.asTiff[FieldTag]
        fieldType <- byteBuffer.asTiff[TiffFieldType]
        numberOfValues <- byteBuffer.asTiff[Int]
        valueOffset <- byteBuffer.asTiff[ValueOffset]
      } yield (ImageFileDirectory(fieldTag, fieldType, numberOfValues, valueOffset))
    }

  }

  object syntax {

    implicit final class TiffAdapter(val byteBuffer: ByteBuffer) extends AnyVal {
      def asTiff[A](implicit encoder: TiffEncoder[A]): Either[TiffEncodingError, A] =
        encoder.encode(byteBuffer)
    }

  }

  object imageCalc {
    object syntax {
      implicit final class InfoExtractor(val tiffImage: TiffImage) extends AnyVal {
        def stripsPerImage(): Option[Int] = {
          for {
            imageLength <- tiffImage.imageFileDirectories.find(_.fieldTag == ImageLength).map(_.valueOffset.value.toDouble)
            rowsPerStrip <- tiffImage.imageFileDirectories.find(_.fieldTag == RowsPerStrip).map(_.valueOffset.value.toDouble)
          } yield Math.floor((imageLength + rowsPerStrip - 1)/rowsPerStrip).toInt
        }

        def samplesPerPixel(): Option[Int] = {
          tiffImage.imageFileDirectories.find(_.fieldTag == SamplesPerPixel)
              .map(_.valueOffset.value)
        }

        private def stripByteCounts(configuration: Configuration): Option[Int] = {
          stripsPerImage().flatMap {
            spi =>
              configuration match {
                case Chunky =>
                  Some(spi)
                case Planar =>
                  samplesPerPixel().map(_ * spi)
              }
          }
        }

        def byteOffsetOfStrip(): Option[Int] = {
          import model.planarConfiguration.syntax._
          for {
            stripOffsets <- tiffImage.imageFileDirectories.find(_.fieldTag == StripOffsets)
            planarConfiguration <- tiffImage.planarType()
            stripByteCounts <- stripByteCounts(planarConfiguration)
          } yield(stripByteCounts)
        }

        def imageFraction(strip: Strip): Option[ImageFraction] = {
          val bytes: Array[Byte] = tiffImage.readData(strip)
          val buffer = ByteBuffer.wrap(bytes).order(tiffImage.header.byteOrder.asJava)
          import tiff.syntax._
          val rgbData: List[RGB] = buffer.getPixels()
          for {
            numberOfRows <- strip.numberOfRows
            pixelsPerRow = rgbData.size / numberOfRows
            _ = println(s"number of rows: $numberOfRows with pixels per row: $pixelsPerRow")
          } yield (ImageFraction(rgbData.grouped(pixelsPerRow).map(ImageRow).toList))
        }
      }


    }
  }

}