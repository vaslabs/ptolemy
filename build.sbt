
lazy val commonSettings = Seq(
  organization := "org.vaslabs",
  scalaVersion := "2.12.4"
)




lazy val imageProcessor = project.in(file("image-processing"))
  .settings(commonSettings: _*)
  .settings(
    name := "ptolemy-image-processor",
    libraryDependencies ++= Dependencies.imageProcessor
  )

lazy val imageStore = project.in(file("store"))
    .settings(commonSettings: _*)
    .settings(
      name := "ptolemy-image-store",
      libraryDependencies ++= Dependencies.store
    ).dependsOn(imageProcessor)


lazy val ptolemy = project.in(file("."))
  .settings(commonSettings: _*)
  .settings(
    name := "ptolemy"
  )
  .aggregate(imageProcessor)

