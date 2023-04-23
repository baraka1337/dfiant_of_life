import dfhdl.*

class top_life_de10() extends EDDesign:
    val MAX10_CLK1_50 = Bit     <> IN // 100 MHz clock
    val KEY           = Bits(2) <> IN // button (active low)
    val VGA_HS        = Bit     <> OUT // horizontal sync
    val VGA_VS        = Bit     <> OUT // vertical sync
    val VGA_R         = Bits(4) <> OUT // 4-bit VGA red
    val VGA_G         = Bits(4) <> OUT // 4-bit VGA green
    val VGA_B         = Bits(4) <> OUT // 4-bit VGA blue

    val top_life = new TopLife(isSDL = false)

    top_life.btn_rst_n <> KEY(0)
    top_life.clk_50m   <> MAX10_CLK1_50

    process(all) {
        VGA_R :== top_life.vga_color.red
        VGA_G :== top_life.vga_color.green
        VGA_B :== top_life.vga_color.blue
    }

    top_life.vga_hsync <> VGA_HS
    top_life.vga_vsync <> VGA_VS
