import dfhdl.*
import scala.math._
import Utils.*

class FramebufferBram(
    val CORDW: Int        = 16,
    val WIDTH: Int        = 320,
    val HEIGHT: Int       = 180,
    val CIDXW: Int        = 4,
    val CHANW: Int        = 4,
    val SCALE: Int        = 4,
    val F_IMAGE: String   = "",
    val F_PALETTE: String = ""
) extends EDDesign:
    val clk_sys = Bit         <> IN
    val clk_pix = Bit         <> IN
    val rst_sys = Bit         <> IN
    val rst_pix = Bit         <> IN
    val de      = Bit         <> IN
    val frame   = Bit         <> IN
    val line    = Bit         <> IN
    val we      = Boolean     <> IN
    val x       = SInt(CORDW) <> IN
    val y       = SInt(CORDW) <> IN
    val cidx    = UInt(CIDXW) <> IN

    val busy  = Bit         <> OUT
    val clip  = Bit         <> OUT
    val red   = UInt(CHANW) <> OUT
    val green = UInt(CHANW) <> OUT
    val blue  = UInt(CHANW) <> OUT

    val frame_sys = Bit <> VAR
    val xd_frame  = new XD
    xd_frame.clk_src  <> clk_pix
    xd_frame.clk_dst  <> clk_sys
    xd_frame.flag_src <> frame
    xd_frame.flag_dst <> frame_sys

    val FB_PIXELS   = WIDTH * HEIGHT
    val FB_DATAW    = CIDXW
    val FB_DUALPORT = 1

    val fb_addr_read, fb_addr_write, fb_addr_line = UInt.until(FB_PIXELS) <> VAR
    val fb_cidx_read, fb_cidx_read_p1             = UInt(FB_DATAW)        <> VAR

    val x_add = SInt(CORDW) <> VAR

    process(clk_sys.rising) {
        fb_addr_line :== (y.bits * WIDTH).resize(fb_addr_line.width)
        x_add        :== x
        fb_addr_write :== (x_add + fb_addr_line.signed).bits
            .resize(fb_addr_write.width)
            .uint
    }

    val we_in_p1 = Boolean <> VAR
    val fb_we    = Boolean <> VAR
    // val fb_we, we_in_p1 = Boolean <> VAR
    val fb_cidx_write, cidx_in_p1 = UInt(FB_DATAW) <> VAR

    process(clk_sys.rising) {
        we_in_p1   :== we
        cidx_in_p1 :== cidx
        clip       :== y < 0 || y >= HEIGHT || x < 0 || x >= HEIGHT
        if (busy || clip)
            fb_we :== 0
        else
            fb_we     :== we_in_p1
        fb_cidx_write :== cidx_in_p1
    }

    val bram_inst = new BramSdp(
      WIDTH  = FB_DATAW,
      DEPTH  = FB_PIXELS,
      INIT_F = F_IMAGE
    )

    bram_inst.clk_write  <> clk_sys
    bram_inst.clk_read   <> clk_sys
    bram_inst.we         <> fb_we
    bram_inst.addr_write <> fb_addr_write
    fb_addr_read         <> bram_inst.addr_read
    bram_inst.data_in    <> fb_cidx_write
    bram_inst.data_out   <> fb_cidx_read

    val LB_SCALE = SCALE
    val LB_LEN   = WIDTH
    val LB_BPC   = CHANW

    val lb_data_req = Boolean                <> VAR
    val cnt_h       = UInt.until(LB_LEN + 1) <> VAR

    val lb_en_in, lb_en_out = Boolean <> VAR
    process(all) {
        lb_en_in  := cnt_h < LB_LEN
        lb_en_out := de
    }

    val LAT         = 3
    val lb_en_in_sr = Bits(LAT) <> VAR

    process(clk_sys.rising) {
        if (rst_sys)
            lb_en_in_sr :== all(0)
        else
            lb_en_in_sr :== (lb_en_in, lb_en_in_sr(LAT - 1, 1))
    }

    process(clk_sys.rising) {
        if (fb_addr_read < FB_PIXELS - 1)
            if (lb_data_req)
                cnt_h :== 0
                if (FB_DUALPORT != 0)
                    busy :== 1
            else if (cnt_h < LB_LEN)
                cnt_h        :== cnt_h + 1
                fb_addr_read :== fb_addr_read + 1
        else
            cnt_h :== LB_LEN

        if (frame_sys.bool)
            fb_addr_read :== 0
            busy         :== 0
        if (rst_sys)
            fb_addr_read :== 0
            busy         :== 0
            cnt_h        :== LB_LEN
        if (lb_en_in_sr == b"100")
            busy :== 0
    }

    val lb_in_0 = Bits(LB_BPC) <> VAR
    val lb_in_1 = Bits(LB_BPC) <> VAR
    val lb_in_2 = Bits(LB_BPC) <> VAR

    val lb_inst = new LineBuffer(
      WIDTH = LB_BPC,
      LEN   = LB_LEN,
      SCALE = LB_SCALE
    )

    lb_inst.clk_in  <> clk_sys
    lb_inst.clk_out <> clk_pix
    lb_inst.rst_out <> rst_sys
    lb_inst.rst_in  <> rst_pix
    lb_data_req     <> lb_inst.data_req
    lb_inst.en_in   <> lb_en_in_sr(0)
    lb_inst.en_out  <> lb_en_out
    lb_inst.frame   <> frame
    lb_inst.line    <> line
    lb_inst.din(0)  <> lb_in_0
    lb_inst.din(1)  <> lb_in_1
    lb_inst.din(2)  <> lb_in_2

    process(clk_sys.rising) {
        fb_cidx_read_p1 :== fb_cidx_read
    }

    val CLUTW     = 3 * CHANW
    val clut_colr = Bits(CLUTW) <> VAR
    val clut = new RomAsync(
      WIDTH  = CLUTW,
      DEPTH  = pow(2, CIDXW).toInt,
      INIT_F = F_PALETTE
    )

    clut.addr <> fb_cidx_read_p1
    clut_colr <> clut.data

    val lb_en_out_p1 = Boolean <> VAR

    process(clk_sys.rising) {
        lb_inst.din(0)
        lb_in_0 :== clut_colr(CHANW - 1, 0)
        lb_in_1 :== clut_colr(2 * CHANW - 1, CHANW)
        lb_in_2 :== clut_colr(3 * CHANW - 1, 2 * CHANW)
    }

    process(clk_pix.rising) {
        lb_en_out_p1 :== lb_en_out
    }

    process(all) {
        if (lb_en_out_p1)
            red   := lb_inst.dout(2)
            green := lb_inst.dout(1)
            blue  := lb_inst.dout(0)
        else
            red   := 0
            green := 0
            blue  := 0
    }
