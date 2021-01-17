package miniplc0java.analyser;


import miniplc0java.instruction.FnInstruction;
import miniplc0java.instruction.Instruction;
import miniplc0java.instruction.Operation;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;

public class out {
    public static void Out(PrintStream output, ArrayList<String> globalV, ArrayList<FnInstruction> fnList) throws Exception{
        output.write(intToByte(0x72303b3e));
        output.write(intToByte(0x1));

        output.write(intToByte(globalV.size()));

        for (String s : globalV) { //全局
            if (s.equals("1")) {
                output.write(0);
                output.write(intToByte(8));
                output.write(longToByte(0L));
            } else if (s.equals("0")) {
                output.write(1);
                output.write(intToByte(8));
                output.write(longToByte(0L));
            } else { //函数名、字符串
                output.write(1);
                output.write(s.length());
                output.write(s.getBytes());
            }
        }

        output.write(intToByte(fnList.size()));// functions.count

        for (FnInstruction fnInstruction : fnList) { //function
            output.write(intToByte(fnInstruction.getName()));
            output.write(intToByte(fnInstruction.getRet_slots()));
            output.write(intToByte(fnInstruction.getParam_slots()));
            output.write(intToByte(fnInstruction.getLoc_slots()));
            output.write(intToByte(fnInstruction.getBodyCount()));

            ArrayList<Instruction> fnInstructions = fnInstruction.getBodyItem();

            for (Instruction instruction : fnInstructions) {
                output.write(instruction.getOpt().getI());
                if (instruction.getValue() != null) {
                    if (instruction.getOpt() == Operation.push) {
                        output.write(longToByte((long) instruction.getValue()));
                    } else {
                        output.write(intToByte((int) instruction.getValue()));
                    }
                }
            }
        }
    }

    public static byte[] longToByte(long val) {
        byte[] b = new byte[8];
        b[7] = (byte) (val & 0xff);
        b[6] = (byte) ((val >> 8) & 0xff);
        b[5] = (byte) ((val >> 16) & 0xff);
        b[4] = (byte) ((val >> 24) & 0xff);
        b[3] = (byte) ((val >> 32) & 0xff);
        b[2] = (byte) ((val >> 40) & 0xff);
        b[1] = (byte) ((val >> 48) & 0xff);
        b[0] = (byte) ((val >> 56) & 0xff);
        return b;
    }

    public static byte[] intToByte(int val) {
        byte[] b = new byte[4];
        b[3] = (byte) (val & 0xff);
        b[2] = (byte) ((val >> 8) & 0xff);
        b[1] = (byte) ((val >> 16) & 0xff);
        b[0] = (byte) ((val >> 24) & 0xff);
        return b;
    }
}
