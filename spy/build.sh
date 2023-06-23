#!/bin/bash -x
kotlinc -include-runtime -d spy.jar spy.kt
mv spy.jar ~/lib
rm *.class
rm -rf META-INF
