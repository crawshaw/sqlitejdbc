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
