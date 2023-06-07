import dfhdl.*
import scala.math._
import Utils.*
import GameDefs.*

class FramebufferBram extends RTDesign(sysCfg):
    val clk_sys = Bit         <> IN
    val clk_pix = Bit         <> IN
    val rst_sys = Bit         <> IN
    val rst_pix = Bit         <> IN
    val de      = Bit         <> IN
    val frame   = Bit         <> IN
    val line    = Bit         <> IN
    val we      = Boolean     <> IN
    val pixel   = Pixel       <> IN
    val cidx    = UInt(CIDXW) <> IN

    val busy     = Bit <> OUT
    val busy_reg = Bit <> REG
    busy := busy_reg
    val clip     = Bit <> OUT
    val clip_reg = Bit <> REG
    clip := clip_reg
    val color = PColor <> OUT

    val frame_sys = Bit <> WIRE
    val xd_frame = new XD(
      srcCfg = pixCfg,
      dstCfg = sysCfg
    )
    xd_frame.src.flag <> frame
    xd_frame.dst.flag <> frame_sys

    val FB_PIXELS   = WIDTH * HEIGHT
    val FB_DATAW    = CIDXW
    val FB_DUALPORT = 1

    val fb_addr_read, fb_addr_write, fb_addr_line = UInt.until(FB_PIXELS) <> REG
    val fb_cidx_read                              = UInt(FB_DATAW)        <> WIRE
    val fb_cidx_read_p1                           = UInt(FB_DATAW)        <> REG(pixCfg)

    val x_add = SInt(CORDW) <> REG

    fb_addr_line.din := (pixel.y.bits * WIDTH).resize(fb_addr_line.width)
    x_add.din        := pixel.x
    fb_addr_write.din := (x_add + fb_addr_line.signed).bits
        .resize(fb_addr_write.width)
        .uint

    val we_in_p1 = Boolean <> REG
    val fb_we    = Boolean <> REG
    // val fb_we, we_in_p1 = Boolean <> VAR
    val fb_cidx_write, cidx_in_p1 = UInt(FB_DATAW) <> REG

    we_in_p1.din   := we
    cidx_in_p1.din := cidx
    clip_reg.din   := pixel.y < 0 || pixel.y >= HEIGHT || pixel.x < 0 || pixel.x >= HEIGHT
    if (busy || clip)
        fb_we.din := 0
    else
        fb_we.din     := we_in_p1
    fb_cidx_write.din := cidx_in_p1

    val bram_inst = new BramSdp(
      WIDTH    = FB_DATAW,
      DEPTH    = FB_PIXELS,
      INIT_F   = F_IMAGE,
      writeCfg = sysCfg,
      readCfg  = sysCfg
    )

    bram_inst.write.en   <> fb_we
    bram_inst.write.addr <> fb_addr_write
    bram_inst.read.addr  <> fb_addr_read
    bram_inst.write.data <> fb_cidx_write
    bram_inst.read.data  <> fb_cidx_read

    val LB_SCALE = SCALE
    val LB_LEN   = WIDTH

    val lb_data_req = Boolean                <> WIRE
    val cnt_h       = UInt.until(LB_LEN + 1) <> REG

    val lb_en_in, lb_en_out = Boolean <> WIRE
    lb_en_in  := cnt_h < LB_LEN
    lb_en_out := de

    val LAT         = 3
    val lb_en_in_sr = Bits(LAT) <> REG

    if (rst_sys)
        lb_en_in_sr.din := all(0)
    else
        lb_en_in_sr.din := (lb_en_in, lb_en_in_sr(LAT - 1, 1))

    if (fb_addr_read < FB_PIXELS - 1)
        if (lb_data_req)
            cnt_h.din := 0
            if (FB_DUALPORT != 0)
                busy_reg.din := 1
        else if (cnt_h < LB_LEN)
            cnt_h.din        := cnt_h + 1
            fb_addr_read.din := fb_addr_read + 1
    else
        cnt_h.din := LB_LEN

    if (frame_sys.bool)
        fb_addr_read.din := 0
        busy_reg.din     := 0
    if (rst_sys)
        fb_addr_read.din := 0
        busy_reg.din     := 0
        cnt_h.din        := LB_LEN
    if (lb_en_in_sr == b"100")
        busy_reg.din := 0

    val lb_in_0 = Bits(CHANW) <> REG
    val lb_in_1 = Bits(CHANW) <> REG
    val lb_in_2 = Bits(CHANW) <> REG

    val lb_inst = new LineBuffer(
      WIDTH  = CHANW,
      LEN    = LB_LEN,
      SCALE  = LB_SCALE,
      inCfg  = sysCfg,
      outCfg = pixCfg
    )

    lb_data_req        <> lb_inst.in.data_req
    lb_inst.in.en      <> lb_en_in_sr(0)
    lb_inst.out.en     <> lb_en_out
    lb_inst.out.frame  <> frame
    lb_inst.out.line   <> line
    lb_inst.in.data(0) <> lb_in_0
    lb_inst.in.data(1) <> lb_in_1
    lb_inst.in.data(2) <> lb_in_2

    fb_cidx_read_p1.din := fb_cidx_read

    val clut_colr = Bits(CLUTW) <> WIRE
    val clut = new RomAsync(
      WIDTH  = CLUTW,
      DEPTH  = pow(2, CIDXW).toInt,
      INIT_F = F_PALETTE
    )

    clut.addr <> fb_cidx_read_p1
    clut_colr <> clut.data

    val lb_en_out_p1 = Boolean <> REG

    lb_in_0.din := clut_colr(CHANW - 1, 0)
    lb_in_1.din := clut_colr(2 * CHANW - 1, CHANW)
    lb_in_2.din := clut_colr(3 * CHANW - 1, 2 * CHANW)

    lb_en_out_p1.din := lb_en_out

    if (lb_en_out_p1)
        color.red   := lb_inst.out.data(2)
        color.green := lb_inst.out.data(1)
        color.blue  := lb_inst.out.data(0)
    else
        color.red   := all(0)
        color.green := all(0)
        color.blue  := all(0)
