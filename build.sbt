import com.typesafe.config._

val conf = ConfigFactory.parseFile(new File("conf/application-common.conf")).resolve()

version := conf.getString("app.version")

name := "bugatti2"

scalaVersion := "2.10.4"

libraryDependencies ++= {
  val akkaVersion = "2.3.8"
  Seq(
    jdbc,
    cache,
    "com.typesafe.akka" %% "akka-remote" % akkaVersion,
    "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
    "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test",
    "com.qianmi.bugatti" % "spirit_2.10" % "1.4.6",
    "mysql" % "mysql-connector-java" % "5.1.34",
    "com.typesafe.slick" %% "slick" % "2.1.0",
    "org.slf4j" % "slf4j-nop" % "1.6.4",
    "joda-time" % "joda-time" % "2.4",
    "org.joda" % "joda-convert" % "1.6",
    "com.github.tototoshi" %% "slick-joda-mapper" % "1.2.0",
    "commons-net" % "commons-net" % "3.3",
    "commons-io" % "commons-io" % "2.4",
    "org.pac4j" % "play-pac4j_scala2.10" % "1.3.0",
    "org.pac4j" % "pac4j-cas" % "1.6.0",
    // WebJars pull in client-side web libraries
    "org.webjars" %% "webjars-play" % "2.3.0-2",
    "org.webjars" % "bootstrap" % "3.1.1-2",
    "org.webjars" % "requirejs" % "2.1.15",
    "org.webjars" % "angularjs" % "1.2.28",
    "org.webjars" % "jquery" % "1.11.2",
    "org.webjars" % "angular-ui-router" % "0.2.13",
    "org.webjars" % "angular-sanitize" % "1.2.15",
    "org.webjars" % "angular-ui-bootstrap" % "0.12.0",
    "org.webjars" % "angular-ui-tree" % "2.1.5",
    "org.webjars" % "angular-file-upload" % "1.6.1",
    "org.webjars" % "ace" % "01.08.2014",
    "org.webjars" % "angular-ui-ace" % "0.1.0" exclude("org.webjars", "ace"),
    "org.webjars" % "jstree" % "3.0.1",
    "org.webjars" % "jsdifflib" % "0fcca118e",
    "org.webjars" % "datatables" % "1.10.0-beta-2",
    "org.webjars" % "angular-loading-bar" % "0.5.1",
    "org.webjars" % "angular-growl" % "0.4.0",
    "org.webjars" % "bindonce" % "0.3.1",
    "org.webjars" % "angular-chosen" % "1.0.6",
    "org.eclipse.jgit" % "org.eclipse.jgit" % "3.4.1.201406201815-r",
    "org.yaml" % "snakeyaml" % "1.13",
    "org.scalatest" % "scalatest_2.10" % "2.2.1" % "test"
  )
}

resolvers ++= Seq(
  "Nexus repository" at "http://nexus.dev.ofpay.com/nexus/content/groups/public/",
  "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/",
  "JBoss repository" at "https://repository.jboss.org/nexus/content/repositories/",
  "Scala-Tools Maven2 Snapshots Repository" at "http://scala-tools.org/repo-snapshots"
)

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalacOptions in ThisBuild ++= Seq("-feature")

