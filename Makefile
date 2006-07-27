java_sources := $(shell find src -name \*.java)
java_classes := $(java_sources:src/%.java=build/%.class)

ifeq ($(target),)
target := $(shell uname)
endif

Linux_CC         := gcc
Linux_CCFLAGS    := -I$(java_home)/include
Linux_LINKFLAGS  := -shared
Linux_LIBNAME    := libsqlitejdbc.so

Darwin_CC        := gcc
Darwin_CCFLAGS   := -I/System/Library/Frameworks/JavaVM.framework/Headers
Darwin_LINKFLAGS := -dynamiclib -framework JavaVM
Darwin_LIBNAME   := libsqlitejdbc.jnilib

Win_CC           := i386-mingw32msvc-gcc
Win_CCFLAGS      := -D_JNI_IMPLEMENTATION_ -I$(java_home)/include -Iwork
Win_LINKFLAGS    := -Wl,--kill-at -shared
Win_LIBNAME      := sqlitejdbc.dll


CC        := $($(target)_CC)
CCFLAGS   := $($(target)_CCFLAGS) -Iwork/sqlite/$(target) -Ibuild/$(target)
LINKFLAGS := $($(target)_LINKFLAGS)
LIBNAME   := $($(target)_LIBNAME)


default: test

build/%.class: src/%.java
	@mkdir -p build
	javac -sourcepath src -d build $<

compile: work/sqlite/$(target)/main.o $(java_classes)
	@mkdir -p build/$(target)
	javah -classpath build -jni -o build/$(target)/DB.h org.sqlite.DB
	$(CC) $(CCFLAGS) -c -O -o build/$(target)/DB.o src/org/sqlite/DB.c
	$(CC) $(CCFLAGS) $(LINKFLAGS) -o build/$(target)/$(LIBNAME) \
		build/$(target)/DB.o work/sqlite/$(target)/*.o

work/sqlite-src.zip:
	@mkdir -p work
	wget -O work/sqlite-src.zip http://www.sqlite.org/sqlite-source-3_3_6.zip

work/sqlite/%/main.o: work/sqlite-src.zip
	mkdir -p work/sqlite/$*
	(cd work/sqlite/$*; \
	          unzip -o ../../sqlite-src.zip; \
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
	rm -rf work/sqlite

distclean: clean
	rm -rf work
