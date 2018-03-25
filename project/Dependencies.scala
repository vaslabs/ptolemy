import sbt._

object Dependencies {

  object libs {
    object versions {
      object scrimage {
        val version = "2.1.8"
      }
      object cats {
        val version = "0.9.0"
      }
      object circe {
        val version = "0.8.0"
      }
    }



    object circe {
      import versions.circe.version
      private val core = "io.circe" %% "circe-core" % version
      private val generic = "io.circe" %% "circe-generic" % version
      private val parser = "io.circe" %% "circe-parser" % version
      private val java8 = "io.circe" %% "circe-java8" % version
      val all = Seq(core, generic, parser, java8)
    }
    object scrimage {
      import versions.scrimage.version
      val core = "com.sksamuel.scrimage" %% "scrimage-core" % version
      val extras =  "com.sksamuel.scrimage" %% "scrimage-io-extra" % version
      val filters = "com.sksamuel.scrimage" %% "scrimage-filters" % version
      val all = Seq(core, extras, filters)
    }

    object cats {
      import versions.cats.version
      private val core = "org.typelevel" %% "cats-core" % version
      private val macros = "org.typelevel" %% "cats-macros" % version
      private val kernel = "org.typelevel" %% "cats-kernel" % version
      val required = Seq(core, macros, kernel)
    }

    object test {
      val scalatest = "org.scalatest" %% "scalatest" % "3.0.5" % Test
    }

    object akka {
      val actor = "com.typesafe.akka" %% "akka-actor" % "2.5.11"
      val testKit = "com.typesafe.akka" %% "akka-testkit" % "2.5.11" % Test
      val cluster = "com.typesafe.akka" %% "akka-cluster" % "2.5.11"
      val sharding = "com.typesafe.akka" %% "akka-cluster-sharding" %  "2.5.11"

      val all = Seq(actor, testKit, cluster, sharding)
    }
  }

  lazy val imageProcessor = libs.circe.all ++ Seq(libs.test.scalatest)

  lazy val store = libs.akka.all :+ libs.test.scalatest
}
