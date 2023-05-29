import dfhdl.*
import Utils.*
import GameDefs.*

class Life extends EDDesign:
    val clk   = Bit     <> IN // clock
    val rst   = Boolean <> IN // reset
    val start = Boolean <> IN // start generation
    val ready = Boolean <> OUT // cell state ready to be read
    val alive = Bit     <> OUT // is the cell alive? (when ready)
    @hw.unused
    val changed = Bit   <> VAR // cell's state changed (when ready)
    val pixel   = Pixel <> OUT
    @hw.unused
    val running = Bit <> VAR // life is running
    @hw.unused
    val done = Bit <> VAR // generation complete (high for one tick)

    // world buffer selection
    val next_gen = Bit <> VAR // where to write the next generation
    process(clk.rising) {
        if (start) next_gen :== !next_gen // swap every generation
        if (rst) next_gen   :== 0
    }

    // world in BRAM
    val DATAW: Int        = 1 // cells are either dead or alive
    val WORLD_WIDTH: Int  = WIDTH + 2 // wider to handle boundary
    val WORLD_HEIGHT: Int = HEIGHT + 2 // taller to handle boundary
    val WORLD_CELLS: Int  = WORLD_WIDTH * WORLD_HEIGHT
    val DEPTH: Int        = 2 * WORLD_CELLS

    val we        = Bit               <> VAR
    val cell_id   = UInt.until(DEPTH) <> VAR // cell_id is basis of write address
    val addr_read = UInt.until(DEPTH) <> VAR
    val data_in   = UInt(DATAW)       <> VAR
    val data_out  = UInt(DATAW)       <> VAR

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
    val addr_read_offs  = UInt.until(DEPTH) <> VAR
    val addr_write_offs = UInt.until(DEPTH) <> VAR
    process(all) {
        if (next_gen)
            addr_read_offs  := addr_read
            addr_write_offs := cell_id + WORLD_CELLS
        else
            addr_read_offs  := addr_read + WORLD_CELLS
            addr_write_offs := cell_id
    }

    val bram_inst = new BramSdp(
      WIDTH  = DATAW,
      DEPTH  = DEPTH,
      INIT_F = SEED_FILE
    )

    bram_inst.clk_write  <> clk
    bram_inst.clk_read   <> clk
    bram_inst.we         <> we
    bram_inst.addr_write <> addr_write_offs
    bram_inst.addr_read  <> addr_read_offs
    bram_inst.data_in    <> data_in
    bram_inst.data_out   <> data_out

    // cell coordinates
    val cell_x     = UInt.until(WORLD_WIDTH)  <> VAR // active cell (horizontal)
    val cell_y     = UInt.until(WORLD_HEIGHT) <> VAR // active cell (vertical)
    val read_step  = UInt.until(STEPS)        <> VAR // reading step
    val inc_read   = Bit                      <> VAR // perform incremental read
    val grid_sr    = Bits(GRID) X GRID        <> VAR
    val neigh_cnt  = UInt.until(GRID * GRID)  <> VAR // count of neighbours
    val grid_index = UInt(2)                  <> VAR

    enum State extends Encode:
        case IDLE, INIT, READ, NEIGH, UPDATE, NEW_CELL, NEW_LINE
    import State.*
    val state = State <> VAR init IDLE
    // life generation state
    process(all) {
        grid_index := ((read_step - 2) % 3).resize(2)
    }
    process(clk.rising) {
        // single-cycle flags: 0 by default
        ready :== 0
        we    :== 0
        done  :== 0

        state match
            case INIT =>
                read_step :== 0
                inc_read  :== 1
                // grid_sr   :== all(all(0))
                neigh_cnt :== 0
                state     :== READ
                running   :== 1

                // first cell after padding
                cell_x  :== 1
                cell_y  :== 1
                cell_id :== WORLD_WIDTH + 1

            case READ =>
                addr_read :== (cell_id.signed + read_addresses(read_step)).bits.resize(addr_read.width).uint
                if (read_step >= 2 && (!inc_read || read_step >= 8))
                    grid_sr(grid_index) :== (grid_sr(grid_index)(1, 0), data_out)

                if (read_step == STEPS - 1)
                    state :== NEIGH
                else
                    read_step :== read_step + 1
            case NEIGH =>
                // def sumbits(list: (DFBit <> VAL)*): DFUInt[Int] <> VAL = ???
                /* verilator lint_off WIDTH */
                neigh_cnt :== grid_sr(0)(0, 0).resize(neigh_cnt.width) + grid_sr(0)(1) +
                    grid_sr(0)(2) + grid_sr(1)(0) + grid_sr(1)(2) +
                    grid_sr(2)(0) + grid_sr(2)(1) + grid_sr(2)(2)

                /* verilator lint_on WIDTH */
                state :== UPDATE

            case UPDATE =>
                // update cell state
                we    :== 1 // write new cell state next cycle
                ready :== 1 // ready for output next cycle
                /* verilator lint_off WIDTH */
                pixel.x :== cell_x - 1 // correct horizontal position for padding
                pixel.y :== cell_y - 1 // correct vertical position for padding
                /* verilator lint_on WIDTH */

                if (grid_sr(1)(1)) // cell was alive this generation
                    if (neigh_cnt == 2 || neigh_cnt == 3) // still alive
                        data_in :== 1
                        alive   :== 1
                        changed :== 0
                    else // now dead
                        data_in :== 0
                        alive   :== 0
                        changed :== 1
                else // was dead this generation
                if (neigh_cnt == 3) // now alive
                    data_in :== 1
                    alive   :== 1
                    changed :== 1
                else // still dead
                    data_in :== 0
                    alive   :== 0
                    changed :== 0

                // what next?
                if (cell_x == WORLD_WIDTH - 2) // final cell on line
                    if (cell_y == WORLD_HEIGHT - 2) // final line of cells
                        state   :== IDLE
                        running :== 0
                        done    :== 1
                    else state :== NEW_LINE
                else
                    state :== NEW_CELL
                neigh_cnt :== 0

            case NEW_CELL =>
                cell_x    :== cell_x + 1
                cell_id   :== cell_id + 1
                inc_read  :== 1 // incremental read
                read_step :== 6 // read new column of 3 cells (skip A-F)
                state     :== READ

            case NEW_LINE =>
                cell_y    :== cell_y + 1
                cell_x    :== 1
                cell_id   :== cell_id + 3 // skip 2 cells of padding
                read_step :== 0 // read all nine cells at start of line
                state     :== READ

            case _ =>
                if (start)
                    state :== INIT
                else
                    state :== IDLE

        if (rst)
            state   :== IDLE
            ready   :== 0
            alive   :== 0
            changed :== 0
            pixel.x :== 0
            pixel.y :== 0
            running :== 0
            done    :== 0
    }
