import dfhdl.*
import Utils.binaryFileToIntVector

class BramSdp(
    val WIDTH: Int = 8,
    val DEPTH: Int = 256,
    val INIT_F: String = ""
) extends EDDesign:
  val clk_write, clk_read = Bit <> IN
  val we = Boolean <> IN
  val addr_write, addr_read = UInt.until(DEPTH) <> IN
  val data_in = UInt(WIDTH) <> IN
  val data_out = UInt(WIDTH) <> OUT

  val memory =
    UInt(WIDTH) X DEPTH <> VAR init binaryFileToIntVector(INIT_F, WIDTH, DEPTH)

  process(clk_write) {
    if (clk_write.rising)
      if (we)
        memory(addr_write) := data_in
  }

  process(clk_read) {
    if (clk_read.rising)
      data_out := memory(addr_read)
  }
