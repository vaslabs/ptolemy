package org.vaslabs.ptolemy.images

import java.io._
import java.nio.ByteBuffer

import model.errors._
import model.image._

object TiffReader {

  import TiffImplicits.implicits._
  import TiffImplicits.syntax._
  import eitherUtils._

  def readFromFile(fileLocation: String): Either[TiffEncodingError, TiffImage] = {
    val file = new File(fileLocation)
    var fileReader: RandomAccessFile = null
    try {
      fileReader = new RandomAccessFile(file, "r")
      val buffer = Array.ofDim[Byte](8)
      fileReader.read(buffer, 0, 8)
      val byteBuffer = ByteBuffer.wrap(buffer)
      for {
        imageHeader <- toTiffHeader(byteBuffer)
        offset = imageHeader.offset.value
        index = fileReader.seek(offset)
        ifdEntry = Array.ofDim[Byte](2)
        _ = fileReader.read(ifdEntry, 0, 2)
        ifdEntryBuffer = ByteBuffer.wrap(ifdEntry).order(imageHeader.byteOrder.asJava)
        numberOfIfds = ifdEntryBuffer.getShort()
        imageFileDirectoryEither: Either[TiffEncodingError, List[ImageFileDirectory]] =
            (1 to numberOfIfds).toList.map(_ =>
              {
                val arrayBuffer = Array.ofDim[Byte](12)
                fileReader.read(arrayBuffer, 0, 12)
                val byteBuffer = ByteBuffer.wrap(arrayBuffer).order(imageHeader.byteOrder.asJava)
                byteBuffer.asTiff[ImageFileDirectory]
              }).concat

        imageFileDirectory <- imageFileDirectoryEither

      } yield (TiffImage(imageHeader, imageFileDirectory, fileReader))
    }
  }

  def readOffset(byteOrder: ByteOrder, buffer: ByteBuffer): Either[TiffEncodingError, TiffOffset] = {
    val orderedByteBuffer = buffer.order(byteOrder.asJava)

    Right(TiffOffset(orderedByteBuffer.getInt))
  }

  private def toTiffHeader(buffer: ByteBuffer): Either[TiffEncodingError, TiffHeader] = {
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
