import dfhdl.*
import Utils.*
import GameDefs.*

class Life(
    val cfg: RTDomainCfg
) extends RTDesign(cfg):
    val start     = Boolean <> IN // start generation
    val ready     = Boolean <> OUT // cell state ready to be read
    val ready_reg = Boolean <> REG init 0 // cell state ready to be read
    ready := ready_reg
    val alive     = Bit <> OUT // is the cell alive? (when ready)
    val alive_reg = Bit <> REG init 0 // is the cell alive? (when ready)
    alive := alive_reg
    val pixel     = Pixel <> OUT
    val pixel_reg = Pixel <> REG
    pixel := pixel_reg
    @hw.unused
    val changed = Bit <> REG init 0 // cell's state changed (when ready)
    @hw.unused
    val running = Bit <> REG init 0 // life is running
    @hw.unused
    val done = Bit <> REG init 0 // generation complete (high for one tick)

    // world buffer selection
    val next_gen = Bit <> REG init 0 // where to write the next generation
    if (start) next_gen.din := !next_gen // swap every generation

    // world in BRAM
    val DATAW: Int        = 1 // cells are either dead or alive
    val WORLD_WIDTH: Int  = WIDTH + 2 // wider to handle boundary
    val WORLD_HEIGHT: Int = HEIGHT + 2 // taller to handle boundary
    val WORLD_CELLS: Int  = WORLD_WIDTH * WORLD_HEIGHT
    val DEPTH: Int        = 2 * WORLD_CELLS

    val we        = Bit               <> REG
    val cell_id   = UInt.until(DEPTH) <> REG // cell_id is basis of write address
    val addr_read = UInt.until(DEPTH) <> REG
    val data_in   = UInt(DATAW)       <> REG
    val data_out  = UInt(DATAW)       <> WIRE

    val read_addresses = SInt(cell_id.width) X 10 <> VAR init Vector(
      -WORLD_WIDTH - 1, // A
      -1, // B
      WORLD_WIDTH - 1, // C
      -WORLD_WIDTH, // D
      0, // E
      WORLD_WIDTH, // F
      -WORLD_WIDTH + 1, // G
      1, // H
      WORLD_WIDTH + 1, // I
      0 // E
    )

    // add offset to read and write addresses to match buffer used
    val addr_read_offs  = UInt.until(DEPTH) <> WIRE
    val addr_write_offs = UInt.until(DEPTH) <> WIRE
    if (next_gen)
        addr_read_offs  := addr_read
        addr_write_offs := cell_id + WORLD_CELLS
    else
        addr_read_offs  := addr_read + WORLD_CELLS
        addr_write_offs := cell_id

    val bram_inst = new BramSdp(
      WIDTH    = DATAW,
      DEPTH    = DEPTH,
      INIT_F   = SEED_FILE,
      writeCfg = cfg,
      readCfg  = cfg
    )

    bram_inst.write.en   <> we
    bram_inst.write.addr <> addr_write_offs
    bram_inst.read.addr  <> addr_read_offs
    bram_inst.write.data <> data_in
    bram_inst.read.data  <> data_out

    // cell coordinates
    val cell_x     = UInt.until(WORLD_WIDTH)  <> REG // active cell (horizontal)
    val cell_y     = UInt.until(WORLD_HEIGHT) <> REG // active cell (vertical)
    val read_step  = UInt.until(STEPS)        <> REG // reading step
    val inc_read   = Bit                      <> REG // perform incremental read
    val grid_sr    = Bits(GRID) X GRID        <> REG
    val neigh_cnt  = UInt.until(GRID * GRID)  <> REG // count of neighbours
    val grid_index = UInt(2)                  <> WIRE

    enum State extends Encode:
        case IDLE, INIT, READ, NEIGH, UPDATE, NEW_CELL, NEW_LINE
    import State.*
    val state = State <> REG init IDLE
    // life generation state
    grid_index := ((read_step - 2) % 3).resize(2)
    // single-cycle flags: 0 by default
    ready_reg.din := 0
    we.din        := 0
    done.din      := 0

    state match
        case INIT =>
            read_step.din := 0
            inc_read.din  := 1
            // grid_sr   := all(all(0))
            neigh_cnt.din := 0
            state.din     := READ
            running.din   := 1

            // first cell after padding
            cell_x.din  := 1
            cell_y.din  := 1
            cell_id.din := WORLD_WIDTH + 1

        case READ =>
            addr_read.din := (cell_id.signed + read_addresses(read_step)).bits.resize(addr_read.width).uint
            if (read_step >= 2 && (!inc_read || read_step >= 8))
                grid_sr(grid_index).din := (grid_sr(grid_index)(1, 0), data_out)

            if (read_step == STEPS - 1)
                state.din := NEIGH
            else
                read_step.din := read_step + 1
        case NEIGH =>
            // def sumbits(list: (DFBit <> VAL)*): DFUInt[Int] <> VAL = ???
            /* verilator lint_off WIDTH */
            neigh_cnt.din := grid_sr(0)(0, 0).resize(neigh_cnt.width) + grid_sr(0)(1) +
                grid_sr(0)(2) + grid_sr(1)(0) + grid_sr(1)(2) +
                grid_sr(2)(0) + grid_sr(2)(1) + grid_sr(2)(2)

            /* verilator lint_on WIDTH */
            state.din := UPDATE

        case UPDATE =>
            // update cell state
            we.din        := 1 // write new cell state next cycle
            ready_reg.din := 1 // ready for output next cycle
            /* verilator lint_off WIDTH */
            pixel_reg.din.x := cell_x - 1 // correct horizontal position for padding
            pixel_reg.din.y := cell_y - 1 // correct vertical position for padding
            /* verilator lint_on WIDTH */

            if (grid_sr(1)(1)) // cell was alive this generation
                if (neigh_cnt == 2 || neigh_cnt == 3) // still alive
                    data_in.din   := 1
                    alive_reg.din := 1
                    changed.din   := 0
                else // now dead
                    data_in.din   := 0
                    alive_reg.din := 0
                    changed.din   := 1
            else // was dead this generation
            if (neigh_cnt == 3) // now alive
                data_in.din   := 1
                alive_reg.din := 1
                changed.din   := 1
            else // still dead
                data_in.din   := 0
                alive_reg.din := 0
                changed.din   := 0

            // what next?
            if (cell_x == WORLD_WIDTH - 2) // final cell on line
                if (cell_y == WORLD_HEIGHT - 2) // final line of cells
                    state.din   := IDLE
                    running.din := 0
                    done.din    := 1
                else state.din := NEW_LINE
            else
                state.din := NEW_CELL
            neigh_cnt.din := 0

        case NEW_CELL =>
            cell_x.din    := cell_x + 1
            cell_id.din   := cell_id + 1
            inc_read.din  := 1 // incremental read
            read_step.din := 6 // read new column of 3 cells (skip A-F)
            state.din     := READ

        case NEW_LINE =>
            cell_y.din    := cell_y + 1
            cell_x.din    := 1
            cell_id.din   := cell_id + 3 // skip 2 cells of padding
            read_step.din := 0 // read all nine cells at start of line
            state.din     := READ

        case _ =>
            if (start)
                state.din := INIT
            else
                state.din := IDLE
