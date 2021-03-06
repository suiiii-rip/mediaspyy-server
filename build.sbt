import com.typesafe.sbt.packager.docker.DockerVersion

val versions = new {
  val zio = "1.0.4-2"
  val zioInteropCats = "2.2.0.1"
  val zioInteropReactivestreams = "1.3.0.7-2"
  val zioLogging = "0.5.4"
  val zioConfig = "1.0.0-RC31-1"
  val zioJson = "0.1"

  val http4s = "0.21.14"
  val logback = "1.2.3"
  val reactivestreams = "1.0.3"

  val mongo = "4.1.1"

  val commonsNet = "3.7.2"

  val scalatest = "3.2.3"
  val scalatic = "3.2.2"

  val scala = "2.13.4"
}

val dependencies = {
  import versions._
  new {
    val zio = "dev.zio" %% "zio" % versions.zio
    val `zio-streams` = "dev.zio" %% "zio-streams" % versions.zio
    val `zio-interop-cats` = "dev.zio" %% "zio-interop-cats" % zioInteropCats
    val `zio-interop-reactivestreams` = "dev.zio" %% "zio-interop-reactivestreams" % zioInteropReactivestreams

    val `zio-logging` = "dev.zio" %% "zio-logging" % zioLogging
    val `zio-logging-slf4j` = "dev.zio" %% "zio-logging-slf4j" % zioLogging

    val `zio-config` = "dev.zio" %% "zio-config" % zioConfig
    val `zio-config-magnolia` = "dev.zio" %% "zio-config-magnolia" % zioConfig

    val `zio-json` = "dev.zio" %% "zio-json" % zioJson

    val `http4s-core` = "org.http4s" %% "http4s-core" % http4s
    val `http4s-server` = "org.http4s" %% "http4s-server" % http4s
    val `http4s-blaze-server` = "org.http4s" %% "http4s-blaze-server" % http4s
    val `http4s-client` = "org.http4s" %% "http4s-client" % http4s
    val `http4s-blaze-client` = "org.http4s" %% "http4s-blaze-client" % http4s
    val `http4s-dsl` = "org.http4s" %% "http4s-dsl" % http4s

    val `bson` = "org.mongodb" % "bson" % mongo
    val `mongodb-driver-core` = "org.mongodb" % "mongodb-driver-core" % mongo
    val `mongo-scala-bson` = "org.mongodb.scala" %% "mongo-scala-bson" % mongo
    val `mongo-scala-driver` = "org.mongodb.scala" %% "mongo-scala-driver" % mongo

    val `commons-net` = "commons-net" % "commons-net" % commonsNet

    val `logback-classic` = "ch.qos.logback" % "logback-classic" % logback
    val `reactive-streams` = "org.reactivestreams" % "reactive-streams" % reactivestreams

    val scalatest = "org.scalatest" %% "scalatest" % versions.scalatest % "test"
    val scalatic = "org.scalactic" %% "scalactic" % versions.scalatic
  }
}

val commonSettings = Seq(
  organization := "rip.suiiii",
  version := "1.0.0-SNAPSHOT",
  scalaVersion := versions.scala,
  scalacOptions := Seq("-unchecked", "-deprecation", "-feature", "-encoding", "utf8"),
  dependencyOverrides ++= {
    import dependencies._
    Seq(
      scalatest,
    )
  },
  dockerVersion := Some(DockerVersion(19, 3, 13, Some("ce"))),
  dockerBaseImage := "registry.hub.docker.com/library/openjdk:11",
)

lazy val core = Project(
  id = "mediaspyy-core",
  base = file("core")
)
  .settings(
    libraryDependencies ++= {
      import dependencies._
      Seq(
        `logback-classic`,
      )
    },
    unusedCompileDependenciesFilter -= moduleFilter("ch.qos.logback", "logback-classic"),
  )
  .settings(commonSettings: _*)
  .enablePlugins(ReproducibleBuildsPlugin)


lazy val app = Project(
  id = "mediaspyy-app",
  base = file("app")
)
  .settings(
    libraryDependencies ++= {
      import dependencies._
      Seq(
        zio,
        `zio-streams`,
        `zio-interop-cats`,
        `zio-interop-reactivestreams`,
        `zio-logging`,
        `zio-logging-slf4j`,
        `zio-config`,
        `zio-config-magnolia`,
        `zio-json`,

        `http4s-core`,
        `http4s-server`,
        `http4s-blaze-server`,
        `http4s-client`,
        `http4s-blaze-client`,
        `http4s-dsl`,

        bson,
        `mongodb-driver-core`,
        `mongo-scala-bson`,
        `mongo-scala-driver`,

        `reactive-streams`,

        `commons-net`,
      )
    },
    undeclaredCompileDependenciesFilter -= moduleFilter("co.fs2", "fs2-core"),
    undeclaredCompileDependenciesFilter -= moduleFilter("com.chuusai", "shapeless"),
    undeclaredCompileDependenciesFilter -= moduleFilter("dev.zio", "izumi-reflect"),
    undeclaredCompileDependenciesFilter -= moduleFilter("org.typelevel", "cats-core"),
    undeclaredCompileDependenciesFilter -= moduleFilter("org.typelevel", "cats-effect"),
    // zio config
    undeclaredCompileDependenciesFilter -= moduleFilter("com.propensive", "magnolia"),
    undeclaredCompileDependenciesFilter -= moduleFilter("com.propensive", "mercator"),


    undeclaredCompileDependenciesFilter -= moduleFilter("io.chrisdavenport", "vault"),
    unusedCompileDependenciesFilter -= moduleFilter("ch.qos.logback", "logback-classic"),
    crossPaths := false,

    dockerExposedPorts ++= Seq(8080),
  )
  .settings(commonSettings: _*)
  .enablePlugins(ReproducibleBuildsPlugin, JavaAppPackaging, UniversalDeployPlugin, DockerPlugin)
  .dependsOn(core)

lazy val root = (project in file("."))
  .settings(commonSettings: _*)
  .aggregate(core, app)
