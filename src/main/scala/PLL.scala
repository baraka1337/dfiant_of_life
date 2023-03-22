import dfhdl.*

class pll extends EDBlackBox(EDBlackBox.Source.NA, EDBlackBox.Source.NA):
    val areset = Bit <> IN
    val inclk0 = Bit <> IN
    val c0     = Bit <> OUT
    val c1     = Bit <> OUT
    val locked = Bit <> OUT
