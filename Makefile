# Makefile for SQLite JDBC Driver 
# Author: David Crawshaw 2006
# Released under New BSD License (see LICENSE file).
#
# No auto-goop. Just try typing 'make'. See README for more.
#

ifndef JAVA_HOME
$(error Set env variable JAVA_HOME)
endif

#
# Target-specific variables
#
ifeq ($(os),)
	ifeq ($(shell uname),Darwin)
		os := Darwin
	endif
	ifeq ($(findstring CYGWIN,$(shell uname)),CYGWIN)
		os := Win
	endif
	ifeq ($(findstring MINGW,$(shell uname)),MINGW)
		os := Win
	endif
endif
ifeq ($(os),)
	os := Default
endif

# Windows uses different path separators, because they hate me
ifeq ($(os),Win)
	sep := ;
else
	sep := :
endif

ifeq ($(arch),)
arch := $(shell uname -m)
endif

Default_CC        := gcc
Default_STRIP     := strip
Default_CFLAGS    := -I$(JAVA_HOME)/include -O -fPIC
Default_LINKFLAGS := -shared
Default_LIBNAME   := libsqlitejdbc.so

Darwin_CC        := gcc -arch $(arch)
Darwin_STRIP     := strip -x
Darwin_CFLAGS    := -I$(JAVA_HOME)/include -O -fPIC
Darwin_LINKFLAGS := -dynamiclib
Darwin_LIBNAME   := libsqlitejdbc.jnilib

Win_CC           := $(arch)-mingw32-gcc
Win_STRIP        := $(arch)-mingw32-strip
Win_CFLAGS       := -D_JNI_IMPLEMENTATION_ -Ilib/inc_win -O
Win_LINKFLAGS    := -Wl,--kill-at -shared
Win_LIBNAME      := sqlitejdbc.dll

CC        := $($(os)_CC)
STRIP     := $($(os)_STRIP)
CFLAGS    := $($(os)_CFLAGS)
LINKFLAGS := $($(os)_LINKFLAGS)
LIBNAME   := $($(os)_LIBNAME)


#
# Generic variables
#

# TODO: remove these by using symlinks upstream/nestedvm and /sqlite
nestedvm_version := 2007-01-12
nestedvm := nestedvm-$(nestedvm_version)
sqlite_version := 3.3.12
sqlite := sqlite-$(sqlite_version)

target     := $(os)-$(arch)
sqlitejdbc := sqlitejdbc-v$(shell cat VERSION)

jni_md := $(shell find "$(JAVA_HOME)" -name jni_md.h)
ifneq ($(jni_md),)
jni_include := $(shell dirname "$(jni_md)")
endif

libs := build$(sep)$(subst  ,$(sep),$(wildcard lib/*.jar))

java_sources := $(wildcard src/org/sqlite/*.java)
java_classes := $(java_sources:src/%.java=build/%.class)
native_classes := $(filter-out %NestedDB.class,$(java_classes))
nested_classes := $(filter-out %NativeDB.class,$(java_classes))
test_sources := $(wildcard src/test/*.java)
test_classes := $(test_sources:src/%.java=build/%.class)
tests        := $(subst /,.,$(patsubst build/%.class,%,$(test_classes)))

default: test-nested test-native


CC        := $($(os)_CC)
STRIP     := $($(os)_STRIP)
CFLAGS    := $($(os)_CFLAGS)
LINKFLAGS := $($(os)_LINKFLAGS)
LIBNAME   := $($(os)_LIBNAME)

CFLAGS := $(CFLAGS) -Iupstream/$(sqlite)-$(target) -Ibuild
ifneq ($(jni_include),)
CFLAGS := $(CFLAGS) -I$(jni_include)
endif


upstream/%:
	$(MAKE) -C upstream CC="$(CC)" CFLAGS="$(CFLAGS)" $*

build/org/%.class: src/org/%.java
	@mkdir -p build
	javac -source 1.2 -target 1.2 -classpath "upstream/$(nestedvm)/build" \
	    -sourcepath src -d build $<

build/test/%.class: src/test/%.java
	@mkdir -p build
	javac -target 1.5 -classpath "$(libs)" -sourcepath src/test -d build $<

native: upstream/$(sqlite)-$(target)/main.o $(native_classes)
	@mkdir -p build/$(target)
	javah -classpath build -jni -o build/NativeDB.h org.sqlite.NativeDB
	rm -f build/org/sqlite/NestedDB*.class
	cd build && jar cf $(sqlitejdbc)-native.jar `find org -name \*.class`
	$(CC) $(CFLAGS) -c -O -o build/$(target)/NativeDB.o \
		src/org/sqlite/NativeDB.c
	rm -f upstream/$(sqlite)-$(target)/NestedDB.o # TODO this is ugly
	$(CC) $(CFLAGS) $(LINKFLAGS) -o build/$(target)/$(LIBNAME) \
		build/$(target)/NativeDB.o upstream/$(sqlite)-$(target)/*.o
	$(STRIP) build/$(target)/$(LIBNAME)

nested: upstream/build/org/sqlite/SQLite.class $(nested_classes)
	rm -f build/org/sqlite/NativeDB*.class
	cd build && jar cf $(sqlitejdbc)-nested.jar \
	    `find org -name \*.class` \
	    -C ../upstream/build org/sqlite/SQLite.class \
	    -C ../upstream/$(nestedvm)/build org/ibex

dist/$(sqlitejdbc)-nested.tgz: nested
	@mkdir -p dist
	tar cfz dist/$(sqlitejdbc)-nested.tgz \
	    README -C build $(sqlitejdbc)-nested.jar

dist/$(sqlitejdbc)-$(target).tgz: native
	@mkdir -p dist
	tar cfz dist/$(sqlitejdbc)-$(target).tgz README \
	    -C build $(sqlitejdbc)-native.jar -C $(target) $(LIBNAME)

test-native: native $(test_classes)
	java -Djava.library.path=build/$(target) \
	    -cp "build/$(sqlitejdbc)-native.jar$(sep)$(libs)" \
	    org.junit.runner.JUnitCore $(tests)

test-nested: nested $(test_classes)
	java -cp "build/$(sqlitejdbc)-nested.jar$(sep)$(libs)" \
	    org.junit.runner.JUnitCore $(tests)

clean:
	rm -rf build
	rm -rf dist
	rm -rf upstream/build

distclean: clean upstream/clean

dist/$(sqlitejdbc)-src.tgz:
	@mkdir -p dist
	tar cfz dist/$(sqlitejdbc)-src.tgz \
	    Makefile README LICENSE VERSION src lib/junit-4.1.jar upstream/Makefile

doc:
	mkdir -p build/doc
	javadoc -notree -d build/doc src/org/sqlite/*.java
