import sbt._

class LiftProject(info: ProjectInfo) extends DefaultWebProject(info) with IdeaProject {
  val liftVersion = "2.2"

  // uncomment the following if you want to use the snapshot repo
  // val scalatoolsSnapshot = ScalaToolsSnapshots

  // If you're using JRebel for Lift development, uncomment
  // this line
  // override def scanDirectories = Nil

  override def libraryDependencies = Set(
    "net.liftweb" %% "lift-webkit" % liftVersion % "compile->default",
    "net.liftweb" %% "lift-record" % liftVersion % "compile->default",
    "org.squeryl" % "squeryl_2.8.1" % "0.9.4-RC3" % "compile->default",
    "org.mortbay.jetty" % "jetty" % "6.1.22" % "test->default",
    "junit" % "junit" % "4.5" % "test->default",
    "log4j" % "log4j" % "1.2.16",
    "org.slf4j" % "slf4j-log4j12" % "1.6.1",
    "org.scala-tools.testing" %% "specs" % "1.6.6" % "test->default",
    "org.mockito" % "mockito-all" % "1.8.5" % "test->default",
    "mysql" % "mysql-connector-java" % "5.1.9",
    "com.h2database" % "h2" % "1.2.138"
  ) ++ super.libraryDependencies
}
