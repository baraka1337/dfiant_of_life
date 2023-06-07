import dfhdl.*
import Utils.binaryFileToIntVector

class BramSdp(
    val WIDTH: Int     = 8,
    val DEPTH: Int     = 256,
    val INIT_F: String = "",
    val writeCfg: RTDomainCfg,
    val readCfg: RTDomainCfg
) extends RTDesign:

    val write = new RTDomain(writeCfg):
        val en     = Boolean             <> IN
        val addr   = UInt.until(DEPTH)   <> IN
        val data   = UInt(WIDTH)         <> IN
        val memory = UInt(WIDTH) X DEPTH <> VAR init binaryFileToIntVector(INIT_F, WIDTH, DEPTH) @@ INIT_F.nonEmpty

        if (en)
            memory(addr) := data

    val read = new RTDomain(readCfg):
        val addr = UInt.until(DEPTH) <> IN
        val data = UInt(WIDTH)       <> OUT
        data := write.memory(addr)
