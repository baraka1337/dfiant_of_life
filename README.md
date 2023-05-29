# DFiant of Life

## Using verilator in wsl

```bash
cd verilator
make # Regular compile
make trace # Compile with VCD dump - simx.vcd 
./obj_dir/life_sdl # Running
```

## Creating a Quartus Project

```bash
cd quartus
quartus_sh -t game_of_life.tcl
```

*if `quartus_sh` doesn't exist, add the Quartus's binaries folder the the PATH*

The last command creates the project. Double click `game_of_life.qpf` to open the project.
