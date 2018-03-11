package org.vaslabs.ptolemy.images

import org.scalatest.{Matchers, WordSpec}
import model.image._

class TiffReaderSpec extends WordSpec with Matchers {

  "when reading tiff file" should {

    "read the tiff header" in {
      val tiffImage =
        TiffReader.readFromFile("/home/vnicolaou/Downloads/heic1502a.tif")
      tiffImage should matchPattern {
        case (Right(TiffImage(TiffHeader(LittleIndian, TiffOffset(1816554936)), list, _))) if list.size == 24 =>
      }

      println(tiffImage.right.get.header)
      val resultImage = tiffImage.right.get
      resultImage.imageFileDirectories.foreach(println)
      resultImage.dimensions shouldBe Dimensions(40000, 12788)
      resultImage.description shouldBe "This image, captured with the NASA/ESA Hubble Space Telescope, is the largest and sharpest image ever taken of the Andromeda galaxy — otherwise known as M31. This is a cropped version of the full image and has 1.5 billion pixels. You would need more than 600 HD television screens to display the whole image. It is the biggest Hubble image ever released and shows over 100 million stars and thousands of star clusters embedded in a section of the galaxy’s pancake-shaped disc stretching across over 40 000 light-years. This image is too large to be easily displayed at full resolution and is best appreciated using the zoom tool."
      import model.colorModes.syntax._
      resultImage.intepret shouldBe Some(model.colorModes.RGB)
      import TiffImplicits.imageCalc.syntax._
      resultImage.stripsPerImage() shouldBe Some(6394)

    }
  }

}
