import dfhdl.*

class top_life_sdl(
    val CORDW: Int     = 10,
    val isSDL: Boolean = false
) extends EDDesign:
    val sim_clk = Bit         <> IN // 100 MHz clock
    val sdl_sx  = UInt(CORDW) <> OUT // horizontal SDL position
    val sdl_sy  = UInt(CORDW) <> OUT // vertical SDL position
    val sdl_de  = Bit         <> OUT
    val sdl_r   = UInt(8)     <> OUT // 8-bit VGA red
    val sdl_g   = UInt(8)     <> OUT // 8-bit VGA green
    val sdl_b   = UInt(8)     <> OUT // 8-bit VGA blue

    val vga_r = UInt(4) <> VAR
    val vga_g = UInt(4) <> VAR
    val vga_b = UInt(4) <> VAR

    val sx = SInt(16) <> VAR
    val sy = SInt(16) <> VAR

    process(all) {
        sdl_sx := sx.bits.resize(CORDW)
        sdl_sy := sy.bits.resize(CORDW)

        sdl_r := vga_r.bits.repeat(2)
        sdl_g := vga_g.bits.repeat(2)
        sdl_b := vga_b.bits.repeat(2)
    }

    val top_life = new TopLife(isSDL = true)

    top_life.sx      <> sx
    top_life.sy      <> sy
    top_life.de      <> sdl_de
    top_life.clk_50m <> sim_clk

    top_life.vga_r <> vga_r
    top_life.vga_g <> vga_g
    top_life.vga_b <> vga_b
