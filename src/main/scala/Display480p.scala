import dfhdl.*
import Utils.*

class Display480p(
    val CORDW: Int     = 16, // signed coordinate width (bits)
    val H_RES: Int     = 640, // horizontal resolution (pixels)
    val V_RES: Int     = 480, // vertical resolution (lines)
    val H_FP: Int      = 16, // horizontal front porch
    val H_SYNC: Int    = 96, // horizontal sync
    val H_BP: Int      = 48, // horizontal back porch
    val V_FP: Int      = 10, // vertical front porch
    val V_SYNC: Int    = 2, // vertical sync
    val V_BP: Int      = 33, // vertical back porch
    val H_POL: Boolean = false, // horizontal sync polarity (0:neg, 1:pos)
    val V_POL: Boolean = false // vertical sync polarity (0:neg, 1:pos)
) extends EDDesign:
  val clk_pix = Bit         <> IN
  val rst_pix = Bit         <> IN
  val hsync   = Bit         <> OUT
  val vsync   = Bit         <> OUT
  val de      = Bit         <> OUT
  val frame   = Bit         <> OUT
  val line    = Bit         <> OUT
  val sx      = SInt(CORDW) <> OUT
  val sy      = SInt(CORDW) <> OUT

  // horizontal timings
  val H_STA  = 0 - H_FP - H_SYNC - H_BP // horizontal start
  val HS_STA = H_STA + H_FP // sync start
  val HS_END = HS_STA + H_SYNC // sync end
  val HA_STA = 0 // active start
  val HA_END = H_RES - 1 // active end

  // vertical timings
  val V_STA  = 0 - V_FP - V_SYNC - V_BP // vertical start
  val VS_STA = V_STA + V_FP // sync start
  val VS_END = VS_STA + V_SYNC // sync end
  val VA_STA = 0 // active start
  val VA_END = V_RES - 1 // active end

  val x = SInt(CORDW) <> VAR init H_STA
  val y = SInt(CORDW) <> VAR init V_STA
  // generate horizontal and vertical sync with correct polarity
  process(clk_pix.rising) {
    if (rst_pix)
      if (H_POL)
        hsync :== (x > HS_STA && y <= HS_END)
      else
        hsync :== !(x > HS_STA && y <= HS_END)

      if (V_POL)
        vsync :== (y > VS_STA && y <= VS_END)
      else
        vsync :== !(y > VS_STA && y <= VS_END)
    else
      hsync :== !H_POL
      vsync :== !V_POL
  }

  // control signals
  process(clk_pix) {
    if (clk_pix.rising)
      if (rst_pix)
        de :== 0
        frame :== 0
        line :== 0
      else
        de :== (y >= VA_STA && x >= HA_STA)
        frame :== (y == V_STA && x == H_STA)
        line :== (x == H_STA)
  }
  // calculate horizontal and vertical screen position
  process(clk_pix.rising) {
    if (x == HA_END) // last pixel on line?
      x :== H_STA
      if (y == VA_END)
        y :== V_STA
      else
        y :== y + 1 // last line on screen?
    else x :== x + 1
    if (rst_pix)
      x :== H_STA
      y :== V_STA
  }

  // delay screen position to match sync and control signals
  process(clk_pix.rising) {
    sx :== x
    sy :== y
    if (rst_pix)
      sx :== H_STA
      sy :== V_STA
  }
