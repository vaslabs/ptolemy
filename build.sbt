
lazy val commonSettings = Seq(
  organization := "org.vaslabs",
  scalaVersion := "2.12.4"
)




lazy val imageProcessor = project.in(file("image-processing"))
  .settings(commonSettings: _*)
  .settings(
    name := "ptolemy-image-processor",
    libraryDependencies ++= Dependencies.imageProcessor
  ).enablePlugins(PartialUnification)


lazy val ptolemy = project.in(file("."))
  .settings(commonSettings: _*)
  .settings(
    name := "ptolemy"
  )
  .aggregate(imageProcessor)

