import sbt._

object Dependencies {

  object libs {
    object versions {
      object scrimage {
        val version = "2.1.8"
      }
    }
    object scrimage {
      import versions.scrimage.version
      val core = "com.sksamuel.scrimage" %% "scrimage-core" % version
      val extras =  "com.sksamuel.scrimage" %% "scrimage-io-extra" % version
      val filters = "com.sksamuel.scrimage" %% "scrimage-filters" % version
      val all = Seq(core, extras, filters)
    }

    object test {
      val scalatest = "org.scalatest" %% "scalatest" % "3.0.5" % Test
    }
  }

  lazy val scrimageDependencies = libs.scrimage.all ++ Seq(libs.test.scalatest)
}
