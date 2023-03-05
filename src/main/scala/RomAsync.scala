import dfhdl.*
import Utils.*

class RomAsync(
    val WIDTH: Int     = 8,
    val DEPTH: Int     = 256,
    val INIT_F: String = ""
) extends EDDesign:
    val addr = UInt.until(DEPTH) <> IN
    val data = UInt(WIDTH)       <> OUT

    val memory =
        UInt(WIDTH) X DEPTH <> VAR init hexFileToIntVector(INIT_F, WIDTH, DEPTH)

    process(all) {
        data := memory(addr)
    }
