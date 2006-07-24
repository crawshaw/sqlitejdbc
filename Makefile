java_sources := $(shell find src -name \*.java)
java_classes := $(java_sources:src/%.java=build/%.class)

ifeq ($(target),)
target := $(shell uname)
endif

Linux_CCFLAGS    := -I$(java_home)/include
Linux_LINKFLAGS  := -shared
Linux_LIBNAME    := libsqlitejdbc.so

Darwin_CCFLAGS   := -I/System/Library/Frameworks/JavaVM.framework/Headers
Darwin_LINKFLAGS := -dynamiclib -framework JavaVM
Darwin_LIBNAME   := libsqlitejdbc.jnilib

CCFLAGS   := $($(target)_CCFLAGS) -Iwork -Ibuild
LINKFLAGS := $($(target)_LINKFLAGS)
LIBNAME   := $($(target)_LIBNAME)


default: test

build/%.class: src/%.java
	@mkdir -p build
	javac -sourcepath src -d build $<

compile: work/main.o $(java_classes)
	javah -classpath build -jni -o build/DB.h org.sqlite.DB
	cc $(CCFLAGS) -c -O -o build/DB.o src/org/sqlite/DB.c
	cc $(CCFLAGS) $(LINKFLAGS) -o build/$(LIBNAME) \
		build/DB.o work/*.o

work/sqlite-src.zip:
	@mkdir -p work
	wget -O work/sqlite-src.zip http://www.sqlite.org/sqlite-source-3_3_6.zip

work/main.o: work/sqlite-src.zip
	(cd work; unzip -o sqlite-src.zip; \
	          mv shell.c shell.c.old; \
	          mv tclsqlite.c tclsqlite.c.not)
	(cd work; cc -c -O -DSQLITE_ENABLE_COLUMN_METADATA *.c)

build/test/test.db:
	mkdir -p build/test
	sqlite3 build/test/test.db ".read src/test/create.sql"

test: compile build/test/test.db
	java -Djava.library.path=build -cp build test.Test

clean:
	rm -rf build

distclean: clean
	rm -rf work
