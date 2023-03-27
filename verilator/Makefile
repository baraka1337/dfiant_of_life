## Project F: FPGA Graphics - Verilator Sim Makefile
## (C)2022 Will Green, open source hardware released under the MIT License
## Learn more at https://projectf.io/posts/fpga-graphics/

CFLAGS = -O2
SDL_CFLAGS = `sdl2-config --cflags`
SDL_LDFLAGS = `sdl2-config --libs`

DIR=.
DFIANT="${DIR}/../sandbox"
origin_dir="."

life_sdl: life_sdl.exe

%.exe: %.mk
	make -C ./obj_dir -f Vtop_$<

%.mk: main_life_sdl.cpp
	verilator $(TRACE_FLAG) -Wno-WIDTHEXPAND -Wno-UNSIGNED -I${DIR} -I${DFIANT} \
 -cc ${DFIANT}/top_life_sdl.sv --exe main_life_sdl${TRACE_END}.cpp -o $(basename $@) \
		-CFLAGS "${CFLAGS} ${SDL_CFLAGS}" -LDFLAGS "${SDL_LDFLAGS}"

all: life_sdl

trace: TRACE_FLAG=--trace
trace: TRACE_END=_trace
trace: life_sdl

clean:
	rm -rf ./obj_dir

.PHONY: all clean trace
