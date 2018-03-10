package org.vaslabs.ptolemy.images

import java.io._
import java.nio.ByteBuffer
import java.nio.{ByteOrder => JByteOrder}


object TiffReader {

  import TiffImplicits.implicits._
  import TiffImplicits.syntax._
  import eitherUtils._
  def readFromFile(fileLocation: String): Either[TiffEncodingError, TiffImage] = {
    val file = new File(fileLocation)
    var fileReader: FileInputStream = null
    try {
      fileReader = new FileInputStream(file)
      val buffer = Array.ofDim[Byte](8)
      fileReader.read(buffer, 0, 8)
      val byteBuffer = ByteBuffer.wrap(buffer)
      for {
        imageHeader <- toTiffHeader(byteBuffer)
        offset = imageHeader.offset.value
        index = fileReader.skip(offset - 8)
        ifdEntry = Array.ofDim[Byte](2)
        _ = fileReader.read(ifdEntry, 0, 2)
        ifdEntryBuffer = ByteBuffer.wrap(ifdEntry).order(imageHeader.byteOrder.asJava)
        numberOfIfds = ifdEntryBuffer.getShort()
        imageFileDirectoryEither: Either[TiffEncodingError, List[ImageFileDirectory]] = (1 to numberOfIfds).toList.map(_ => {
          val arrayBuffer = Array.ofDim[Byte](12)
          fileReader.read(arrayBuffer, 0, 12)
          val byteBuffer = ByteBuffer.wrap(arrayBuffer).order(imageHeader.byteOrder.asJava)
          byteBuffer.asTiff[ImageFileDirectory]
        }).concat

        imageFileDirectory <- imageFileDirectoryEither

      } yield (TiffImage(imageHeader, imageFileDirectory))
    }
    finally {
      if (fileReader != null)
        fileReader.close()
    }
  }

  def readOffset(byteOrder: ByteOrder, buffer: ByteBuffer):
  Either[TiffEncodingError, TiffOffset] =
  {
    val orderedByteBuffer  = buffer.order(byteOrder.asJava)

    Right(TiffOffset(orderedByteBuffer.getInt))
  }

  private def toTiffHeader(buffer: ByteBuffer):
  Either[TiffEncodingError, TiffHeader] =
  {
    for {
      byteOrder <- toByteOrder((buffer.get(), buffer.get()))
      tiffSignature <- tiffSignature(byteOrder, (buffer.get(), buffer.get()))
      offset <- readOffset(byteOrder, buffer)
    } yield (TiffHeader(byteOrder, offset))
  }

  private def toByteOrder(bytes: (Byte, Byte)): Either[TiffEncodingError, ByteOrder] = {
    bytes match {
      case ('I', 'I') =>
        Right(LittleIndian)
      case ('M', 'M') =>
        Right(BigIndian)
      case other =>
        Left(UnrecognisedByteOrderSignature)
    }
  }

  private def tiffSignature(byteOrder: ByteOrder, value: (Byte, Byte)): Either[TiffEncodingError, Boolean] = {
    val signature = byteOrder match {
      case LittleIndian => (value._2 << 8) | (value._1)
      case BigIndian => (value._1 << 8) | (value._2)
    }
    if (signature == 42)
      Right(true)
    else
      Left(NotSignedAsTiff(signature))
  }
}

case class TiffImage(header: TiffHeader, imageFileDirectories: Seq[ImageFileDirectory] = Seq.empty)

sealed trait TiffEncodingError

object UnrecognisedByteOrderSignature extends TiffEncodingError
case object FieldTagUnderflow extends TiffEncodingError
case object FieldTypeUnderflow extends TiffEncodingError
case object IntUnderflow extends TiffEncodingError

case class InvalidFieldType(signature: Short) extends TiffEncodingError

case class NotSignedAsTiff(unexpectedValue: Int) extends TiffEncodingError
case class UnexpectedHeaderSize(size: Int) extends TiffEncodingError

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

case class FieldTag(value: Short) extends AnyVal

object FieldTag {
  implicit val fieldTagOrdering = new Ordering[FieldTag] {
    override def compare(x: FieldTag, y: FieldTag): Int = x.value.compareTo(y.value)
  }
}

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
    implicit val fieldTagEncoder: TiffEncoder[FieldTag] = (byteBuffer) =>
      if (byteBuffer.remaining() < 2)
        Left(FieldTagUnderflow)
      else
        Right(FieldTag(byteBuffer.getShort()))

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
}