lazy val commonSettings = Seq(
  organization := "org.vaslabs",
  scalaVersion := "2.12.4",
  publishMavenStyle := true,
  //  resolvers += "Apache Staging" at "https://repository.apache.org/content/groups/staging/",
  scalacOptions in Compile ++= Seq(
    "-encoding", "UTF-8",
    "-target:jvm-1.8",
    "-feature",
    "-deprecation",
    "-unchecked",
    "-Xlint",
    "-Xfuture",
    "-Ywarn-dead-code",
    "-Ywarn-unused-import",
    "-Ywarn-unused",
    "-Ywarn-nullary-unit"
  ),
  scalacOptions in (Compile, doc) ++= Seq("-groups", "-implicits"),
  javacOptions in (Compile, doc) ++= Seq("-notimestamp", "-linksource"),
  autoAPIMappings := true,


  parallelExecution in Test := false,
  parallelExecution in IntegrationTest := true,

  publishArtifact in Test := false,

  pomExtra := <scm>
    <url>git@github.com:vaslabs/ptolemy.git</url>
    <connection>scm:git:git@github.com:vaslabs/ptolemy.git</connection>
  </scm>
    <developers>
      <developer>
        <id>vaslabs</id>
        <name>Vasilis Nicolaou</name>
        <url>http://git.vaslabs.org</url>
      </developer>
    </developers>,

  licenses := ("MIT", url("http://opensource.org/licenses/MIT")) :: Nil
)



lazy val imageProcessor = project.in(file("image-processing"))
    .settings(commonSettings: _*)
      .settings(
        name := "ptolemy-image-processor",
        libraryDependencies ++= Dependencies.scrimageDependencies
      )

lazy val ptolemy = project.in(file("."))
    .settings(commonSettings: _*)
      .settings(
        name := "ptolemy"
      )
      .aggregate(imageProcessor)

