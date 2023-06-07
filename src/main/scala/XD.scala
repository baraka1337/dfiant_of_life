import dfhdl.*
import dfhdl.core.RTDomain

class XD(srcCfg: RTDomainCfg, dstCfg: RTDomainCfg) extends RTDesign:
    
    val src = new RTDomain(srcCfg):
        val flag   = Bit <> IN
        val toggle = Bit <> REG init 0
        toggle.din := toggle ^ flag

    val dst = new RTDomain(dstCfg):
        val flag = Bit     <> OUT
        val shr  = Bits(4) <> REG init all(0)
        shr.din := (shr(2, 0), src.toggle)

        flag := shr(3) ^ shr(2)
