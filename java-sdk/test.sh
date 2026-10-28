#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
bash build.sh
mkdir -p build/test-classes
javac --add-modules jdk.httpserver -encoding UTF-8 -cp build/classes -d build/test-classes src/test/java/org/mse802/sdk/QuokkaRedirectTest.java
java --add-modules jdk.httpserver -cp build/classes:build/test-classes org.mse802.sdk.QuokkaRedirectTest
