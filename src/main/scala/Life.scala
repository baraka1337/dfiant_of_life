import dfhdl.*
import Utils.*

class Life(
    val CORDW: Int     = 16, // signed coordinate width
    val WIDTH: Int     = 6, // world width in cells
    val HEIGHT: Int    = 6, // world height in cells
    val F_INIT: String = "" // initial world state
) extends EDDesign:
    val clk     = Bit         <> IN // clock
    val rst     = Boolean     <> IN // reset
    val start   = Boolean     <> IN // start generation
    val ready   = Boolean     <> OUT // cell state ready to be read
    val alive   = Bit         <> OUT // is the cell alive? (when ready)
    val changed = Bit         <> OUT // cell's state changed (when ready)
    val x       = SInt(CORDW) <> OUT
    val y       = SInt(CORDW) <> OUT
    val running = Bit         <> OUT // life is running
    val done    = Bit         <> OUT // generation complete (high for one tick)

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
      INIT_F = F_INIT
    )

    bram_inst.clk_write  <> clk
    bram_inst.clk_read   <> clk
    bram_inst.we         <> we
    bram_inst.addr_write <> addr_write_offs
    bram_inst.addr_read  <> addr_read_offs
    bram_inst.data_in    <> data_in
    bram_inst.data_out   <> data_out

    // cell coordinates
    val GRID: Int  = 3 // neighbours are a 3x3 grid
    val STEPS: Int = 11 // 9 reads and 2 cycles of latency
    val cell_x     = UInt.until(WORLD_WIDTH)  <> VAR // active cell (horizontal)
    val cell_y     = UInt.until(WORLD_HEIGHT) <> VAR // active cell (vertical)
    val read_step  = UInt.until(STEPS)        <> VAR // reading step
    val inc_read   = Bit                      <> VAR // perform incremental read
    val top_sr     = Bits(GRID)               <> VAR // shift reg for neighbours
    val mid_sr     = Bits(GRID)               <> VAR
    val bot_sr     = Bits(GRID)               <> VAR
    val neigh_cnt  = UInt.until(GRID * GRID)  <> VAR // count of neighbours

    enum State extends Encode:
        case IDLE, INIT, READ, NEIGH, UPDATE, NEW_CELL, NEW_LINE
    import State.*
    val state = State <> VAR init IDLE
    // life generation state
    process(clk.rising) {
        // single-cycle flags: 0 by default
        ready :== 0
        we    :== 0
        done  :== 0

        state match
            case INIT =>
                read_step :== 0
                inc_read  :== 0
                top_sr    :== all(0)
                mid_sr    :== all(0)
                bot_sr    :== all(0)
                neigh_cnt :== 0
                state     :== READ
                running   :== 1

                // first cell after padding
                cell_x  :== 1
                cell_y  :== 1
                cell_id :== WORLD_WIDTH + 1

            case READ =>
                // 1 cycle to set address and 1 cycle BRAM read latency
                read_step match
                    case 0 =>
                        addr_read :== cell_id - WORLD_WIDTH - 1 // A

                    case 1 =>
                        addr_read :== cell_id - 1 // B

                    case 2 =>
                        addr_read             :== cell_id + WORLD_WIDTH - 1 // C
                        if (!inc_read) top_sr :== (top_sr(1, 0), data_out) // A

                    case 3 =>
                        addr_read             :== cell_id - WORLD_WIDTH // D
                        if (!inc_read) mid_sr :== (mid_sr(1, 0), data_out) // B

                    case 4 =>
                        addr_read             :== cell_id // E
                        if (!inc_read) bot_sr :== (bot_sr(1, 0), data_out) // C

                    case 5 =>
                        addr_read             :== cell_id + WORLD_WIDTH // F
                        if (!inc_read) top_sr :== (top_sr(1, 0), data_out) // D

                    case 6 =>
                        addr_read             :== cell_id - WORLD_WIDTH + 1 // G
                        if (!inc_read) mid_sr :== (mid_sr(1, 0), data_out) // E

                    case 7 =>
                        addr_read             :== cell_id + 1 // H
                        if (!inc_read) bot_sr :== (bot_sr(1, 0), data_out) // F

                    case 8 =>
                        addr_read :== cell_id + WORLD_WIDTH + 1 // I
                        top_sr    :== (top_sr(1, 0), data_out) // G

                    case 9 =>
                        mid_sr :== (mid_sr(1, 0), data_out) // H

                    case 10 =>
                        bot_sr :== (bot_sr(1, 0), data_out) // I

                    case _ => addr_read :== 0

                if (read_step == STEPS - 1)
                    state :== NEIGH
                else
                    read_step :== read_step + 1
            case NEIGH =>
                /* verilator lint_off WIDTH */
                neigh_cnt :== top_sr(0, 0).resize(neigh_cnt.width) + top_sr(1) +
                    top_sr(2) + mid_sr(0) + mid_sr(2) +
                    bot_sr(0) + bot_sr(1) + bot_sr(2)

                /* verilator lint_on WIDTH */
                state :== UPDATE

            case UPDATE =>
                // update cell state
                we    :== 1 // write new cell state next cycle
                ready :== 1 // ready for output next cycle
                /* verilator lint_off WIDTH */
                x :== cell_x - 1 // correct horizontal position for padding
                y :== cell_y - 1 // correct vertical position for padding
                /* verilator lint_on WIDTH */

                if (mid_sr(1)) // cell was alive this generation
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
            x       :== 0
            y       :== 0
            running :== 0
            done    :== 0
    }
