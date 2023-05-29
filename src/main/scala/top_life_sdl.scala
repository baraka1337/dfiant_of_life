import dfhdl.*
import GameDefs.*

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

    val pixel = Pixel <> VAR

    val top_life = new TopLife(isSDL = true)

    process(all) {
        sdl_sx := pixel.x.bits.resize(CORDW)
        sdl_sy := pixel.y.bits.resize(CORDW)

        sdl_r := top_life.vga_color.red.bits.repeat(2)
        sdl_g := top_life.vga_color.green.bits.repeat(2)
        sdl_b := top_life.vga_color.blue.bits.repeat(2)
    }

    top_life.sdl_pixel <> pixel
    top_life.de        <> sdl_de
    top_life.clk_50m   <> sim_clk
    top_life.vga_hsync <> OPEN
    top_life.vga_vsync <> OPEN
