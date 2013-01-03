import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName         = "opener-of-the-way"
  val appVersion      = "1.0-SNAPSHOT"

  val appDependencies = Seq(
    // Add your project dependencies here,
    //"com.typesafe.akka" % "akka-testkit_2.10.0-RC1" % "2.1.0-RC1",
    "org.squeryl" % "squeryl_2.10.0-RC1" % "0.9.5-4" % "compile->default",
    "org.mockito" % "mockito-all" % "1.8.5" % "test->default",
    "c3p0" % "c3p0" % "0.9.1.2",
    "mysql" % "mysql-connector-java" % "5.1.21",
    //"log4j" % "log4j" % "1.2.16",
    "com.h2database" % "h2" % "1.2.147"
  )

  val main = play.Project(appName, appVersion, appDependencies).settings(
    // Add your own project settings here      
  )

}
