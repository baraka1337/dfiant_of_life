import DFiant.*

class ID(using DFC) extends RTDesign:
  val x = DFUInt(8) <> IN 
  val y = DFUInt(8) <> OUT
  y := x.reg

@main def hello: Unit = 
  import DFiant.compiler.stages.printCodeString
  println("Hello, welcome to the DFiant demo!")
  println("Printing the top:")
  val top = new ID
  top.printCodeString