import dfhdl.*
import Utils.binaryFileToIntVector

class BramSdp(
    val WIDTH: Int     = 8,
    val DEPTH: Int     = 256,
    val INIT_F: String = ""
) extends EDDesign:
    val clk_write  = Bit               <> IN
    val clk_read   = Bit               <> IN
    val we         = Boolean           <> IN
    val addr_write = UInt.until(DEPTH) <> IN
    val addr_read  = UInt.until(DEPTH) <> IN
    val data_in    = UInt(WIDTH)       <> IN
    val data_out   = UInt(WIDTH)       <> OUT

    val memory = if (INIT_F.isEmpty) {
        UInt(WIDTH) X DEPTH <> VAR setName "memory"
    } else {
        UInt(WIDTH) X DEPTH <> VAR init binaryFileToIntVector(INIT_F, WIDTH, DEPTH) setName "memory"
    }

    process(clk_write.rising) {
        if (we)
            memory(addr_write) :== data_in
    }

    process(clk_read.rising) {
        data_out :== memory(addr_read)
    }
