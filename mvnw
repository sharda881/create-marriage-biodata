#!/bin/sh
# Maven Wrapper script

# Resolve links: $0 may be a link
PRG="$0"
while [ -h "$PRG" ] ; do
  ls=`ls -ld "$PRG"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '/.*' > /dev/null; then
    PRG="$link"
  else
    PRG=`dirname "$PRG"`/"$link"
  fi
done
SAVED="`pwd`"
cd "`dirname \"$PRG\"`/" >/dev/null
APP_HOME="`pwd -P`"
cd "$SAVED" >/dev/null

APP_NAME="Maven Wrapper"
APP_BASE_NAME=`basename "$0"`

# Add default JVM options here
DEFAULT_JVM_OPTS=""

# Download maven wrapper if not present
if [ ! -f "$APP_HOME/.mvn/wrapper/maven-wrapper.jar" ]; then
    echo "Downloading Maven Wrapper..."
    if command -v curl > /dev/null 2>&1 ; then
        curl -sL -o "$APP_HOME/.mvn/wrapper/maven-wrapper.jar" "https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar"
    elif command -v wget > /dev/null 2>&1 ; then
        wget -q -O "$APP_HOME/.mvn/wrapper/maven-wrapper.jar" "https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar"
    else
        echo "Error: Please install curl or wget to download Maven Wrapper"
        exit 1
    fi
fi

WRAPPER_JAR="$APP_HOME/.mvn/wrapper/maven-wrapper.jar"
WRAPPER_PROPERTIES="$APP_HOME/.mvn/wrapper/maven-wrapper.properties"

# Determine Java command
if [ -n "$JAVA_HOME" ] ; then
    JAVACMD="$JAVA_HOME/bin/java"
else
    JAVACMD="java"
fi

# Execute Maven Wrapper
exec "$JAVACMD" $DEFAULT_JVM_OPTS $MAVEN_OPTS -classpath "$WRAPPER_JAR" org.apache.maven.wrapper.MavenWrapperMain "$@"
