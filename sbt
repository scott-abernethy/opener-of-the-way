#!/bin/bash
HOMEDIR=~
java -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005 -XX:MaxPermSize=786m -Xmx712M -Xss2M -XX:+CMSClassUnloadingEnabled -noverify -javaagent:$HOMEDIR/bin/jrebel/jrebel.jar -jar `dirname $0`/sbt-launcher.jar "$@"
