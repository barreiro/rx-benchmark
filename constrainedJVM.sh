#!/bin/sh
if [[ "x$JAVA_HOME" != "x" ]]; then
  taskset -c 0,2 $JAVA_HOME/bin/java "$@";
else
  taskset -c 0,2 java "$@";
fi 