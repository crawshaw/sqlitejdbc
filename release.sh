#!/bin/sh
#
# Compile and prepare for distribution Mac/Win/Java versions.
# Expects to run on Mac OS X with the DarwinPorts windows cross compiler.
#

sqlitejdbc="sqlitejdbc-v`cat VERSION`"

#
# pure java and source
#
echo '*** compiling pure java ***'
make dist/$sqlitejdbc-nested.tgz \
     dist/$sqlitejdbc-src.tgz

#
# universal binary
#
maclib=libsqlitejdbc.jnilib

echo '*** compiling for mac/ppc ***'
make os=Darwin arch=ppc native

echo '*** compiling for mac/i386 ***'
make os=Darwin arch=i386 native

echo '*** lipo ppc and i386 ***'
mkdir -p build/Darwin-universal
lipo -create build/Darwin-ppc/$maclib \
             build/Darwin-i386/$maclib \
     -output build/Darwin-universal/$maclib
mkdir -p dist
tar cfz dist/$sqlitejdbc-Mac.tgz README \
    -C build $sqlitejdbc-native.jar \
    -C Darwin-universal $maclib

#
# windows
#
echo '*** compiling for windows ***'
make os=Win arch=i386 dist/$sqlitejdbc-Win-i386.tgz

#
# build changes.html
#
cat > changes.html << EOF
<html>
<head>
<link rel="stylesheet" type="text/css" href="/content.css" />
<title>SQLiteJDBC - Changelog</title>
</head>
<body>
EOF
cat web/ad.inc >> changes.html
echo '<div class="content"><h1>Changelog</h1>' >> changes.html
cat web/nav.inc >> changes.html
echo '<h3>HEAD</h3><ul>' >> changes.html
# do not go back before version 008
sh -c 'darcs changes --from-patch="version 015"' | grep \* >> changes.html
perl -pi -e "s/^  \* version ([0-9]+)$/<\/ul><h3>Version \$1<\/h3><ul>/g" \
	changes.html
perl -pi -e "s/^  \* (.*)$$/<li>\$1<\/li>/g" changes.html
echo '</ul></div></body></html>' >> changes.html

#
# push release to web server
#
if [ "$1" = "elmo" ]; then
    cp dist/$sqlitejdbc-*.tgz \
        /var/www/zentus.com/www/sqlitejdbc/dist/
    cd /var/www/zentus.com/www/sqlitejdbc/src && darcs pull -a
    mv changes.html /var/www/zentus.com/www/sqlitejdbc/
    cp web/*.html web/*.css /var/www/zentus.com/www/sqlitejdbc/
fi
