#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
mkdir -p build/classes
javac -encoding UTF-8 -d build/classes src/main/java/org/mse802/sdk/Main.java
jar --create --file build/mse802-quantum-sdk.jar --main-class org.mse802.sdk.Main -C build/classes .
echo "Built build/mse802-quantum-sdk.jar"
