package org.vaslabs.ptolemy.images

import java.io._
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.{ByteOrder => JByteOrder}

object TiffReader {


  def readFromFile(fileLocation: String): Either[TiffEncodingError, TiffImage] = {
    val file = new File(fileLocation)
    var fileReader: FileInputStream = null
    try {
      fileReader = new FileInputStream(file)
      val buffer = Array.ofDim[Byte](8)
      fileReader.read(buffer, 0, 8)
      buffer.foreach(b => println(s"${Byte.byte2int( b)}"))
      val byteBuffer = ByteBuffer.wrap(buffer)
      val imageHeader = toTiffHeader(byteBuffer)

      imageHeader.map(TiffImage)
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

case class TiffImage(header: TiffHeader)

sealed trait TiffEncodingError

object UnrecognisedByteOrderSignature extends TiffEncodingError

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