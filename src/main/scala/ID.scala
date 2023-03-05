import dfhdl.*

class ID extends EDDesign:
    val x = UInt(8) <> IN
    val y = UInt(8) <> OUT
    y <> x

@main def hello: Unit =
    println("Hello, welcome to the DFHDL demo!")
    println("Printing the top:")
    import backends.verilog.sv2005
    val top = new ID
    // val top = new TopLife
    import compiler.stages.StageRunner
    StageRunner.logDebug()
    top.compile
        .toFolder("sandbox")
        .printGenFiles
        .lint
