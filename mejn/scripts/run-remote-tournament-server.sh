# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

#!/bin/bash
# Launch the RemoteTournamentServer RMI server as a standalone JVM
# Reads host/port from rmi-default.properties

set -e
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$SCRIPT_DIR/../../.."
JAR_DIR="$PROJECT_ROOT/mejn/build/libs"
LIB_DIR="$JAR_DIR/lib"

# Find main jar
MAIN_JAR=$(ls "$JAR_DIR"/mejn-*.jar | head -n 1)
if [[ ! -f "$MAIN_JAR" ]]; then
  echo "Main jar not found in $JAR_DIR. Build the project first."
  exit 1
fi

# Build classpath: main jar + all jars in lib
CLASSPATH="$MAIN_JAR"
for dep in "$LIB_DIR"/*.jar; do
  CLASSPATH="$CLASSPATH:$dep"
done

# Launch
exec java -cp "$CLASSPATH" com.rttnghs.mejn.rmi.RemoteTournamentServer


