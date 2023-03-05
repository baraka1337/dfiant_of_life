// scalafmt: { align.tokens = [{code = "<>"}, {code = "="}, {code = "=>"}, {code = ":="}]}
import dfhdl.*

class LineBuffer(
    val WIDTH: Int = 8,
    val LEN: Int   = 640,
    val SCALE: Int = 1
) extends EDDesign:
    val clk_in  = Bit     <> IN
    val clk_out = Bit     <> IN
    val rst_in  = Bit     <> IN
    val rst_out = Bit     <> IN
    val en_in   = Boolean <> IN
    val en_out  = Boolean <> IN
    val frame   = Boolean <> IN
    val line    = Boolean <> IN

    val din      = Vector.fill(3)(UInt(WIDTH) <> IN)
    val dout     = Vector.fill(3)(UInt(WIDTH) <> OUT)
    val data_req = Bit <> OUT

    val addr_out, addr_in = UInt.until(LEN) <> VAR
    val cnt_h, cnt_v      = UInt.until(LEN) <> VAR

    val set_end  = Boolean <> VAR
    val get_data = Bit     <> VAR

    process(clk_out.rising) {
        if (frame)
            addr_out :== 0
            cnt_h :== 0
            cnt_v :== 0
            set_end :== 1
        else if (en_out && !set_end)
            if (cnt_h == SCALE - 1)
                cnt_h :== 0
                if (addr_out == LEN - 1)
                    addr_out :== 0
                    if (cnt_v == SCALE - 1)
                        cnt_v :== 0
                        set_end :== 1
                    else cnt_v :== cnt_v + 1
                else addr_out :== addr_out + 1
            else cnt_h :== cnt_h
        else if (get_data)
            set_end :== 0

        if (rst_out)
            addr_out :== 0
            cnt_h :== 0
            cnt_v :== 0
            set_end :== 0
    }

    process(all) {
        get_data := line && set_end
    }

    val xd_req = new XD()
    xd_req.clk_src  <> clk_out
    xd_req.clk_dst  <> clk_in
    xd_req.flag_src <> get_data
    xd_req.flag_dst <> data_req

    process(clk_in.rising) {
        if (en_in)
            if (addr_in == LEN - 1)
                addr_in :== 0
            else
                addr_in :== addr_in + 1
        if (data_req || rst_in)
            addr_in :== 0
    }

    val ch = Vector.fill(3)(new BramSdp(WIDTH = WIDTH, DEPTH = LEN))
    for (i <- 0 until 3) {
        ch(i).clk_write  <> clk_in
        ch(i).clk_read   <> clk_out
        ch(i).we         <> en_in
        ch(i).addr_write <> addr_in
        ch(i).addr_read  <> addr_out
        ch(i).data_in    <> din(i)
        ch(i).data_out   <> dout(i)
    }
