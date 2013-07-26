import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName         = "opener-of-the-way"
  val appVersion      = "0.7.1"

  val appDependencies = Seq(
    jdbc,
    // Add your project dependencies here,
    //"com.typesafe.akka" % "akka-testkit_2.10.0-RC1" % "2.1.0-RC1",
    "org.squeryl" % "squeryl_2.10" % "0.9.5-6" % "compile->default",
    "org.mockito" % "mockito-all" % "1.8.5" % "test->default",
    "mysql" % "mysql-connector-java" % "5.1.21",
    "com.h2database" % "h2" % "1.2.147",
    "org.mindrot" % "jbcrypt" % "0.3m"
  )

  val main = play.Project(appName, appVersion, appDependencies).settings(
    // Add your own project settings here    
    scalacOptions ++= Seq("-feature")
  )

}
