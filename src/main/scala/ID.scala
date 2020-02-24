import DFiant._ //Required in any DFiant compilation program

trait ID extends DFDesign { //This our `ID` dataflow design
  val x = DFSInt[16] <> IN  //The input port is a signed 16-bit integer
  val y = DFSInt[16] <> OUT //The output port is a signed 16-bit integer
  y := x //Trivial direct input-to-output assignment
}

object IDApp extends DFApp.VHDLCompiler[ID](DFApp.Config.Print) //The ID compilation program entry-point