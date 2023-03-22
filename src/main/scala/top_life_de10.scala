import dfhdl.*

class top_life_de10() extends EDDesign:
    val MAX10_CLK1_50 = Bit     <> IN // 100 MHz clock
    val KEY           = Bits(2) <> IN // button (active low)
    val VGA_HS        = Bit     <> OUT // horizontal sync
    val VGA_VS        = Bit     <> OUT // vertical sync
    val VGA_R         = UInt(4) <> OUT // 4-bit VGA red
    val VGA_G         = UInt(4) <> OUT // 4-bit VGA green
    val VGA_B         = UInt(4) <> OUT // 4-bit VGA blue

    val top_life = new TopLife(isSDL = false)

    top_life.btn_rst_n <> KEY(0)
    top_life.clk_50m   <> MAX10_CLK1_50

    top_life.vga_r <> VGA_R
    top_life.vga_g <> VGA_G
    top_life.vga_b <> VGA_B

    top_life.vga_hsync <> VGA_HS
    top_life.vga_vsync <> VGA_VS
