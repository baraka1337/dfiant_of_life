import dfhdl.*
import Utils.*

class Display480p(
    val cfg: RTDomainCfg,
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
) extends RTDesign(cfg):
    val hsync = Bit         <> OUT
    val vsync = Bit         <> OUT
    val de    = Bit         <> OUT
    val frame = Bit         <> OUT
    val line  = Bit         <> OUT
    val sx    = SInt(CORDW) <> OUT
    val sy    = SInt(CORDW) <> OUT

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

    val x = SInt(CORDW) <> REG init H_STA
    val y = SInt(CORDW) <> REG init V_STA

    val hsync_wire = Bit <> WIRE
    val vsync_wire = Bit <> WIRE
    // generate horizontal and vertical sync with correct polarity
    if (H_POL)
        hsync_wire := (x > HS_STA && x <= HS_END)
    else
        hsync_wire := !(x > HS_STA && x <= HS_END)
    hsync          := hsync_wire.reg(1, initValue = !H_POL)

    if (V_POL)
        vsync_wire := (y > VS_STA && y <= VS_END)
    else
        vsync_wire := !(y > VS_STA && y <= VS_END)
    vsync          := vsync_wire.reg(1, initValue = !V_POL)

    // control signals
    de    := (y >= VA_STA && x >= HA_STA).reg(1, initValue = 0)
    frame := (y == V_STA && x == H_STA).reg(1, initValue = 0)
    line  := (x == H_STA).reg(1, initValue = 0)

    // calculate horizontal and vertical screen position
    if (x == HA_END) // last pixel on line?
        x.din := H_STA
        if (y == VA_END)
            y.din := V_STA
        else
            y.din := y + 1 // last line on screen?
    else x.din := x + 1

    // delay screen position to match sync and control signals
    sx := x.reg
    sy := y.reg
