#!/usr/bin/env bash
# Compila el APK de JH VIDEO (desde la carpeta jh-video/).
# Gradle (tarea copyApkToRoot) copia solo el APK a jh-video/ como JH-VIDEO-v<versión>.apk
# y borra el de la versión anterior, así que aquí no hace falta copiarlo a mano.
set -e
export JAVA_HOME="/c/Program Files/Java/jdk-11.0.30"
export PATH="$JAVA_HOME/bin:$PATH"
echo "Usando Java: $($JAVA_HOME/bin/java -version 2>&1 | head -n 1)"
cd android
./gradlew clean assembleDebug
cd ..
echo "BUILD EXITOSO! APK generado en: $(pwd)/$(ls JH-VIDEO-v*.apk)"
