# Simple wrapper for cmake

# Use "make PREFIX=/my/dir" to change installation location
PREFIX = /usr/local

BUILD_DIR = build

all:
	test -d $(BUILD_DIR) || mkdir $(BUILD_DIR)
	(cd $(BUILD_DIR); cmake .. -G "Unix Makefiles" -DCMAKE_INSTALL_PREFIX=$(PREFIX); cmake --build .  --verbose)

# May require sudo
install: all
	(cd $(BUILD_DIR); $(MAKE) install)

clean:
	rm -rf $(BUILD_DIR)

test: all
	(cd $(BUILD_DIR); $(MAKE) test)

