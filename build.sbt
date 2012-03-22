name := "play-routerDSL"

scalaVersion := "2.9.1"

resolvers ++= Seq(
    DefaultMavenRepository,
    Resolver.url("Play", url("http://download.playframework.org/ivy-releases/"))(Resolver.ivyStylePatterns),
    "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
    "Typesafe Other Repository" at "http://repo.typesafe.com/typesafe/repo/",
    "sbt-idea-repo" at "http://mpeltonen.github.com/maven/",
    Resolver.url("sbt-plugin-releases", url("http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-releases/"))(Resolver.ivyStylePatterns),
    "gseitz@github" at "http://gseitz.github.com/maven/"
)


// Dependencies

libraryDependencies ++= Seq(
    "play" %% "play" % "2.0"
)

// Test dependencies

libraryDependencies ++= Seq(
    "play" %% "play-test" % "2.0" % "test"
)
