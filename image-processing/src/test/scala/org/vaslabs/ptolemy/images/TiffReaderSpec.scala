package org.vaslabs.ptolemy.images

import org.scalatest.{Matchers, WordSpec}

class TiffReaderSpec extends WordSpec with Matchers {

  "when reading tiff file" should {

    "read the tiff header" in {
      val tiffImage =
        TiffReader.readFromFile("/home/vnicolaou/Downloads/heic1502a.tif")
      tiffImage shouldBe (Right(TiffImage(TiffHeader(LittleIndian, TiffOffset(1816554936)))))
    }
  }

}
