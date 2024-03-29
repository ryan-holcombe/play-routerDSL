import sbt._
import Keys._
import PlayProject._

object ApplicationBuild extends Build {

    val appName         = "playTest"
    val appVersion      = "1.0-SNAPSHOT"

    val appDependencies = Seq(
      "default" %% "play-routerdsl" % "0.1-SNAPSHOT"
    )

    val main = PlayProject(appName, appVersion, appDependencies, mainLang = SCALA).settings(
      // Add your own project settings here      
    )

}
