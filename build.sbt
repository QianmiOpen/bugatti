import com.typesafe.config._

val conf = ConfigFactory.parseFile(new File("conf/application-common.conf")).resolve()

version := conf.getString("app.version")

name := "bugatti2"

libraryDependencies ++= Seq(
  jdbc,
  cache,
  "com.typesafe.akka" %% "akka-remote" % "2.2.2",
  "com.typesafe.akka" %% "akka-slf4j" % "2.2.2",
  "com.qianmi.bugatti" % "spirit_2.10" % "1.3.1",
  "mysql" % "mysql-connector-java" % "5.1.29",
  "com.typesafe.slick" %% "slick" % "2.0.1",
  "org.slf4j" % "slf4j-nop" % "1.6.4",
  "joda-time" % "joda-time" % "2.3",
  "org.joda" % "joda-convert" % "1.5",
  "com.github.tototoshi" %% "slick-joda-mapper" % "1.1.0",
  "commons-net" % "commons-net" % "3.3",
  "commons-io" % "commons-io" % "2.4",
  "org.pac4j" % "play-pac4j_scala" % "1.2.0",
  "org.pac4j" % "pac4j-cas" % "1.5.0",
  // WebJars pull in client-side web libraries
  "org.webjars" %% "webjars-play" % "2.2.1-2",
  "org.webjars" % "requirejs" % "2.1.11-1",
  "org.webjars" % "angularjs" % "1.2.20",
  "org.webjars" % "jquery" % "1.11.0",
  "org.webjars" % "bootstrap" % "3.1.1",
  "org.webjars" % "angular-ui-router" % "0.2.8-2",
  "org.webjars" % "angular-sanitize" % "1.2.15",
  "org.webjars" % "angular-ui-bootstrap" % "0.11.0-2",
  "org.webjars" % "angular-ui-tree" % "2.1.5",
  "org.webjars" % "angular-file-upload" % "1.6.1",
  "org.webjars" % "ace" % "07.31.2013",
  "org.webjars" % "angular-ui-ace" % "0.1.0",
  "org.webjars" % "jstree" % "3.0.1",
  "org.webjars" % "jsdifflib" % "0fcca118e",
  "org.webjars" % "datatables" % "1.10.0-beta-2",
  "org.eclipse.jgit" % "org.eclipse.jgit" % "3.4.1.201406201815-r",
  "org.yaml" % "snakeyaml" % "1.13"
)

resolvers ++= Seq(
  "Nexus repository" at "http://nexus.dev.ofpay.com/nexus/content/groups/public/",
  "JBoss repository" at "https://repository.jboss.org/nexus/content/repositories/",
  "Scala-Tools Maven2 Snapshots Repository" at "http://scala-tools.org/repo-snapshots"
)

play.Project.playScalaSettings
