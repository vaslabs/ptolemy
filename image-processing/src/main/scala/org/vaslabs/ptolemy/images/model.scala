package org.vaslabs.ptolemy.images

import java.io.{FileInputStream, FileReader, RandomAccessFile}
import java.nio.charset.Charset

import org.vaslabs.ptolemy.images.model.fieldTypes.{TiffFieldType, ValueOffset}
import org.vaslabs.ptolemy.images.model.tags._
import java.nio.{ByteBuffer, ByteOrder => JByteOrder}

import org.vaslabs.ptolemy.images.model.image.{ImageFileDirectory, TiffImage}

object model {

  object errors {
    sealed trait TiffEncodingError

    object UnrecognisedByteOrderSignature extends TiffEncodingError

    case object FieldTagUnderflow extends TiffEncodingError
    case class UnrecognisedFieldTag(value: Int) extends TiffEncodingError

    case object FieldTypeUnderflow extends TiffEncodingError

    case object IntUnderflow extends TiffEncodingError
    case object ShortUnderflow extends TiffEncodingError

    case class InvalidFieldType(signature: Short) extends TiffEncodingError

    case class NotSignedAsTiff(unexpectedValue: Int) extends TiffEncodingError

    case class UnexpectedHeaderSize(size: Int) extends TiffEncodingError
  }

  object tags {
    sealed trait FieldTag {
      val value: Int
    }

    case object NewSubfileType extends FieldTag {
      val value = 254
    }
    case object ImageWidth extends FieldTag {
      val value = 256
    }
    case object ImageLength extends FieldTag {
      val value = 257
    }
    case object BitsPerSample extends FieldTag {
      val value = 258
    }
    case object Compression extends FieldTag {
      val value = 259
    }
    case object ImageDescription extends FieldTag {
      val value = 270
    }
    case object PhotometricInterpretation extends FieldTag {
      val value = 262
    }
    case object StripOffsets extends FieldTag {
      val value = 273
    }
    case object Orientation extends FieldTag {
      val value = 274
    }
    case object SamplesPerPixel extends FieldTag {
      val value = 277
    }
    case object RowsPerStrip extends FieldTag {
      val value = 278
    }
    case object StripByteCounts extends FieldTag {
      val value = 279
    }
    case object XResolution extends FieldTag {
      val value = 282
    }
    case object YResolution extends FieldTag {
      val value = 283
    }
    case object PlanarConfiguration extends FieldTag {
      val value = 284
    }
    case object ResolutionUnit extends FieldTag {
      val value = 296
    }
    case object Software extends FieldTag {
      val value = 305
    }
    case object DateTime extends FieldTag {
      val value = 306
    }
    case object Predictor extends FieldTag {
      val value = 317
    }


    case class GenericTag(value: Int) extends FieldTag

    object FieldTag {
      implicit val fieldTagOrdering = new Ordering[FieldTag] {
        override def compare(x: FieldTag, y: FieldTag): Int = x.value.compareTo(y.value)
      }
      val encoding: Map[Int, FieldTag] = Map(
        NewSubfileType.value -> NewSubfileType,
        ImageWidth.value -> ImageWidth,
        ImageLength.value -> ImageLength,
        BitsPerSample.value -> BitsPerSample,
        Compression.value -> Compression,
        ImageDescription.value -> ImageDescription,
        PhotometricInterpretation.value -> PhotometricInterpretation,
        StripOffsets.value -> StripOffsets,
        Orientation.value -> Orientation,
        SamplesPerPixel.value -> SamplesPerPixel,
        RowsPerStrip.value -> RowsPerStrip,
        StripByteCounts.value -> StripByteCounts,
        XResolution.value -> XResolution,
        YResolution.value -> YResolution,
        PlanarConfiguration.value -> PlanarConfiguration,
        ResolutionUnit.value -> ResolutionUnit,
        Software.value -> Software,
        DateTime.value -> DateTime,
        Predictor.value -> Predictor
      )
    }

  }


  object fieldTypes {
    sealed trait TiffFieldType

    case object TiffByte extends TiffFieldType

    case object TiffAscii extends TiffFieldType

    case object TiffShort extends TiffFieldType

    case object TiffLong extends TiffFieldType

    case object TiffRational extends TiffFieldType

    case object TiffSignedByte extends TiffFieldType

    case object TiffUndefined extends TiffFieldType

    case object TiffSignedShort extends TiffFieldType

    case object TiffSignedLong extends TiffFieldType

    case object TiffSignedRational extends TiffFieldType

    case object TiffFloat extends TiffFieldType

    case object TiffDouble extends TiffFieldType

    case class ValueOffset(val value: Int) extends AnyVal
  }

  object image {


    case class TiffImage(header: TiffHeader, imageFileDirectories: Seq[ImageFileDirectory] = Seq.empty,
               private val fileReader: RandomAccessFile) {

      protected lazy val width: Int =
        imageFileDirectories.find(_.fieldTag == ImageWidth).map(_.valueOffset.value).getOrElse(0)

      protected lazy val length: Int =
        imageFileDirectories.find(_.fieldTag == ImageLength).map(_.valueOffset.value).getOrElse(0)

      lazy val description = imageFileDirectories.find(_.fieldTag == ImageDescription).map(tag => {
        fetchDescription(tag)
      }).getOrElse("No description")

      lazy val dimensions: Dimensions = Dimensions(width, length)

      private[this] def fetchDescription(imageDescription: ImageFileDirectory): String = {
        import fieldTypes._
        fileReader.seek(imageDescription.valueOffset.value)
        imageDescription.fieldType match {
          case TiffAscii =>
            val sizeInBytes = imageDescription.numberOfValues - 1
            val bytes = Array.ofDim[Byte](sizeInBytes)
            val _ = fileReader.read(bytes, 0, sizeInBytes)
            val byteBuffer = ByteBuffer.wrap(bytes).order(header.byteOrder.asJava)
            val byteArray = byteBuffer.array()
            new String(byteArray, Charset.forName("UTF-8"))
        }
      }

    }
    case class Dimensions(width: Int, length: Int)


    sealed trait ByteOrder {
      def asJava: JByteOrder
    }

    case object LittleIndian extends ByteOrder {
      def asJava = JByteOrder.LITTLE_ENDIAN
    }

    case object BigIndian extends ByteOrder {
      def asJava = JByteOrder.BIG_ENDIAN
    }

    case class TiffHeader(
                           byteOrder: ByteOrder,
                           offset: TiffOffset
                         )

    case class TiffOffset(val value: Int) extends AnyVal

    case class ImageFileDirectory(
                                   fieldTag: FieldTag, fieldType: TiffFieldType, numberOfValues: Int, valueOffset: ValueOffset
                                 )



  }

  object planarConfiguration {

    sealed trait Configuration

    case object Chunky extends Configuration

    case object Planar extends Configuration

    object syntax {

      implicit final class PlanarTranslator(val image: TiffImage) extends AnyVal {
        def planarType(): Option[Configuration] = {
          val configValue: Option[Int] = for {
            planarConfiguration <- image.imageFileDirectories.find(_.fieldTag == PlanarConfiguration)
          } yield (planarConfiguration.valueOffset.value)
          configValue.flatMap {
            case 1 => Some(Chunky)
            case 2 => Some(Planar)
            case other => None
          }
        }
      }

    }

  }

  object colorModes {
    sealed trait PhotometricIntepretationMode

    case object WhiteIs0 extends PhotometricIntepretationMode
    case object BlackIs0 extends PhotometricIntepretationMode
    case object RGB extends PhotometricIntepretationMode

    object syntax {
      implicit final class IntepretColorMode(val image: TiffImage) extends AnyVal {
        private def intepret(imageFileDirectory: ImageFileDirectory): Option[PhotometricIntepretationMode]
            = imageFileDirectory match {
          case ImageFileDirectory(PhotometricInterpretation, _, _, offset) =>
            offset.value match {
              case 0 => Some(WhiteIs0)
              case 1 => Some(BlackIs0)
              case 2 => Some(RGB)
              case other => None
            }
          case other => None
        }
        def intepret: Option[PhotometricIntepretationMode] =
          image.imageFileDirectories.find(_.fieldTag == PhotometricInterpretation)
              .flatMap(fd => intepret(fd))
      }
    }
  }

  object tiffDataReader {

    class Strip protected (
        image: TiffImage) {



      lazy val data: Iterable[Strip] =
        new StripIterable(image)
    }

    class StripIterable(
          tiffImage: TiffImage)
        extends Iterable[Strip]{

      import TiffImplicits.imageCalc.syntax._
      val stripOffsets = tiffImage.imageFileDirectories.find(_.fieldTag == StripOffsets)
      val stripsPerImage = tiffImage.stripsPerImage()

      override def iterator: Iterator[Strip] = new Iterator[Strip] {

        override def hasNext: Boolean = ???

        override def next(): Strip = ???
      }
    }

  }

}
