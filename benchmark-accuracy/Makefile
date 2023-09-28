all: tools mosaic

tools:
	cd dlmmse && make -f makefile.gcc

mosaic:
	test -d mosaic || mkdir mosaic
	$(foreach file, $(wildcard truth/*), dlmmse/mosaic -f -p RGGB $(file) mosaic/$(notdir $(file));)