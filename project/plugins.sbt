// Comment to get more information during initialization
logLevel := Level.Warn

// The Typesafe repository
resolvers ++= Seq(
  "Nexus repository" at "http://nexus.dev.ofpay.com/nexus/content/groups/public/",
  "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/",
  "JBoss repository" at "https://repository.jboss.org/nexus/content/repositories/",
  "Scala-Tools Maven2 Snapshots Repository" at "http://scala-tools.org/repo-snapshots"
)

// Use the Play sbt plugin for Play projects
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.3.7")
