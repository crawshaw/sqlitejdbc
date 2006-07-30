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
os := $(shell uname)
endif

ifeq ($(arch),)
arch := $(shell uname -m)
endif

ifeq ($(arch),Power Macintosh) # OS X gives funny result for 'uname -m'
arch := powerpc
endif


# OS Specific Variables #############################################

Linux_CC         := gcc
Linux_STRIP      := strip
Linux_CCFLAGS    := -Isrc/jni/Linux
Linux_LINKFLAGS  := -shared
Linux_LIBNAME    := libsqlitejdbc.so

Darwin_CC        := $(arch)-apple-darwin-gcc
Darwin_STRIP     := $(arch)-apple-darwin-strip -x
Darwin_CCFLAGS   := -Isrc/jni/Darwin
Darwin_LINKFLAGS := -dynamiclib
Darwin_LIBNAME   := libsqlitejdbc.jnilib

Win_CC           := $(arch)-mingw32msvc-gcc
Win_STRIP        := $(arch)-mingw32msvc-strip
Win_CCFLAGS      := -D_JNI_IMPLEMENTATION_ -Isrc/jni/Win -Iwork
Win_LINKFLAGS    := -Wl,--kill-at -shared
Win_LIBNAME      := sqlitejdbc.dll


# Makefile Variables, loaded from OS vars above #####################

java_sources := $(shell find src -name \*.java)
java_classes := $(java_sources:src/%.java=build/%.class)

target    := $(os)-$(arch)
VERSION   := $(shell cat VERSION)
LIPO      := powerpc-apple-darwin-lipo
CC        := $($(os)_CC)
STRIP     := $($(os)_STRIP)
CCFLAGS   := $($(os)_CCFLAGS) -Iwork/sqlite/$(target) -Ibuild
LINKFLAGS := $($(os)_LINKFLAGS)
LIBNAME   := $($(os)_LIBNAME)


default: test

build/%.class: src/%.java
	@mkdir -p build
	javac -sourcepath src -d build $<

compile: work/sqlite/$(target)/main.o $(java_classes)
	@mkdir -p build/$(target)
	javah -classpath build -jni -o build/DB.h org.sqlite.DB
	jar cf build/sqlitejdbc.jar -C build org
	$(CC) $(CCFLAGS) -c -O -o build/$(target)/DB.o src/org/sqlite/DB.c
	$(CC) $(CCFLAGS) $(LINKFLAGS) -o build/$(target)/$(LIBNAME) \
		build/$(target)/DB.o work/sqlite/$(target)/*.o
	$(STRIP) build/$(target)/$(LIBNAME)

dist: compile
	@mkdir -p dist
	tar cfz dist/sqlitejdbc-v$(VERSION)-$(target).tgz README \
	    -C build sqlitejdbc.jar -C $(target) $(LIBNAME)

all:
	@make os=Linux arch=i386 dist
	@make os=Win arch=i586 dist
	@make os=Darwin arch=powerpc compile
	@make os=Darwin arch=i386 compile
	@mkdir build/Darwin-lipo
	$(LIPO) -create \
	    build/Darwin-powerpc/libsqlitejdbc.jnilib \
	    build/Darwin-i386/libsqlitejdbc.jnilib \
	    -output build/Darwin-lipo/libsqlitejdbc.jnilib
	tar cfz dist/sqlitejdbc-v$(VERSION)-Mac.tgz README \
	    -C build sqlitejdbc.jar -C Darwin-powerpc libsqlitejdbc.jnilib
	tar cfz dist/sqlitejdbc-v$(VERSION)-src.tgz \
		Makefile README LICENSE VERSION src/org

work/sqlite-src.zip:
	@mkdir -p work
	wget -O work/sqlite-src.zip http://www.sqlite.org/sqlite-source-3_3_6.zip

work/sqlite/%/main.o: work/sqlite-src.zip
	mkdir -p work/sqlite/$*
	(cd work/sqlite/$*; \
	          unzip -qo ../../sqlite-src.zip; \
	          mv shell.c shell.c.old; \
	          mv tclsqlite.c tclsqlite.c.not; \
	          $(CC) -c -O -DSQLITE_ENABLE_COLUMN_METADATA *.c)

build/test/test.db:
	mkdir -p build/test
	sqlite3 build/test/test.db ".read src/test/create.sql"

test: compile build/test/test.db
	java -Djava.library.path=build/$(target) -cp build test.Test

clean:
	rm -rf build
	rm -rf dist
	rm -rf work/sqlite

distclean: clean
	rm -rf work
