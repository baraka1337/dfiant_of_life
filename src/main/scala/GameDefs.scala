import dfhdl.*

object GameDefs:

    val pix_clkCfg = ClkCfg(ClkCfg.Edge.Rising)
    val sys_clkCfg = ClkCfg(ClkCfg.Edge.Rising)
    val sys_rstCfg = RstCfg(
      RstCfg.Mode.Async,
      RstCfg.Active.Low
    )
    val pix_rstCfg = RstCfg(
      RstCfg.Mode.Async,
      RstCfg.Active.Low
    )

    val sysCfg = RTDomainCfg(sys_clkCfg, sys_rstCfg)
    val pixCfg = RTDomainCfg(pix_clkCfg, pix_rstCfg)

    val CORDW      = 16
    val GEN_FRAMES = 5 // each generation lasts this many frames
    val SEED_FILE  = "gosper_gun_64x48.mem" // world seed
    val GRID       = 3 // neighbours are a 3x3 grid
    val STEPS      = 11 // 9 reads and 2 cycles of latency

    // framebuffer (FB)
    val WIDTH     = 64
    val HEIGHT    = 48
    val CIDXW     = 2
    val CHANW     = 4
    val SCALE     = 10
    val F_IMAGE   = ""
    val F_PALETTE = "life_palette.mem"

    val CLUTW = 3 * CHANW

    case class Pixel(
        x: SInt[CORDW.type] <> VAL,
        y: SInt[CORDW.type] <> VAL
    ) extends Struct

    case class PColor(
        red: Bits[CHANW.type] <> VAL,
        green: Bits[CHANW.type] <> VAL,
        blue: Bits[CHANW.type] <> VAL
    ) extends Struct

// object GameDefs1 extends GameDefs(16)
// object GameDefs2 extends GameDefs(8)
