import com.gu.deploy.PlayArtifact._
import sbt._
import sbt.Keys._
import play.Play.autoImport._
import PlayKeys._
import com.typesafe.sbt.web._
import com.typesafe.sbt.web.Import._
import Dependencies._
import com.typesafe.sbt.gzip.Import._

object WorkflowBuild extends Build {

  val commonSettings =
    Seq(
      scalaVersion := "2.10.4",
      scalaVersion in ThisBuild := "2.10.4",
      organization := "com.gu",
      version      := "0.1",
      fork in Test := false,
      resolvers ++= Seq("Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"),
      scalacOptions ++= Seq("-feature", "-deprecation", "-language:higherKinds", "-Xfatal-warnings"),
      doc in Compile <<= target.map(_ / "none"),
      incOptions := incOptions.value.withNameHashing(nameHashing = true)
    )

  lazy val commonLib = project("common-lib")
    .settings(
      libraryDependencies ++= databaseDependencies
    )

  lazy val prole = playProject("prole")
                  .settings(libraryDependencies ++= awsDependencies ++ testDependencies)
                  .settings(playDefaultPort := 9091)

  val sass = taskKey[Seq[File]]("Compiles Sass files")
  val sassOutputStyle = settingKey[String]("CSS output style (nested|expanded|compact|compressed)")

  lazy val root = playProject("prototype")
    .settings(
      libraryDependencies ++= databaseDependencies ++ akkaDependencies ++ awsDependencies ++ googleOAuthDependencies
    )
    .settings(
      sassOutputStyle := "compressed",
      sass := {
        val log = streams.value.log

        val sourceDir = (resourceDirectory in Assets).value
        val sourceFiles = ((sourceDir ** "*.scss") --- (sourceDir ** "_*.scss")).get


        log.info("Compiling " + sourceFiles.length + " Sass sources...")

        // node-sass cli for nodejs lib-sass wrapper
        val sassCmd = baseDirectory(_ / "node_modules/.bin/node-sass").value

        sourceFiles.flatMap(src => {
          val dest = src.getParentFile / src.base + ".min.css"
          log.info("Compiling Sass source: " + src.toString)

          // sourcemap arg has to go at end
          Seq(sassCmd.toString, "--include-path", sourceDir.toString, "--output-style", sassOutputStyle.value, src.toString, dest, "--source-map").!!(log)

          // return sequence of files generated
          Seq(file(dest), file(dest + ".map"))
        })
      },
      sourceGenerators in Assets <+= sass
    )
    .settings(
      includeFilter in gzip := "*.html" || "*.css" || "*.js",
      pipelineStages := Seq(gzip)
    ).settings(playDefaultPort := 9090)

  def project(path: String): Project =
    Project(path, file(path)).settings(commonSettings: _*)

  def playProject(path: String): Project =
    Project(path, file(path)).enablePlugins(play.PlayScala).enablePlugins(SbtWeb)
      .settings(libraryDependencies += ws)
      .settings(commonSettings ++ playArtifactDistSettings ++ playArtifactSettings: _*)
      .settings(magentaPackageName := path)
      .dependsOn(commonLib)

  def playArtifactSettings = Seq(
    ivyXML :=
      <dependencies>
        <exclude org="commons-logging"/>
        <exclude org="org.springframework"/>
        <exclude org="org.scala-tools.sbt"/>
      </dependencies>
  )

}
