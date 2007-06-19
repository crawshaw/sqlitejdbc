sqlite_version := 3.4.0
nestedvm_version := 2007-01-12

nestedvm := nestedvm-$(nestedvm_version)
sqlite := sqlite-$(sqlite_version)

nestedvm_bin := $(nestedvm)/upstream/install/bin

sep := :
ifeq ($(findstring CYGWIN, $(shell uname)),CYGWIN)
	sep := ;
endif
ifeq ($(findstring MINGW, $(shell uname)),MINGW)
	sep := ;
endif

#
# Gneral Glue
#
$(nestedvm)/%:
	$(MAKE) -C $(nestedvm) $*

clean:
	rm -rf build

#
# Build Targets
#
dl/$(sqlite).zip:
	@mkdir -p dl
	curl -odl/$(sqlite).zip \
	    http://www.sqlite.org/sqlite-source-$(subst .,_,$(sqlite_version)).zip

dl/$(nestedvm).tgz:
	@mkdir -p dl
	curl -odl/$(nestedvm).tgz http://nestedvm.ibex.org/dist/$(nestedvm).tgz

$(nestedvm)/Makefile: dl/$(nestedvm).tgz
	tar xfz dl/$(nestedvm).tgz

$(sqlite)-%/main.o: dl/$(sqlite).zip
	@mkdir -p $(sqlite)-$*
	unzip -qo dl/$(sqlite).zip -d $(sqlite)-$*
	perl -pi -e "s/sqlite3_api;/sqlite3_api = 0;/g" $(sqlite)-$*/sqlite3ext.h
	rm $(sqlite)-$*/tclsqlite.c
	(cd $(sqlite)-$*; $(CC) -c $(CFLAGS) \
	    -DSQLITE_ENABLE_COLUMN_METADATA \
	    -DSQLITE_CORE \
	    -DSQLITE_OMIT_LOAD_EXTENSION *.c)

build/SQLite.mips: $(nestedvm)/Makefile $(nestedvm)/env.sh
	@mkdir -p build
	@mkdir -p $(sqlite)-nestedvm
	unzip -qo dl/$(sqlite).zip -d $(sqlite)-nestedvm
	cp ../src/org/sqlite/NestedDB.c $(sqlite)-nestedvm
	perl -pi -e "s/sqlite3_api;/sqlite3_api = 0;/g" \
	    $(sqlite)-nestedvm/sqlite3ext.h
	# WARNING: line below could be dangerous to native driver, that's why
	#          I've replicated the code from above and ensured no other sane
	#          human being will ever understand this build process. Sorry.
	perl -pi -e "s/# define THREADSAFE 1//g" $(sqlite)-nestedvm/os_unix.c
	rm $(sqlite)-nestedvm/tclsqlite.c
	(. ./$(nestedvm)/env.sh; cd $(sqlite)-nestedvm; $$CC -c $$CFLAGS \
	    -DSQLITE_ENABLE_COLUMN_METADATA \
	    -DSQLITE_CORE \
	    -DSQLITE_OMIT_LOAD_EXTENSION *.c)

	./$(nestedvm)/upstream/install/bin/mips-unknown-elf-gcc \
	        -march=mips1 --static \
	        -o $@ $(sqlite)-nestedvm/*.o
			#-Wl,--gc-sections

build/org/sqlite/SQLite.class: build/SQLite.mips
	java -cp $(nestedvm)/build$(sep)$(nestedvm)/upstream/build/classgen/build \
	    org.ibex.nestedvm.Compiler \
	    -outformat class -d build -o unixRuntime \
	    org.sqlite.SQLite build/SQLite.mips

