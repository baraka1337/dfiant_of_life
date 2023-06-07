import dfhdl.*
import GameDefs.*

class LineBuffer(
    val WIDTH: Int = 8,
    val LEN: Int   = 640,
    val SCALE: Int = 1,
    val inCfg: RTDomainCfg,
    val outCfg: RTDomainCfg
) extends RTDesign:

    val out = new RTDomain(outCfg):
        val data     = Vector.fill(3)(UInt(WIDTH) <> OUT)
        val en       = Boolean         <> IN
        val frame    = Boolean         <> IN
        val line     = Boolean         <> IN
        val addr     = UInt.until(LEN) <> REG init 0
        val cnt_v    = UInt.until(LEN) <> REG init 0
        val cnt_h    = UInt.until(LEN) <> REG init 0
        val set_end  = Boolean         <> REG init 0
        val get_data = Bit             <> WIRE

        if (frame)
            addr.din    := 0
            cnt_h.din   := 0
            cnt_v.din   := 0
            set_end.din := 1
        else if (en && !set_end)
            if (cnt_h == SCALE - 1)
                cnt_h.din := 0
                if (addr == LEN - 1)
                    addr.din := 0
                    if (cnt_v == SCALE - 1)
                        cnt_v.din   := 0
                        set_end.din := 1
                    else cnt_v.din := cnt_v + 1
                else addr.din := addr + 1
            else cnt_h.din := cnt_h + 1
        else if (get_data)
            set_end.din := 0

        get_data := line && set_end

    val in = new RTDomain(inCfg):
        val data     = Vector.fill(3)(UInt(WIDTH) <> IN)
        val data_req = Bit             <> OUT
        val addr     = UInt.until(LEN) <> REG init 0
        val en       = Boolean         <> IN
        if (en)
            if (addr == LEN - 1)
                addr.din := 0
            else
                addr.din := addr + 1
        if (data_req)
            addr.din := 0

    val xd_req = new XD(srcCfg = outCfg, dstCfg = inCfg)
    xd_req.src.flag <> out.get_data
    xd_req.dst.flag <> in.data_req

    val ch = Vector.fill(3)(
      new BramSdp(
        WIDTH    = WIDTH,
        DEPTH    = LEN,
        writeCfg = inCfg,
        readCfg  = outCfg
      )
    )
    for (i <- 0 until 3) {
        ch(i).write.en   <> in.en
        ch(i).write.addr <> in.addr
        ch(i).read.addr  <> out.addr
        ch(i).write.data <> in.data(i)
        ch(i).read.data  <> out.data(i)
    }
