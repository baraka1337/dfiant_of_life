import dfhdl.*

class TopLife(
    val isSDL: Boolean = false
) extends EDDesign:
    val clk_50m   = Bit      <> IN // 100 MHz clock
    val btn_rst_n = Bit      <> IN // reset button (active low)
    val vga_hsync = Bit      <> OUT // horizontal sync
    val vga_vsync = Bit      <> OUT // vertical sync
    val sx        = SInt(16) <> OUT // horizontal SDL position
    val sy        = SInt(16) <> OUT // vertical SDL position
    val de        = Bit      <> OUT
    val vga_r     = UInt(4)  <> OUT // 4-bit VGA red
    val vga_g     = UInt(4)  <> OUT // 4-bit VGA green
    val vga_b     = UInt(4)  <> OUT // 4-bit VGA blue

    val de_local   = Bit <> VAR
    val GEN_FRAMES = 5 // each generation lasts this many frames
    val SEED_FILE  = "gosper_gun_64x48.mem" // world seed
    // localparam SEED_FILE = "gosper_gun_64x48.mem"  // world seed

    // generate pixel clock
    val clk_pix        = Bit <> VAR
    val clk_100m       = Bit <> VAR
    val clk_pix_locked = Bit <> VAR
    val rst_pix        = Bit <> VAR

    if (isSDL) then
        process(all) {
            clk_pix_locked := 1
            clk_100m       := clk_50m
            clk_pix        := clk_50m
        }
    else
        val clock_inst = new pll
        clock_inst.areset <> !btn_rst_n
        clock_inst.inclk0 <> clk_50m
        clock_inst.c0     <> clk_pix
        clock_inst.c1     <> clk_100m
        clock_inst.locked <> clk_pix_locked

    process(clk_pix.rising) {
        rst_pix :== !clk_pix_locked // wait for clock lock
    }

    // display sync signals and coordinates
    val CORDW        = 16
    val hsync, vsync = Bit <> VAR
    val frame, line  = Bit <> VAR

    process(all) {
        de := de_local
    }

    val display_inst = new Display480p(
      CORDW = CORDW
    )
    display_inst.clk_pix <> clk_pix
    display_inst.rst_pix <> rst_pix
    display_inst.sx      <> sx
    display_inst.sy      <> sy
    display_inst.de      <> de_local
    display_inst.hsync   <> hsync
    display_inst.vsync   <> vsync
    display_inst.frame   <> frame
    display_inst.line    <> line

    val frame_sys = Bit <> VAR // start of new frame in system clock domain
    val xd_frame  = new XD
    xd_frame.clk_src  <> clk_pix
    xd_frame.clk_dst  <> clk_100m
    xd_frame.flag_src <> frame
    xd_frame.flag_dst <> frame_sys

    // life signals
    /* verilator lint_off UNUSED */
    val life_start               = Bit <> VAR
    val life_alive, life_changed = Bit <> VAR
    /* verilator lint_on UNUSED */

    // start life generation in blanking every GEN_FRAMES
    val cnt_frames = UInt.until(GEN_FRAMES) <> VAR
    process(clk_100m.rising) {
        life_start :== 0
        if (frame_sys)
            if (cnt_frames == GEN_FRAMES - 1)
                life_start :== 1
                cnt_frames :== 0
            else cnt_frames :== cnt_frames + 1

    }

    // framebuffer (FB)
    val FB_WIDTH   = 64
    val FB_HEIGHT  = 48
    val FB_CIDXW   = 2
    val FB_CHANW   = 4
    val FB_SCALE   = 10
    val FB_IMAGE   = ""
    val FB_PALETTE = "life_palette.mem"

    val fb_we    = Bit            <> VAR
    val fbx, fby = SInt(CORDW)    <> VAR // framebuffer coordinates
    val fb_cidx  = Bits(FB_CIDXW) <> VAR
    /* verilator lint_off UNUSED */
    val fb_busy = Bit <> VAR // when framebuffer is busy it cannot accept writes
    /* verilator lint_on UNUSED */
    val fb_red, fb_green, fb_blue = UInt(FB_CHANW) <> VAR // colours for display

    val fb_inst = new FramebufferBram(
      WIDTH     = FB_WIDTH,
      HEIGHT    = FB_HEIGHT,
      CIDXW     = FB_CIDXW,
      CHANW     = FB_CHANW,
      SCALE     = FB_SCALE,
      F_IMAGE   = FB_IMAGE,
      F_PALETTE = FB_PALETTE
    )
    fb_inst.clk_sys <> clk_100m
    fb_inst.clk_pix <> clk_pix
    fb_inst.rst_sys <> 0
    fb_inst.rst_pix <> 0
    fb_inst.de      <> de_local
    // de              <> fb_inst.de
    fb_inst.frame <> frame
    fb_inst.line  <> line
    fb_inst.we    <> fb_we
    fb_inst.x     <> fbx
    fb_inst.y     <> fby
    fb_inst.cidx  <> fb_cidx
    fb_inst.busy  <> fb_busy
    // fb_inst.clip <>
    fb_inst.red   <> fb_red
    fb_inst.green <> fb_green
    fb_inst.blue  <> fb_blue

    // select colour based on cell state
    process(all) {
        fb_cidx := life_alive.bits.resize(fb_cidx.width)
        // fb_cidx(0) :== life_alive
        // fb_cidx(1) :== 0
    }

    val life_inst = new Life(
      CORDW  = CORDW,
      WIDTH  = FB_WIDTH,
      HEIGHT = FB_HEIGHT,
      F_INIT = SEED_FILE
    )
    life_inst.clk     <> clk_100m // clock
    life_inst.rst     <> false // reset
    life_inst.start   <> life_start // start generation
    fb_we             <> life_inst.ready // cell state ready to be read
    life_inst.alive   <> life_alive // is the cell alive? (when ready)
    life_inst.changed <> life_changed // cell's state changed (when ready)
    life_inst.x       <> fbx // horizontal cell position
    life_inst.y       <> fby // vertical cell position

    // reading from FB takes one cycle: delay display signals to match

    val hsync_p1, vsync_p1 = Bit <> VAR
    process(clk_pix.rising) {
        hsync_p1 :== hsync
        vsync_p1 :== vsync
    }

    // VGA output
    process(clk_pix.rising) {
        vga_hsync :== hsync_p1
        vga_vsync :== vsync_p1
        vga_r     :== fb_red
        vga_g     :== fb_green
        vga_b     :== fb_blue
    }
