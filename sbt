#!/bin/bash
JREBEL_HOME=/opt/JRebel
java -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005 -XX:MaxPermSize=786m -Xmx712M -Xss2M -XX:+CMSClassUnloadingEnabled -noverify -javaagent:$JREBEL_HOME/jrebel.jar -jar `dirname $0`/sbt-launcher.jar "$@"
