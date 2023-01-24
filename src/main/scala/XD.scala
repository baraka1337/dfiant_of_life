import dfhdl.*

class XD extends EDDesign:
  val clk_src  = Bit <> IN
  val clk_dst  = Bit <> IN
  val flag_src = Bit <> IN
  val flag_dst = Bit <> OUT

  val toggle_src = Bit     <> VAR init 0
  val shr_dst    = Bits(4) <> VAR init all(0)

  process(clk_src.rising) {
    toggle_src :== toggle_src ^ flag_src
  }

  process(clk_dst.rising) {
    shr_dst :== (shr_dst(2, 0), toggle_src)
  }

  process(all) {
    flag_dst := shr_dst(3) ^ shr_dst(2)
  }
