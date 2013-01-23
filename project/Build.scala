import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName         = "java-with-freemarker"
  val appVersion      = "1.0-SNAPSHOT"

  val appDependencies = Seq(
    javaCore,
    "org.freemarker" % "freemarker" % "2.3.19"
  )

  val main = play.Project(appName, appVersion, appDependencies).settings(
    
    unmanagedResources in Compile <<= (
      javaSource in Compile, 
      classDirectory in Compile, 
      unmanagedResources in Compile
    ) map { (app, classes, resources) =>
      IO.copyDirectory(app / "views", classes / "views", overwrite = true)
      resources
    }

  )

}
