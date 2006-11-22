# Makefile for SQLite JDBC Driver 
# Author: David Crawshaw 2006
# Released under New BSD License (see LICENSE file).
#
# No auto-goop. Just try typing 'make'. See README for more.
#

os := default

ifeq ($(arch),)
arch := $(shell uname -m)
endif

ifeq ($(arch),Power Macintosh) # OS X gives funny result for 'uname -m'
arch := powerpc
endif

ifndef JAVA_HOME
$(error Set env variable JAVA_HOME)
endif

jni_md := $(shell find $(JAVA_HOME) -name jni_md.h)
ifneq ($(jni_md),)
jni_include := $(shell dirname $(jni_md))
endif

libs := $(subst  ,:,$(wildcard lib/*.jar))

java_sources := $(wildcard src/org/sqlite/*.java)
java_classes := $(java_sources:src/%.java=build/%.class)
native_classes := $(filter-out %NestedDB.class,$(java_classes))
nested_classes := $(filter-out %NativeDB.class,$(java_classes))
test_sources := $(wildcard src/test/*.java)
test_classes := $(test_sources:src/%.java=build/%.class)
tests        := $(subst /,.,$(patsubst build/%.class,%,$(test_classes)))

nestedvm_version := 2006-11-19
nestedvm := nestedvm-$(nestedvm_version)

sqlite_version := 3.3.8
sqlite := sqlite-$(sqlite_version)

target    := $(os)-$(arch)
VERSION   := $(shell cat VERSION)
PNAME     := sqlitejdbc-v$(VERSION)
CC        := gcc
CFLAGS   := -I$(JAVA_HOME)/include -I$(jni_include) -O -fPIC \
	-Iupstream/$(sqlite)-$(target) -Ibuild

ifeq ($(shell uname),Darwin)
	STRIP     := strip -x
	LINKFLAGS := -dynamiclib
	LIBNAME   := libsqlitejdbc.jnilib
	LIPO      := lipo
else
# TODO: sqlitejdbc.dll for cygwin
	STRIP     := strip
	LINKFLAGS := -shared
	LIBNAME   := libsqlitejdbc.so
endif


default: test-nested

upstream/%:
	$(MAKE) -C upstream $*

build/org/%.class: src/org/%.java
	@mkdir -p build
	javac -source 1.2 -target 1.2 -cp upstream/$(nestedvm)/build \
	    -sourcepath src -d build $<

build/test/%.class: src/test/%.java
	@mkdir -p build
	javac -target 1.5 -cp $(libs):build -sourcepath src/test -d build $<

native: upstream/$(sqlite)-$(target)/main.o $(native_classes)
	@mkdir -p build/$(target)
	javah -classpath build -jni -o build/NativeDB.h org.sqlite.NativeDB
	jar cf build/sqlitejdbc-native.jar -C build org
	$(CC) $(CFLAGS) -c -O -o build/$(target)/NativeDB.o \
		src/org/sqlite/NativeDB.c
	$(CC) $(CFLAGS) $(LINKFLAGS) -o build/$(target)/$(LIBNAME) \
		build/$(target)/NativeDB.o work/sqlite-$(version)/$(target)/*.o
	$(STRIP) build/$(target)/$(LIBNAME)

nested: upstream/build/org/sqlite/SQLite.class $(nested_classes)

dist/$(PNAME)-$(target).tgz:
	@mkdir -p dist
	tar cfz dist/$(PNAME)-$(target).tgz README \
	    -C build sqlitejdbc.jar -C $(target) $(LIBNAME)

test-native: native $(test_classes)
	java -Djava.library.path=build/$(target) -cp build:$(libs) \
	    org.junit.runner.JUnitCore $(tests)

test-nested: nested $(test_classes)
	java -cp build:upstream/build:upstream/$(nestedvm)/build:$(libs) \
	    org.junit.runner.JUnitCore \
		test.ConnectionTest \
		test.StatementTest \
		test.PrepStmtTest
		#$(tests)

clean:
	rm -rf build
	rm -rf dist

distclean: clean upstream/clean

lipo:
	# TODO
	$(LIPO) -create \
	    build/Darwin-powerpc/libsqlitejdbc.jnilib \
	    build/Darwin-i386/libsqlitejdbc.jnilib \
	    -output build/Darwin-lipo/libsqlitejdbc.jnilib

dist/$(PNAME)-src.tgz:
	@mkdir -p dist
	@mkdir -p work/$(PNAME)/src
	cp Makefile work/$(PNAME)/.
	cp README work/$(PNAME)/.
	cp LICENSE work/$(PNAME)/.
	cp VERSION work/$(PNAME)/.
	cp -R src/org work/$(PNAME)/src/.
	cp -R src/test work/$(PNAME)/src/.
	cp -R lib work/$(PNAME)/
	cd work && tar cfz ../dist/$(PNAME)-src.tgz $(PNAME)


doc:
	mkdir -p build/doc
	javadoc -notree -d build/doc src/org/sqlite/*.java

changes:
	echo '<html><head>' > changes.html
	echo '<link rel="stylesheet" type="text/css" href="/content.css" />' \
		>> changes.html
	echo '<title>SQLiteJDBC - Changelog</title></head><body>' >> changes.html
	cat web/ad.inc >> changes.html
	echo '<div class="content">' >> changes.html
	echo '<h1>Changelog</h1>' >> changes.html
	cat web/nav.inc >> changes.html
	echo '<h3>HEAD</h3><ul>' >> changes.html
	darcs changes --from-patch="version 008" | grep \* >> changes.html
	perl -pi -e \
		"s/^  \* version ([0-9]+)\$$/<\/ul><h3>Version \$$1<\/h3><ul>/g" \
		changes.html
	perl -pi -e "s/^  \* (.*)$$/<li>\$$1<\/li>/g" changes.html
	echo '</ul></div></body></html>' >> changes.html

### only useful on the author's web server
release:
	make changes
	cp dist/sqlitejdbc-v$(VERSION)-*.tgz \
		/var/www/zentus.com/www/sqlitejdbc/dist/
	cd /var/www/zentus.com/www/sqlitejdbc/src && darcs pull -a
	mv changes.html /var/www/zentus.com/www/sqlitejdbc/
	cp web/*.html web/*.css /var/www/zentus.com/www/sqlitejdbc/
	rm build/Darwin-i386/libsqlitejdbc.jnilib
