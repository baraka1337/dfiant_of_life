import dfhdl.*

class ID extends EDDesign:
    val x = UInt(8) <> IN
    val y = UInt(8) <> OUT
    y <> x

@main def hello: Unit =
    println("Hello, welcome to the DFHDL demo!")
    println(dfhdlVersion)
    println("Printing the top:")
    import backends.verilog.sv2005
    val top = new top_life_de10
    import compiler.stages.StageRunner
    StageRunner.logDebug()
    top.printCodeString
    // .toFolder("sandbox")
    // // .printGenFiles
    // .lint
