package miniplc0java.analyser;

import miniplc0java.error.AnalyzeError;
import miniplc0java.error.ErrorCode;
import miniplc0java.instruction.Instruction;
import miniplc0java.tokenizer.TokenType;

import java.util.ArrayList;
import java.util.HashMap;

public class SymbolFunction {
    ArrayList<SymbolEntry> paramList = new ArrayList<>();
//    HashMap<Integer,SymbolBlock> blockTable

    public ArrayList<Instruction> instructions;

    public ArrayList<Instruction> getInstructions() {
        return instructions;
    }

    public void setInstructions(ArrayList<Instruction> instructions) {
        this.instructions = instructions;
    }

    TokenType returnType;
    int index;

    public ArrayList<SymbolEntry> getParamList() {
        return paramList;
    }

    public void setParamList(ArrayList<SymbolEntry> paramList) {
        this.paramList = paramList;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    int stackOffset;

    public SymbolFunction(ArrayList<SymbolEntry> paramList, TokenType returnType, int stackOffset,int index) throws AnalyzeError {
        this.paramList = paramList;
        this.stackOffset = stackOffset;
        this.index = index;
        switch (returnType){
            case VOID:
            case INT:
            case DOUBLE:
                this.returnType = returnType;
            default:
                throw new AnalyzeError(ErrorCode.InvalidInput,null);
        }
    }

    public int getStackOffset() {
        return stackOffset;
    }

    public void setStackOffset(int stackOffset) {
        this.stackOffset = stackOffset;
    }

    public TokenType getReturnType() {
        return returnType;
    }

    public void setReturnType(TokenType returnType) {
        this.returnType = returnType;
    }


}
