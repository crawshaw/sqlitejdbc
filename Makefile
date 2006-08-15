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

jni_md := $(shell find $(JAVA_HOME) -name jni_md.h)
ifneq ($(jni_md),)
jni_include := $(shell dirname $(jni_md))
endif

# Generic Variables, used if no $(os) is given ######################

Default_CC        := gcc
Default_STRIP     := strip
Default_CCFLAGS   := -I$(JAVA_HOME)/include -I$(jni_include) -O
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
Linux_CCFLAGS    := -Isrc/jni/Linux -O
Linux_LINKFLAGS  := -shared
Linux_LIBNAME    := libsqlitejdbc.so

Darwin_CC        := $(arch)-apple-darwin-gcc
Darwin_STRIP     := $(arch)-apple-darwin-strip -x
Darwin_CCFLAGS   := -Isrc/jni/Darwin -O
Darwin_LINKFLAGS := -dynamiclib
Darwin_LIBNAME   := libsqlitejdbc.jnilib

Win_CC           := $(arch)-mingw32msvc-gcc
Win_STRIP        := $(arch)-mingw32msvc-strip
Win_CCFLAGS      := -D_JNI_IMPLEMENTATION_ -Isrc/jni/Win -Iwork -O
Win_LINKFLAGS    := -Wl,--kill-at -shared
Win_LIBNAME      := sqlitejdbc.dll


# Makefile Variables, loaded from OS vars above #####################

java_sources := $(shell find src -name \*.java)
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
	tar cfz dist/$(PNAME)-$(target).tgz README \
	    -C build sqlitejdbc.jar -C $(target) $(LIBNAME)

all: src-tgz
	@make os=Linux arch=i386 dist
	@make os=Win arch=i586 dist
	@make os=Darwin arch=powerpc compile
	@make os=Darwin arch=i386 compile
	@mkdir -p build/Darwin-lipo
	$(LIPO) -create \
	    build/Darwin-powerpc/libsqlitejdbc.jnilib \
	    build/Darwin-i386/libsqlitejdbc.jnilib \
	    -output build/Darwin-lipo/libsqlitejdbc.jnilib
	tar cfz dist/-Mac.tgz README \
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
	cd work && tar cfz ../dist/$(PNAME)-src.tgz $(PNAME)

work/sqlite-src.zip:
	@mkdir -p work
	wget -O work/sqlite-src.zip http://www.sqlite.org/sqlite-source-3_3_7.zip

work/sqlite/%/main.o: work/sqlite-src.zip
	mkdir -p work/sqlite/$*
	(cd work/sqlite/$*; \
	          unzip -qo ../../sqlite-src.zip; \
	          mv shell.c shell.c.old; \
	          mv tclsqlite.c tclsqlite.c.not; \
	          $(CC) -c -O -DSQLITE_ENABLE_COLUMN_METADATA \
	                      -DSQLITE_OMIT_LOAD_EXTENSION *.c)

build/test/test.db:
	mkdir -p build/test
	sqlite3 build/test/test.db ".read src/test/create.sql"

test: compile build/test/test.db
	java -Djava.library.path=build/$(target) -cp build test.Test

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
