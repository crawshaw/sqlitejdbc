# Makefile for SQLite JDBC Driver 
# Author: David Crawshaw 2006
# Released under New BSD License (see LICENSE file).
#
# No auto-goop. It is expected that this file will be run either with
# all the cross-compilers installed by 'make all', or by using the
# 'dist' target with two variables, 'os' and 'arch'. E.g.
#
#  	      make os=Linux arch=i386 dist
#
# See README for more.
#

ifeq ($(os),)
os := Default
endif

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

# Generic Variables, used if no $(os) is given ######################

Default_CC        := gcc
Default_STRIP     := strip
Default_CCFLAGS   := -I$(JAVA_HOME)/include -I$(jni_include) -O -fPIC
Default_LINKFLAGS := -shared
Default_LIBNAME   := libsqlitejdbc.so

ifeq ($(shell uname),Darwin)
	Default_STRIP     := strip -x
	Default_LINKFLAGS := -dynamiclib
	Default_LIBNAME   := libsqlitejdbc.jnilib
endif

# OS Specific Variables #############################################

Linux_CC         := gcc
Linux_STRIP      := strip
Linux_CCFLAGS    := -Isrc/jni/Linux -O -fPIC
Linux_LINKFLAGS  := -shared
Linux_LIBNAME    := libsqlitejdbc.so

Darwin_CC        := $(arch)-apple-darwin-gcc
Darwin_STRIP     := $(arch)-apple-darwin-strip -x
Darwin_CCFLAGS   := -DNDEBUG -Isrc/jni/Darwin -O
Darwin_LINKFLAGS := -dynamiclib
Darwin_LIBNAME   := libsqlitejdbc.jnilib

Win_CC           := $(arch)-mingw32msvc-gcc
Win_STRIP        := $(arch)-mingw32msvc-strip
Win_CCFLAGS      := -D_JNI_IMPLEMENTATION_ -Isrc/jni/Win -Iwork -O
Win_LINKFLAGS    := -Wl,--kill-at -shared
Win_LIBNAME      := sqlitejdbc.dll


# Makefile Variables, loaded from OS vars above #####################

libs := $(subst  ,:,$(shell find lib -name \*.jar))
java_sources := $(shell find src/org -name \*.java)
java_classes := $(java_sources:src/%.java=build/%.class)

target    := $(os)-$(arch)
VERSION   := $(shell cat VERSION)
PNAME     := sqlitejdbc-v$(VERSION)
LIPO      := powerpc-apple-darwin-lipo
CC        := $($(os)_CC)
STRIP     := $($(os)_STRIP)
CCFLAGS   := $($(os)_CCFLAGS) -Iwork/sqlite/$(target) -Ibuild
LINKFLAGS := $($(os)_LINKFLAGS)
LIBNAME   := $($(os)_LIBNAME)


default: test

build/org/%.class: src/org/%.java
	@mkdir -p build
	javac -source 1.2 -target 1.2 -sourcepath src -d build $<

compile: work/sqlite/$(target)/main.o $(java_classes)
	@mkdir -p build/$(target)
	javah -classpath build -jni -o build/NativeDB.h org.sqlite.NativeDB
	jar cf build/sqlitejdbc.jar -C build org
	$(CC) $(CCFLAGS) -c -O -o build/$(target)/NativeDB.o \
		src/org/sqlite/NativeDB.c
	$(CC) $(CCFLAGS) $(LINKFLAGS) -o build/$(target)/$(LIBNAME) \
		build/$(target)/NativeDB.o work/sqlite/$(target)/*.o
	$(STRIP) build/$(target)/$(LIBNAME)

dist: compile
	@mkdir -p dist
	tar cfz dist/$(PNAME)-$(target).tgz README \
	    -C build sqlitejdbc.jar -C $(target) $(LIBNAME)

all: src-tgz
	@mkdir -p src/jni/Linux src/jni/Darwin src/jni/Win
	@make os=Linux arch=i386 dist
	@make os=Win arch=i586 dist
	#@make os=Darwin arch=powerpc compile
	#@make os=Darwin arch=i386 compile
	@mkdir -p build/Darwin-lipo
	$(LIPO) -create \
	    build/Darwin-powerpc/libsqlitejdbc.jnilib \
	    build/Darwin-i386/libsqlitejdbc.jnilib \
	    -output build/Darwin-lipo/libsqlitejdbc.jnilib
	tar cfz dist/$(PNAME)-Mac.tgz README \
	    -C build sqlitejdbc.jar -C Darwin-lipo libsqlitejdbc.jnilib
	jar cfm dist/sqlitejdbc-test.jar src/test/manifest \
	    -C build org -C build test

src-tgz:
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

work/sqlite-src.zip:
	@mkdir -p work
	curl -owork/sqlite-src.zip http://www.sqlite.org/sqlite-source-3_3_8.zip

work/sqlite/%/main.o: work/sqlite-src.zip
	mkdir -p work/sqlite/$*
	(cd work/sqlite/$*; \
	          unzip -qo ../../sqlite-src.zip; \
	          mv shell.c shell.c.old; \
	          mv tclsqlite.c tclsqlite.c.not; \
	          perl -pi -e "s/sqlite3_api;/sqlite3_api = 0;/g" sqlite3ext.h; \
	          $(CC) -c $(CCFLAGS) \
	              -DSQLITE_ENABLE_COLUMN_METADATA \
	              -DSQLITE_CORE \
	              -DSQLITE_ENABLE_FTS1 \
	              -DSQLITE_OMIT_LOAD_EXTENSION *.c)

doc:
	mkdir -p build/doc
	javadoc -notree -d build/doc src/org/sqlite/*.java

test: compile
	javac -target 1.5 -cp $(libs):build -sourcepath src/test -d build \
		$(shell find src/test -name \*.java)
	java -Djava.library.path=build/$(target) -cp build:$(libs) \
		org.junit.runner.JUnitCore \
		test.ConnectionTest \
		test.StatementTest \
		test.PrepStmtTest \
		test.TransactionTest \
		test.UDFTest \
		test.RSMetaDataTest \
		test.DBMetaDataTest

speed: compile build/test/test.db
	java -Djava.library.path=build/$(target) -cp build test.Speed

speedfull: compile build/test/test.db
	java -Djava.library.path=build/$(target) -cp build test.Speed full

clean:
	rm -rf build
	rm -rf dist
	rm -rf work/sqlite

distclean: clean
	rm -rf work


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
