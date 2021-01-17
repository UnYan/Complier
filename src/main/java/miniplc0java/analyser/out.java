package miniplc0java.analyser;


import miniplc0java.instruction.FnInstruction;
import miniplc0java.instruction.Instruction;
import miniplc0java.instruction.Operation;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;

public class out {
    public static void Out(String name, ArrayList<String> globalV, ArrayList<FnInstruction> fnList) throws Exception{
        FileOutputStream file = new FileOutputStream(new File(name));
        file.write(intToByte(0x72303b3e));
        file.write(intToByte(0x1));

        file.write(intToByte(globalV.size()));

        for (String s : globalV) { //全局
            if (s.equals("1")) {
                file.write(0);
                file.write(intToByte(8));
                file.write(longToByte(0L));
            } else if (s.equals("0")) {
                file.write(1);
                file.write(intToByte(8));
                file.write(longToByte(0L));
            } else { //函数名、字符串
                file.write(1);
                file.write(s.length());
                file.write(s.getBytes());
            }
        }

        file.write(intToByte(fnList.size()));// functions.count

        for (FnInstruction fnInstruction : fnList) { //function
            file.write(intToByte(fnInstruction.getName()));
            file.write(intToByte(fnInstruction.getRet_slots()));
            file.write(intToByte(fnInstruction.getParam_slots()));
            file.write(intToByte(fnInstruction.getLoc_slots()));
            file.write(intToByte(fnInstruction.getBodyCount()));

            ArrayList<Instruction> fnInstructions = fnInstruction.getBodyItem();

            for (Instruction instruction : fnInstructions) {
                file.write(instruction.getOpt().getI());
                if (instruction.getValue() != null) {
                    if (instruction.getOpt() == Operation.push) {
                        file.write(longToByte((long) instruction.getValue()));
                    } else {
                        file.write(intToByte((int) instruction.getValue()));
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
