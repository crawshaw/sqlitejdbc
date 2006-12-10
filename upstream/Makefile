sqlite_version := 3.3.8
nestedvm_version := 2006-11-19

nestedvm := nestedvm-$(nestedvm_version)
sqlite := sqlite-$(sqlite_version)

nestedvm_bin := $(nestedvm)/upstream/install/bin

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
	curl -odl/$(nestedvm).tgz \
	    http://www.zentus.com/nestedvm/dist/$(nestedvm).tgz

$(nestedvm)/Makefile: dl/$(nestedvm).tgz
	tar xfz ../dl/$(nestedvm).tgz

$(sqlite)-%/main.o: dl/$(sqlite).zip
	@mkdir -p $(sqlite)-$*
	unzip -qo dl/$(sqlite).zip -d $(sqlite)-$*
	cp ../src/org/sqlite/NestedDB.c $(sqlite)-$*
	perl -pi -e "s/sqlite3_api;/sqlite3_api = 0;/g" $(sqlite)-$*/sqlite3ext.h
	#rm $(sqlite)-$*/shell.c TODO: replace with empty main()
	(cd $(sqlite)-$*; $(CC) -c $(CFLAGS) \
	    -DSQLITE_ENABLE_COLUMN_METADATA \
	    -DNO_TCL \
	    -DSQLITE_CORE \
	    -DSQLITE_ENABLE_FTS1 \
	    -DSQLITE_OMIT_LOAD_EXTENSION *.c)

build/SQLite.mips: $(nestedvm)/Makefile $(nestedvm)/env.sh
	@mkdir -p build
	(. ./$(nestedvm)/env.sh; $(MAKE) -e $(sqlite)-nestedvm/main.o)
	./$(nestedvm)/upstream/install/bin/mips-unknown-elf-gcc \
	        -march=mips1 --static \
	        -o $@ $(sqlite)-nestedvm/*.o
			#-Wl,--gc-sections

build/org/sqlite/SQLite.class: build/SQLite.mips
	java -cp $(nestedvm)/build:$(nestedvm)/upstream/build/classgen/build \
	    org.ibex.nestedvm.Compiler \
	    -outformat class -d build -o unixRuntime \
	    org.sqlite.SQLite build/SQLite.mips

