package miniplc0java.analyser;

import miniplc0java.tokenizer.TokenType;

import java.util.ArrayList;
import java.util.HashMap;

public class SymbolBlock {
    HashMap<String,SymbolBlock> symbolBlockTable = new HashMap<>();
    HashMap<String,SymbolEntry> symbolEntryTable = new HashMap<>();
    ArrayList<SymbolEntry> paramList = new ArrayList<>();
    SymbolBlock parent ;
    boolean isFunction;
    boolean isStart;
    TokenType returnType;


    public SymbolBlock(){}

    public TokenType getReturnType() {
        return returnType;
    }

    public void setReturnType(TokenType returnType) {
        this.returnType = returnType;
    }

    public boolean isFunction() {
        return isFunction;
    }

    public void setFunction(boolean function) {
        isFunction = function;
    }

    public boolean isStart() {
        return isStart;
    }

    public void setStart(boolean start) {
        isStart = start;
    }

    public ArrayList<SymbolEntry> getParamList() {
        return paramList;
    }

    public void setParamList(ArrayList<SymbolEntry> paramList) {
        this.paramList = paramList;
    }

    public SymbolBlock getParent() {
        return parent;
    }

    public void setParent(SymbolBlock parent) {
        this.parent = parent;
    }

    public HashMap<String, SymbolBlock> getSymbolBlockTable() {
        return symbolBlockTable;
    }

    public void setSymbolBlockTable(HashMap<String, SymbolBlock> symbolBlockTable) {
        this.symbolBlockTable = symbolBlockTable;
    }

    public HashMap<String, SymbolEntry> getSymbolEntryTable() {
        return symbolEntryTable;
    }

    public void setSymbolEntryTable(HashMap<String, SymbolEntry> symbolEntryTable) {
        this.symbolEntryTable = symbolEntryTable;
    }
}
