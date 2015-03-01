#!/bin/sh
VERSION="1.0.1"
cp essem-reporter-1.0.pom dist_reporter/lib/essem-reporter-${VERSION}.pom
cd dist_reporter/lib
gpg -ab essem-reporter-${VERSION}.pom
gpg -ab essem-reporter-${VERSION}.jar
gpg -ab essem-reporter-${VERSION}-sources.jar
gpg -ab essem-reporter-${VERSION}-javadoc.jar
jar -cvf ../bundle.jar *