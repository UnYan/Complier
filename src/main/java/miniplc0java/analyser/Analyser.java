package miniplc0java.analyser;

import miniplc0java.error.AnalyzeError;
import miniplc0java.error.CompileError;
import miniplc0java.error.ErrorCode;
import miniplc0java.error.ExpectedTokenError;
import miniplc0java.error.TokenizeError;
import miniplc0java.instruction.FnInstruction;
import miniplc0java.instruction.Instruction;
import miniplc0java.instruction.Operation;
import miniplc0java.tokenizer.Token;
import miniplc0java.tokenizer.TokenType;
import miniplc0java.tokenizer.Tokenizer;
import miniplc0java.util.Pos;

import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.*;
import java.util.function.ToDoubleBiFunction;

public final class Analyser {

    Tokenizer tokenizer;
    ArrayList<Instruction> instructions;

    int globalOffset = 0;
    int argsOffset = 0;
    int localOffset = 0;
    int fnOffset = 0;

    ArrayList<String> GlobalVariable=new ArrayList<>();
    ArrayList<FnInstruction> fnLists = new ArrayList<>();
    ArrayList<Instruction> CurrentFnInstruction;

    boolean hasMain = false;
    int fnPos = 0;
    boolean maintype = false;

    private static int operatorStart = 15;
    private static int operatorEnd = 24;
    private static List<TokenType> operatorList = new ArrayList<>();
    private static HashMap<TokenType,Integer> operatorPriority = new HashMap<>();

    private static int globalVarIndex = 0;
    private static int globalFunIndex = 0;

    ArrayList<TokenType> Symbol = new ArrayList<TokenType>(Arrays.asList(TokenType.AS_KW, TokenType.MUL, TokenType.DIV, TokenType.PLUS, TokenType.MINUS, TokenType.GT, TokenType.LT, TokenType.LE, TokenType.GE, TokenType.EQ, TokenType.NEQ));

    public int[][] SymbolMatrix = {
            {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
            {0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
            {0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
            {0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1},
            {0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1},
            {0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1},
            {0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1},
            {0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1},
            {0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1},
            {0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1},
            {0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1}
    };


    /** 当前偷看的 token */
    Token peekedToken = null;

    /** 符号表 */
    HashMap<String, Integer> symbolHash = new HashMap<>();
    Stack<Symbol> symbolTable = new Stack<>();
    Stack<Integer> symbolInt = new Stack<>();
    HashMap<String, SymbolFunction> symbolFunctionTable = new HashMap<>();

    /** 下一个变量的栈偏移 */
    int nextOffset = 0;

    public Analyser(Tokenizer tokenizer) {
        this.tokenizer = tokenizer;
        this.instructions = new ArrayList<>();
    }

    public List<Instruction> analyse(PrintStream output) throws Exception {
//        addOperatorList();
//        initOperatorPriority();
        analyseProgram(output);
//        outputC0();
        return instructions;
    }

    public void outputC0(){
        byte[] bytes = null;
        DecimalFormat decimalFormat = new DecimalFormat("00");
        System.out.println(Arrays.toString(hex2Bytes("72303b3e")));
        System.out.println(Arrays.toString(hex2Bytes("00000001")));

        System.out.println(Arrays.toString(hex2Bytes(decimalFormat.format(globalVarIndex + 1))));

        System.out.println(Arrays.toString(hex2Bytes(decimalFormat.format(globalFunIndex + 1))));
    }

    public static byte[] hex2Bytes(String hexString) {
        if (hexString == null || hexString.equals("")) {
            return null;
        }

        int length = hexString.length() / 2;
        char[] hexChars = hexString.toCharArray();
        byte[] bytes = new byte[length];
        String hexDigits = "0123456789abcdef";
        for (int i = 0; i < length; i++) {
            int pos = i * 2; // 两个字符对应一个byte
            int h = hexDigits.indexOf(hexChars[pos]) << 4; // 注1
            int l = hexDigits.indexOf(hexChars[pos + 1]); // 注2
            if(h == -1 || l == -1) { // 非16进制字符
                return null;
            }
            bytes[i] = (byte) (h | l);
        }
        return bytes;
    }

    private void addOperatorList(){
        operatorList.add(TokenType.MUL);
        operatorList.add(TokenType.PLUS);
        operatorList.add(TokenType.MINUS);
        operatorList.add(TokenType.DIV);
        operatorList.add(TokenType.EQ);
        operatorList.add(TokenType.NEQ);
        operatorList.add(TokenType.LE);
        operatorList.add(TokenType.GE);
        operatorList.add(TokenType.LT);
        operatorList.add(TokenType.GT);
    }

    private void initOperatorPriority(){
        operatorPriority.put(TokenType.L_PAREN,0);
        operatorPriority.put(TokenType.R_PAREN,0);
        operatorPriority.put(TokenType.SHARP,0);

        operatorPriority.put(TokenType.FUNCTION,1);

        operatorPriority.put(TokenType.AS_KW,3);

        operatorPriority.put(TokenType.MUL,4);
        operatorPriority.put(TokenType.DIV,4);

        operatorPriority.put(TokenType.PLUS,5);
        operatorPriority.put(TokenType.MINUS,5);

        operatorPriority.put(TokenType.GT,6);
        operatorPriority.put(TokenType.LT,6);
        operatorPriority.put(TokenType.GE,6);
        operatorPriority.put(TokenType.LE,6);
        operatorPriority.put(TokenType.EQ,6);
        operatorPriority.put(TokenType.NEQ,6);

        operatorPriority.put(TokenType.ASSIGN,7);
    }

    private boolean checkOperator(TokenType tt1,TokenType tt2,Pos curPos) throws AnalyzeError {
//        if(!OperatorList.contains(tt1) || !OperatorList.contains(tt2))
//            throw new AnalyzeError(ErrorCode.NotOperator,curPos);
//        if( tt2 == TokenType.SHARP || tt2 == TokenType.L_PAREN || tt2 == TokenType.R_PAREN) {
//            return true;
//        }
//        else if(tt2 == TokenType.FUNCTION){
//            switch (tt1){
//                case SHARP:
//                case L_PAREN:
//                case R_PAREN:
//                    return false;
//                default:
//                    return true;
//            }
//        }
        Integer priority1 = operatorPriority.get(tt1);
        Integer priority2 = operatorPriority.get(tt2);

        if(priority1 == null || priority2 == null)
            throw new AnalyzeError(ErrorCode.NotOperator,curPos);

        return priority1 >= priority2;
    }

    /**
     * 查看下一个 Token
     * 
     * @return
     * @throws TokenizeError
     */
    private Token peek() throws TokenizeError {
        if (peekedToken == null) {
            peekedToken = tokenizer.nextToken();
        }
        return peekedToken;
    }

    /**
     * 获取下一个 Token
     * 
     * @return
     * @throws TokenizeError
     */
    private Token next() throws TokenizeError {
        if (peekedToken != null) {
            var token = peekedToken;
            peekedToken = null;
            return token;
        } else {
            return tokenizer.nextToken();
        }
    }

    /**
     * 如果下一个 token 的类型是 tt，则返回 true
     * 
     * @param tt
     * @return
     * @throws TokenizeError
     */
    private boolean check(TokenType tt) throws TokenizeError {
        var token = peek();
        return token.getTokenType() == tt;
    }

    /**
     * 如果下一个 token 的类型是 tt，则前进一个 token 并返回这个 token
     * 
     * @param tt 类型
     * @return 如果匹配则返回这个 token，否则返回 null
     * @throws TokenizeError
     */
    private Token nextIf(TokenType tt) throws TokenizeError {
        var token = peek();
        if (token.getTokenType() == tt) {
            return next();
        } else {
            return null;
        }
    }

    /**
     * 如果下一个 token 的类型是 tt，则前进一个 token 并返回，否则抛出异常
     * 
     * @param tt 类型
     * @return 这个 token
     * @throws CompileError 如果类型不匹配
     */
    private Token expect(TokenType tt) throws CompileError {
        var token = peek();
        if (token.getTokenType() == tt) {
            return next();
        } else {
            throw new ExpectedTokenError(tt, token);
        }
    }

    private Token expect(TokenType tt1,TokenType tt2) throws CompileError {
        var token = peek();
        if (token.getTokenType() == tt1) {
            return next();
        }
        else if(token.getTokenType() == tt2)
            return next();
        else {
            throw new ExpectedTokenError(TokenType.TYPE, token);
        }
    }

    private Token expect(TokenType tt1,TokenType tt2,TokenType tt3) throws CompileError {
        var token = peek();
        if (token.getTokenType() == tt1) {
            return next();
        }
        else if(token.getTokenType() == tt2)
            return next();
        else if(token.getTokenType() == tt3)
            return next();
        else {
            throw new ExpectedTokenError(TokenType.TYPE, token);
        }
    }

    private Token expect(TokenType tt1,TokenType tt2,TokenType tt3,TokenType tt4) throws CompileError {
        var token = peek();
        if (token.getTokenType() == tt1) {
            return next();
        }
        else if(token.getTokenType() == tt2)
            return next();
        else if(token.getTokenType() == tt3)
            return next();
        else if(token.getTokenType() == tt4)
            return next();
        else {
            throw new ExpectedTokenError(TokenType.TYPE, token);
        }
    }

    /**
     * 获取下一个变量的栈偏移
     * 
     * @return
     */
    private int getNextVariableOffset() {
        return this.nextOffset++;
    }

    /**
     * 添加一个符号
     *
     * @param name          名字
     * @param isConstant    是否是常量
     * @param curPos        当前 token 的位置（报错用）
     * @throws AnalyzeError 如果重复定义了则抛异常
     */
    private void addSymbol(String name, boolean isConstant, TokenType type, SymbolType symbolType, Pos curPos) throws AnalyzeError {
//        if (this.symbolTable.get(name) != null) {
//            throw new AnalyzeError(ErrorCode.DuplicateDeclaration, curPos);
//        } else {
//            this.symbolTable.put(name, new SymbolEntry(isConstant, isInitialized, isGlobal, getNextVariableOffset()));
//        }
        if(this.symbolHash.get(name) != null && this.symbolHash.get(name) >= symbolInt.peek()){
            throw new AnalyzeError(ErrorCode.DuplicateDeclaration, curPos);
        }
        else {
            if (this.symbolHash.get(name) != null) {
                int chain = this.symbolHash.get(name);
                switch (symbolType) {
                    case global:
                        this.symbolTable.push(new Symbol(name, chain, type, isConstant, symbolType, globalOffset++));
                        if(isConstant){
                            GlobalVariable.add("0");
                        }else{
                            GlobalVariable.add("1");
                        }
                        break;
                    case args:
                        this.symbolTable.push(new Symbol(name, chain, type, isConstant, symbolType, argsOffset++));
                        break;
                    case local:
                        this.symbolTable.push(new Symbol(name, chain, type, isConstant, symbolType, localOffset++));
                        break;
                    default:
                        throw new AnalyzeError(ErrorCode.InvalidInput, curPos);
                }
            }
            else {
                switch (symbolType) {
                    case global:
                        this.symbolTable.push(new Symbol(name, -1, type, isConstant, symbolType, globalOffset++));
                        if(isConstant){
                            GlobalVariable.add("0");
                        }else{
                            GlobalVariable.add("1");
                        }
                        break;
                    case args:
                        this.symbolTable.push(new Symbol(name, -1, type, isConstant, symbolType, argsOffset++));
                        break;
                    case local:
                        this.symbolTable.push(new Symbol(name, -1, type, isConstant, symbolType, localOffset++));
                        break;
                    default:
                        throw new AnalyzeError(ErrorCode.InvalidInput, curPos);
                }
            }
            this.symbolHash.put(name, symbolTable.size() - 1);
        }
    }

    private Symbol addFnSymbol(String name, Pos curPos) throws AnalyzeError {
        if (this.symbolHash.get(name) != null) {
            throw new AnalyzeError(ErrorCode.DuplicateDeclaration, curPos);
        }
        else {
            this.symbolTable.push(new Symbol(name, true, globalOffset, fnOffset++));
            this.symbolHash.put(name, symbolTable.size() - 1);
            this.symbolInt.push(symbolTable.size());
            return this.symbolTable.peek();
        }

    }


//    private void addVar(String name, boolean isInitialized, boolean isConstant,String value, TokenType type,Pos curPos) throws AnalyzeError {
//        int l = symbolTable.size();
//        HashMap<String,SymbolEntry> table = symbolTable.get(l - 1);
//        if(table.get(name) != null)
//            throw new AnalyzeError(ErrorCode.DuplicateDeclaration, curPos);
//        symbolTable.get(l - 1).put(name,new SymbolEntry(isConstant,isInitialized,value,type,getNextVariableOffset(),globalVarIndex++));
//    }

//    private SymbolBlock addBlock(SymbolBlock parent){
//        SymbolBlock symbolBlock = new SymbolBlock();
//        symbolBlock.parent = parent;
//        parent.symbolBlockTable.put(null, symbolBlock);
//        return symbolBlock;
//    }

    private void addFun(Token token,ArrayList<SymbolEntry> paramList, TokenType returnType,Pos curPos) throws AnalyzeError {
        if(symbolFunctionTable.get(token.getValueString()) != null)
            throw new AnalyzeError(ErrorCode.DuplicateDeclaration, curPos);
        symbolFunctionTable.put(token.getValueString(), new SymbolFunction(paramList,returnType,getNextVariableOffset(),globalFunIndex++));
    }

//    private SymbolEntry varIsDeclared(String name,Pos curPos) throws AnalyzeError {
//        int l = symbolTable.size();
//        for(int i = l - 1 ;i >= 0;i--){
//            HashMap<String,SymbolEntry> table = symbolTable.get(i);
//            if(table.get(name) != null)
//                return table.get(name);
//        }
//        return null;
//    }

//    private boolean varIsConstant(String name,Pos curPos) throws AnalyzeError {
//            SymbolEntry var = varIsDeclared(name,curPos);
//            if(var == null)
//                throw new AnalyzeError(ErrorCode.NotDeclared,curPos);
//            else
//                return var.isConstant;
//    }

//    private boolean varIsInitialized(String name,Pos curPos) throws AnalyzeError {
//        SymbolEntry var = varIsDeclared(name,curPos);
//        if(var == null)
//            throw new AnalyzeError(ErrorCode.NotDeclared,curPos);
//        else
//            return var.isInitialized;
//    }

    private SymbolFunction funIsDeclared(String name){
        return symbolFunctionTable.get(name);
    }

//    private void addFunction(String name, HashMap<String,SymbolEntry> paramList, TokenType returnType, Pos curPos) throws AnalyzeError {
//        if(this.symbolFunctionTable.get(name) != null){
//            throw new AnalyzeError(ErrorCode.DuplicateDeclaration, curPos);
//        }
//        else{
//            this.symbolFunctionTable.put(name ,new SymbolFunction(paramList, returnType,getNextVariableOffset()));
//        }
//    }


//
//    /**
//     * 设置符号为已赋值
//     *
//     * @param name   符号名称
//     * @param curPos 当前位置（报错用）
//     * @throws AnalyzeError 如果未定义则抛异常
//     */
//    private void initializeSymbol(String name, Pos curPos) throws AnalyzeError {
//        var entry = this.symbolTable.get(name);
//        if (entry == null) {
//            throw new AnalyzeError(ErrorCode.NotDeclared, curPos);
//        } else {
//            entry.setInitialized(true);
//        }
//    }

//    /**
//     * 获取变量在栈上的偏移
//     *
//     * @param name   符号名
//     * @param curPos 当前位置（报错用）
//     * @return 栈偏移
//     * @throws AnalyzeError
//     */
//    private int getOffset(String name, Pos curPos) throws AnalyzeError {
//        var entry = this.symbolTable.get(name);
//        if (entry == null) {
//            throw new AnalyzeError(ErrorCode.NotDeclared, curPos);
//        } else {
//            return entry.getStackOffset();
//        }
//    }

//    /**
//     * 获取变量是否是常量
//     *
//     * @param name   符号名
//     * @param curPos 当前位置（报错用）
//     * @return 是否为常量
//     * @throws AnalyzeError
////     */
//    private boolean isConstant(String name, Pos curPos) throws AnalyzeError {
//        var entry = this.symbolTable.get(name);
//        if (entry == null) {
//            throw new AnalyzeError(ErrorCode.NotDeclared, curPos);
//        } else {
//            return entry.isConstant();
//        }
//    }

    public void analyseProgram(PrintStream output) throws Exception {
//
//        while(nextIsFunction() || nextIsDeclStmt()){
//            if(nextIsFunction())
//                analyseFunction();
//            else if(nextIsDeclStmt())
//                analyseDeclStmt();
//        }
        analyseMain();

        expect(TokenType.EOF);
        System.out.println();
        for (String s : GlobalVariable) {
            System.out.println(s);
        }
        for (FnInstruction fnList : fnLists) {
            System.out.println(fnList.toString());
        }

        out.Out(output, GlobalVariable, fnLists);

//        while(true){
//            if(nextIf(TokenType.EOF) != null)
//                return;
//            else if(nextIsFunction())
//                analyseFunction();
//            else if(nextIsDeclStmt())
//                analyseDeclStmt();
//            else
//                throw new AnalyzeError(ErrorCode.InvalidInput,peek().getStartPos());
//        }

    }

    private void analyseMain() throws CompileError {
        FnInstruction startFn = new FnInstruction();
        GlobalVariable.add("_start");
        globalOffset++;
        fnLists.add(startFn);
        while (true) {
            if (check(TokenType.CONST_KW) || check(TokenType.LET_KW)) {
                if (check(TokenType.CONST_KW)) {
                    CurrentFnInstruction = startFn.getBodyItem();
                    analyseConstDeclStmt(true);
                }
                else if (check(TokenType.LET_KW)) {
                    CurrentFnInstruction = startFn.getBodyItem();
                    analyseLetDeclStmt(true);
                }
            }
            else if (check(TokenType.FN_KW)) {
                analyseFunction(); //进入function分析过程
            }
            else {
                break;
//                throw new AnalyzeError(ErrorCode.InvalidAssignment, );
            }
        }

        startFn.setName(0);
        startFn.setRet_slots(0);
        startFn.setParam_slots(0);
        startFn.setLoc_slots(0);
        if(hasMain){
            if(!maintype){
                startFn.getBodyItem().add(new Instruction(Operation.stackalloc, 0));
            }else{
                startFn.getBodyItem().add(new Instruction(Operation.stackalloc, 1));
            }
            startFn.getBodyItem().add(new Instruction(Operation.call, fnPos));
            if(maintype){
                startFn.getBodyItem().add(new Instruction(Operation.popn,1));
            }
        }
        startFn.setBodyCount(startFn.getBodyItem().size());

    }

    private Boolean nextIsDeclStmt() throws TokenizeError {
        peekedToken = peek();
        return peekedToken.getTokenType() == TokenType.LET_KW ||
                peekedToken.getTokenType() == TokenType.CONST_KW;
    }

    private Boolean nextIsFunction() throws TokenizeError {
        peekedToken = peek();
        return peekedToken.getTokenType() == TokenType.FN_KW;
    }

    private void analyseFunction() throws CompileError {
        FnInstruction fnInstruction = new FnInstruction();
        fnLists.add(fnInstruction);
        CurrentFnInstruction = fnInstruction.getBodyItem();
        boolean hasReturn = false;

        expect(TokenType.FN_KW);

        Token funName = expect(TokenType.IDENT);
        GlobalVariable.add(funName.getValue().toString());
        fnInstruction.setName(globalOffset++);
        if(funName.getValue().toString().equals("main")){
            hasMain = true;
            fnPos = fnLists.size()-1;
        }
        Symbol currentSymbol = addFnSymbol(funName.getValue().toString(), funName.getStartPos());

//        if(funIsDeclared(symbolBlock, funName.getValueString()) != null)
//            throw new AnalyzeError(ErrorCode.DuplicateDeclaration,funName.getStartPos());

        expect(TokenType.L_PAREN);
        argsOffset = 0;

        if (check(TokenType.CONST_KW) || check(TokenType.IDENT)) {
            analyseFunctionParamList();
        }

        expect(TokenType.R_PAREN);

        expect(TokenType.ARROW);

        Token returnType = expect(TokenType.IDENT);

        if(returnType.getValue().equals("int")){
            returnType.setTokenType(TokenType.INT);
            fnInstruction.setRet_slots(1);
            argsOffset++;
            for(int i = symbolTable.size() - 1; symbolTable.get(i).getSymbolType() == SymbolType.args; i--){
                symbolTable.get(i).setOffset(symbolTable.get(i).getOffset() + 1);
            }
            if(funName.getValue().toString().equals("main")){
                maintype = true;
            }
        }
        else if(returnType.getValue().equals("double")){
            returnType.setTokenType(TokenType.DOUBLE);
            fnInstruction.setRet_slots(1);
            argsOffset++;
            for(int i = symbolTable.size() - 1; symbolTable.get(i).getSymbolType() == SymbolType.args; i--){
                symbolTable.get(i).setOffset(symbolTable.get(i).getOffset() + 1);
            }
            if(funName.getValue().toString().equals("main")){
                maintype = true;
            }
        }
        else if(returnType.getValue().equals("void")){
            returnType.setTokenType(TokenType.VOID);
            fnInstruction.setRet_slots(0);
            if(funName.getValue() == "main"){
                maintype = false;
            }
        }
        else{
            throw new AnalyzeError(ErrorCode.NotDeclared, returnType.getStartPos());
        }

        fnInstruction.setParam_slots(argsOffset);

        currentSymbol.setType(returnType.getTokenType());

        localOffset = 0;
        hasReturn = analyseBlockStmt(true, returnType.getTokenType(), false, null, -1);
        fnInstruction.setLoc_slots(localOffset);

        if(returnType.getTokenType() != TokenType.VOID && !hasReturn){
            throw new AnalyzeError(ErrorCode.DuplicateDeclaration, new Pos(0,0));
        }
        else if(returnType.getTokenType() == TokenType.VOID && !hasReturn){
            CurrentFnInstruction.add(new Instruction(Operation.ret));
        }

        fnInstruction.setBodyCount(fnInstruction.getBodyItem().size());



    }

    private boolean nextIsFunctionParam() throws TokenizeError {
        peekedToken = peek();
        return peekedToken.getTokenType() == TokenType.CONST_KW ||
                peekedToken.getTokenType() == TokenType.IDENT;
    }

    private void analyseFunctionParamList() throws CompileError {
        analyseFunctionParam();

        while (nextIf(TokenType.COMMA) != null) {
            analyseFunctionParam();
        }
    }

    private void analyseFunctionParam() throws CompileError {
//        boolean isConstant = false;
//        peekedToken = peek();
//        if(peekedToken.getTokenType() == TokenType.CONST_KW) {
//            expect(TokenType.CONST_KW);
//            isConstant = true;
//        }
//
//        Token token = expect(TokenType.IDENT);
//
//        expect(TokenType.COLON);
//
//        Token type = expectTyWithoutVoid();
//
//        SymbolEntry symbolEntry = new SymbolEntry(isConstant,true,"",type.getTokenType(),getNextVariableOffset());
//        symbolEntry.setName(token.getValueString());
//
//        return symbolEntry;
        if (nextIf(TokenType.CONST_KW) != null) {
            Token nameToken = expect(TokenType.IDENT);
            expect(TokenType.COLON);
            Token tyToken = expect(TokenType.IDENT);

            switch (tyToken.getValue().toString()) {
                case "double":
                    addSymbol(nameToken.getValue().toString(), true, TokenType.DOUBLE, SymbolType.args, nameToken.getStartPos());
                    this.symbolTable.get(this.symbolInt.peek() - 1).getParams().add(TokenType.DOUBLE);
                    break;
                case "int":
                    addSymbol(nameToken.getValue().toString(), true, TokenType.INT, SymbolType.args, nameToken.getStartPos());
                    this.symbolTable.get(this.symbolInt.peek() - 1).getParams().add(TokenType.INT);
                    break;
                default:
                    throw new AnalyzeError(ErrorCode.DuplicateDeclaration, nameToken.getStartPos());
            }
        } else {
            Token nameToken = expect(TokenType.IDENT);
            expect(TokenType.COLON); // :
            Token tyToken = expect(TokenType.IDENT);
            switch (tyToken.getValue().toString()) {
                case "double":
                    addSymbol(nameToken.getValue().toString(), false, TokenType.DOUBLE, SymbolType.args, nameToken.getStartPos());
                    this.symbolTable.get(this.symbolInt.peek() - 1).getParams().add(TokenType.DOUBLE);
                    break;
                case "int":
                    addSymbol(nameToken.getValue().toString(), false, TokenType.INT, SymbolType.args, nameToken.getStartPos());
                    this.symbolTable.get(this.symbolInt.peek() - 1).getParams().add(TokenType.INT);
                    break;
                default:
                    throw new AnalyzeError(ErrorCode.DuplicateDeclaration, nameToken.getStartPos());
            }
        }
    }

    private Token expectTyWithoutVoid() throws CompileError {
        return expect(TokenType.INT,TokenType.DOUBLE);
    }

    private Token expectTy() throws CompileError{
        return expect(TokenType.INT,TokenType.VOID,TokenType.DOUBLE);
    }

    private boolean analyseStatement(TokenType tyTokenType, boolean isWhile , ArrayList<Integer> breakEndPos, int continuePos) throws CompileError {
        if(nextIsExpr())
            analyseExprStmt();
        else {
            peekedToken = peek();
            switch (peekedToken.getTokenType()) {
                case LET_KW://ok
                    analyseLetDeclStmt(false);
                    break;
                case CONST_KW://ok
                    analyseConstDeclStmt(false);
                    break;
                case IF_KW://ok
                    analyseIfStmt(tyTokenType, isWhile, breakEndPos, continuePos);
                    break;
                case WHILE_KW://ok
                    analyseWhileStmt(tyTokenType);
                    break;
                case BREAK_KW://ok
                    if(!isWhile){
                        throw new AnalyzeError(ErrorCode.DuplicateDeclaration, new Pos(2,0));
                    }
                    analyseBreakStmt();
                    CurrentFnInstruction.add(new Instruction(Operation.br));
                    int breakPos = CurrentFnInstruction.size()-1;
                    breakEndPos.add(breakPos);
                    break;
                case CONTINUE_KW://ok
                    if(!isWhile){
                        throw new AnalyzeError(ErrorCode.DuplicateDeclaration, new Pos(2,0));
                    }
                    analyseContinueStmt();
                    CurrentFnInstruction.add(new Instruction(Operation.br,continuePos-CurrentFnInstruction.size()));
                    break;
                case RETURN_KW://ok
                    analyseReturnStmt(tyTokenType);
                    return true;
                case L_BRACE:
                    return analyseBlockStmt(false, tyTokenType, isWhile, breakEndPos, continuePos);
                case SEMICOLON:
                    analyseEmptyStmt();
                    break;
                default:
                    throw new AnalyzeError(ErrorCode.InvalidInput,peekedToken.getStartPos());
            }
        }
        return false;
    }

    private void analyseBreakStmt() throws CompileError {
        expect(TokenType.BREAK_KW);
        expect(TokenType.SEMICOLON);
    }

    private void analyseContinueStmt() throws CompileError {
        expect(TokenType.CONTINUE_KW);
        expect(TokenType.SEMICOLON);
    }

    private void analyseExprStmt() throws CompileError {
        TokenType t = null;
        if (nextIsExpr()) {
            t = analyseExpression(true);
        }
        if(t != TokenType.VOID){
            CurrentFnInstruction.add(new Instruction(Operation.popn, 1));
        }

        expect(TokenType.SEMICOLON);
    }

//    private void analyseDeclStmt() throws CompileError {
//        peekedToken = peek();
//        if(peekedToken.getTokenType() == TokenType.LET_KW)
//            analyseLetDeclStmt();
//        else if(peekedToken.getTokenType() == TokenType.CONST_KW)
//            analyseConstDeclStmt();
//        else
//            throw new AnalyzeError(ErrorCode.InvalidInput,peekedToken.getStartPos());
//    }


    private boolean analyseIfStmt(TokenType tyTokenType, boolean isWhile, ArrayList<Integer> breakEndPos, int continuePos) throws CompileError {
        boolean hasReturn = false;
        boolean hasElse = false;

        expect(TokenType.IF_KW);

        TokenType ifExpr = analyseExpression(true);
        if(ifExpr == TokenType.VOID)
            throw new AnalyzeError(ErrorCode.DuplicateDeclaration, new Pos(1,0));

        CurrentFnInstruction.add(new Instruction(Operation.brtrue, 1));
        CurrentFnInstruction.add(new Instruction(Operation.br));
        int currentPos = CurrentFnInstruction.size()-1;

        hasReturn = analyseBlockStmt(false, tyTokenType, isWhile, breakEndPos, continuePos); //if 第一个block块
        CurrentFnInstruction.add(new Instruction(Operation.br));
        int endPos = CurrentFnInstruction.size()-1;

        ArrayList<Integer> Pos = new ArrayList<>();
        while (nextIf(TokenType.ELSE_KW) != null) {
            if (nextIf(TokenType.IF_KW) != null) {
                CurrentFnInstruction.get(currentPos).setValue(CurrentFnInstruction.size()-1 - currentPos);
                ifExpr = analyseExpression(true);
                if(ifExpr == TokenType.VOID){
                    throw new AnalyzeError(ErrorCode.DuplicateDeclaration, new Pos(1,0));
                }
                hasReturn &= analyseBlockStmt(false, tyTokenType, isWhile, breakEndPos, continuePos);
                CurrentFnInstruction.add(new Instruction(Operation.br));
                Pos.add(CurrentFnInstruction.size()-1);
            } else if (check(TokenType.L_BRACE)) {
                CurrentFnInstruction.get(currentPos).setValue(CurrentFnInstruction.size()-1 - currentPos);
                hasReturn &= analyseBlockStmt(false, tyTokenType, isWhile, breakEndPos, continuePos);
                hasElse = true;
                break;
            }
            CurrentFnInstruction.get(currentPos).setValue(CurrentFnInstruction.size()-1 - currentPos);
        }
        CurrentFnInstruction.get(endPos).setValue(CurrentFnInstruction.size()-1-endPos);
        for (Integer po : Pos) {
            CurrentFnInstruction.get(po).setValue(CurrentFnInstruction.size() - 1 - po);
        }
        if(!hasElse){
            return false;
        }
        return hasReturn;
    }

    private void analyseConstDeclStmt(boolean isGlobal) throws CompileError {
        expect(TokenType.CONST_KW);

        Token ident = expect(TokenType.IDENT);
        String name = (String)ident.getValue();

        if(!isGlobal){
            CurrentFnInstruction.add(new Instruction(Operation.loca, localOffset));
        }else{
            CurrentFnInstruction.add(new Instruction(Operation.globa, globalOffset));
        }

        expect(TokenType.COLON);

        Token type = expect(TokenType.IDENT);
        if(type.getValue().equals("int")){
            type.setTokenType(TokenType.INT);
        }
        else if(type.getValue().equals("double")){
            type.setTokenType(TokenType.DOUBLE);
        }
        else{
            throw new AnalyzeError(ErrorCode.NotDeclared, type.getStartPos());
        }

        expect(TokenType.ASSIGN);

        TokenType result = analyseExpression(true);
        if(type.getTokenType() != result){
            throw new AnalyzeError(ErrorCode.NotDeclared, type.getStartPos());
        }
        CurrentFnInstruction.add(new Instruction(Operation.store64));

        expect(TokenType.SEMICOLON);

        if (isGlobal) {
            addSymbol(name, true, type.getTokenType(), SymbolType.global, ident.getStartPos());
        } else {
            addSymbol(name, true, type.getTokenType(), SymbolType.local, ident.getStartPos());
        }
    }

    private void analyseLetDeclStmt(boolean isGlobal) throws CompileError {
        expect(TokenType.LET_KW);

        Token ident = expect(TokenType.IDENT);

        expect(TokenType.COLON);

        Token type = expect(TokenType.IDENT);
        if(type.getValue().equals("int")){
            type.setTokenType(TokenType.INT);
        }
        else if(type.getValue().equals("double")){
            type.setTokenType(TokenType.DOUBLE);
        }
        else{
            throw new AnalyzeError(ErrorCode.NotDeclared, type.getStartPos());
        }

        TokenType result = null;
        if(nextIf(TokenType.ASSIGN) != null){
            if(isGlobal){
                CurrentFnInstruction.add(new Instruction(Operation.globa, globalOffset));
            }else{
                CurrentFnInstruction.add(new Instruction(Operation.loca, localOffset));
            }
            result = analyseExpression(true);
            if(type.getTokenType() != result)
                throw new AnalyzeError(ErrorCode.MismatchedAssignmentType,type.getStartPos());
            else
                CurrentFnInstruction.add(new Instruction(Operation.store64));
        }

        expect(TokenType.SEMICOLON);

        if (isGlobal) {
            addSymbol(ident.getValue().toString(), false, type.getTokenType(), SymbolType.global, ident.getStartPos());
        } else {
            addSymbol(ident.getValue().toString(), false, type.getTokenType(), SymbolType.local, ident.getStartPos());
        }
    }


    private void analyseWhileStmt(TokenType tyTokenType) throws CompileError {
        expect(TokenType.WHILE_KW);

        int InitPos=CurrentFnInstruction.size()-1;

        TokenType whileExpr = analyseExpression(true);

        ArrayList<Integer> breakEndPos = new ArrayList<>();

        CurrentFnInstruction.add(new Instruction(Operation.brtrue, 1));

        CurrentFnInstruction.add(new Instruction(Operation.br));
        int currentPos = CurrentFnInstruction.size()-1;

        if(whileExpr == TokenType.VOID){
            throw new AnalyzeError(ErrorCode.DuplicateDeclaration, new Pos(1,0));
        }
        analyseBlockStmt(false, tyTokenType, true, breakEndPos, InitPos);
        CurrentFnInstruction.add(new Instruction(Operation.br, InitPos-CurrentFnInstruction.size()+1));
        CurrentFnInstruction.get(currentPos).setValue(CurrentFnInstruction.size()-1 - currentPos);
        for (Integer breakEndPo : breakEndPos) {
            CurrentFnInstruction.get(breakEndPo).setValue(CurrentFnInstruction.size() - 1 - breakEndPo);
        }
    }

    private void analyseReturnStmt(TokenType tyTokenType) throws CompileError {
        expect(TokenType.RETURN_KW);

        if(tyTokenType == TokenType.INT || tyTokenType == TokenType.DOUBLE){
            CurrentFnInstruction.add(new Instruction(Operation.arga, 0));
        }
        if (nextIsExpr()) {
            TokenType exprType = analyseExpression(true);
            if(exprType != tyTokenType){
                throw new AnalyzeError(ErrorCode.DuplicateDeclaration, new Pos(1,0));
            }
        }else if(tyTokenType != TokenType.VOID){
            throw new AnalyzeError(ErrorCode.DuplicateDeclaration, new Pos(1,0));
        }
        if(tyTokenType == TokenType.INT || tyTokenType == TokenType.DOUBLE){
            CurrentFnInstruction.add(new Instruction(Operation.store64));
        }
        CurrentFnInstruction.add(new Instruction(Operation.ret));
        expect(TokenType.SEMICOLON);
    }

    private boolean nextIsExpr() throws TokenizeError {
        peekedToken = peek();
        switch (peekedToken.getTokenType()){
            case IDENT:
            case MINUS:
            case L_PAREN:
            case UINT_LITERAL:
            case STRING_LITERAL:
            case DOUBLE_LITERAL:
            case CHAR_LITERAL:
                return true;
            default:
                return false;
        }
    }


    private boolean analyseBlockStmt(boolean isFn, TokenType tyTokenType, boolean isWhile, ArrayList<Integer> breakEndPos, int continuePos) throws CompileError {
        boolean hasReturn = false;

        expect(TokenType.L_BRACE);

        if (!isFn) {
            symbolInt.push(symbolTable.size());
        }

        if(!hasReturn){
            hasReturn = analyseStatement(tyTokenType, isWhile, breakEndPos, continuePos);//进入stmt循环分析
        }
        else{
            analyseStatement(tyTokenType, isWhile, breakEndPos, continuePos); //进入stmt循环分析
        }

        while(nextIsStmt()){
            if(!hasReturn){
                hasReturn = analyseStatement(tyTokenType, isWhile, breakEndPos, continuePos);//进入stmt循环分析
            }
            else{
                analyseStatement(tyTokenType, isWhile, breakEndPos, continuePos); //进入stmt循环分析
            }
        }

        expect(TokenType.R_BRACE);

        int index = symbolInt.pop();
        while (symbolTable.size() > index) {
            Symbol s = symbolTable.pop();
            if (s.getChain() != -1) {
                symbolHash.put(s.getName(), s.getChain());
            } else {
                symbolHash.remove(s.getName());
            }
        }

        return hasReturn;
    }


    private void analyseEmptyStmt() throws CompileError {
        expect(TokenType.SEMICOLON);
    }

    private boolean nextIsStmt() throws TokenizeError {
        if(nextIsExpr())
            return true;
        peekedToken = peek();
        switch (peekedToken.getTokenType()){
            case LET_KW:
            case CONST_KW:
            case IF_KW:
            case WHILE_KW:
            case RETURN_KW:
            case BREAK_KW:
            case CONTINUE_KW:
            case L_BRACE:
            case SEMICOLON:
                return true;
            default:
                return false;
        }
    }

    
    private TokenType analyseExpression(boolean f) throws CompileError {
        peekedToken = peek();
        TokenType type = null;
//        switch (peekedToken.getTokenType()){
//            case IDENT:
//                result1 = analyseIdentExpression();
//                break;
//            case MINUS:
//                type = analyseNegateExpression();
//                break;
//            case L_PAREN:
//                result1 = analyseGroupExpression();
//                break;
//            default:
//                result1 = analyseLiteralExpression();
//        }
        if (check(TokenType.MINUS)) {
            type = analyseNegateExpression();
            if (type == TokenType.INT)
                CurrentFnInstruction.add(new Instruction(Operation.negi));
            else if (type == TokenType.DOUBLE)
                CurrentFnInstruction.add(new Instruction(Operation.negf));
            else
                throw new AnalyzeError(ErrorCode.NotDeclared, new Pos(0, 0));
        }

        if (peek().getTokenType() == TokenType.IDENT) {
            Token nameToken = next();

            Integer index = symbolHash.get(nameToken.getValue().toString());

            if (nextIf(TokenType.ASSIGN) != null) {

                if (index == null) {
                    throw new AnalyzeError(ErrorCode.NotDeclared, nameToken.getStartPos());
                }

                if(symbolTable.get(index).isConst()){
                    throw new AnalyzeError(ErrorCode.NotDeclared, nameToken.getStartPos());
                }

                if(symbolTable.get(index).getSymbolType() == SymbolType.local){
                    CurrentFnInstruction.add(new Instruction(Operation.loca, symbolTable.get(index).getOffset()));
                }else if(symbolTable.get(index).getSymbolType() == SymbolType.global){
                    CurrentFnInstruction.add(new Instruction(Operation.globa, symbolTable.get(index).getOffset()));
                }else{
                    CurrentFnInstruction.add(new Instruction(Operation.arga, symbolTable.get(index).getOffset()));
                }

                TokenType l_type = symbolTable.get(index).getType();
                TokenType r_type = analyseExpression(true);

                if (l_type != r_type) {
                    throw new AnalyzeError(ErrorCode.NotDeclared, nameToken.getStartPos());
                }

                CurrentFnInstruction.add(new Instruction(Operation.store64));
                type = TokenType.VOID;
            }
            else if (nextIf(TokenType.L_PAREN) != null) { //call

                int currentGlobal = 0;
                ArrayList<TokenType> call_array = null;
                TokenType return_type;

                if (index == null) {
                    switch (nameToken.getValue().toString()) {
                        case "getint":
                        case "getchar":
                            call_array = new ArrayList<TokenType>();
                            return_type = TokenType.INT;
                            break;
                        case "getdouble":
                            call_array = new ArrayList<TokenType>();
                            return_type = TokenType.DOUBLE;
                            break;
                        case "putint":
                            call_array = new ArrayList<TokenType>() {{
                                add(TokenType.INT);
                            }};
                            return_type = TokenType.VOID;
                            break;
                        case "putdouble":
                            call_array = new ArrayList<TokenType>() {{
                                add(TokenType.DOUBLE);
                            }};
                            return_type = TokenType.VOID;
                            break;
                        case "putchar":
                            call_array = new ArrayList<TokenType>() {{
                                add(TokenType.INT);
                            }};
                            return_type = TokenType.VOID;
                            break;
                        case "putstr":
                            call_array = new ArrayList<TokenType>() {{
                                add(TokenType.INT);
                            }};
                            return_type = TokenType.VOID;
                            break;
                        case "putln":
                            call_array = new ArrayList<TokenType>();
                            return_type = TokenType.VOID;
                            break;
                        default:
                            throw new AnalyzeError(ErrorCode.NotDeclared, nameToken.getStartPos());
                    }
                    GlobalVariable.add(nameToken.getValue().toString());
                    currentGlobal = globalOffset ++;
                } else {
                    Symbol call_index = symbolTable.get(index);
                    call_array = call_index.getParams();
                    return_type = call_index.getType();
                }

                if(return_type == TokenType.INT || return_type == TokenType.DOUBLE){
                    CurrentFnInstruction.add(new Instruction(Operation.stackalloc, 1));
                }else if(return_type == TokenType.VOID){
                    CurrentFnInstruction.add(new Instruction(Operation.stackalloc, 0));
                }

                if (nextIf(TokenType.R_PAREN) != null) {
                    if (call_array.size() != 0) {
                        throw new AnalyzeError(ErrorCode.NotDeclared, nameToken.getStartPos());
                    } else {
                        type = return_type;
                    }
                } else { //有参数调用
                    TokenType param0 = analyseExpression(true); //
                    int i = 0;
                    if (param0 != call_array.get(i)) {
                        throw new AnalyzeError(ErrorCode.NotDeclared, nameToken.getStartPos());
                    }
                    while (nextIf(TokenType.COMMA) != null) {
                        i++;
                        if (call_array.size() < i) {
                            throw new AnalyzeError(ErrorCode.NotDeclared, nameToken.getStartPos());
                        }
                        TokenType param = analyseExpression(true);
                        if (param != call_array.get(i)) {
                            throw new AnalyzeError(ErrorCode.NotDeclared, nameToken.getStartPos());
                        }
                    }
                    expect(TokenType.R_PAREN);
                    type = return_type;
                }
                if(index != null){
                    CurrentFnInstruction.add(new Instruction(Operation.call, symbolTable.get(index).getFnoffset()));
                }else{
                    CurrentFnInstruction.add(new Instruction(Operation.callname, currentGlobal));
                }
            } else {
                if (index == null) {
                    throw new AnalyzeError(ErrorCode.NotDeclared, nameToken.getStartPos());
                }
                Symbol symbol = symbolTable.get(index);

                if(symbol.getSymbolType() == SymbolType.global){ //取地址
                    CurrentFnInstruction.add(new Instruction(Operation.globa, symbol.getOffset()));
                }else if(symbol.getSymbolType() == SymbolType.local){
                    CurrentFnInstruction.add(new Instruction(Operation.loca, symbol.getOffset()));
                }else{
                    CurrentFnInstruction.add(new Instruction(Operation.arga, symbol.getOffset()));
                }

                CurrentFnInstruction.add(new Instruction(Operation.load64)); //取值

                type = symbolTable.get(index).getType();
            }
        }

        else if (peek().getTokenType() == TokenType.UINT_LITERAL || peek().getTokenType() == TokenType.STRING_LITERAL || peek().getTokenType() == TokenType.DOUBLE_LITERAL || peek().getTokenType() == TokenType.CHAR_LITERAL) {
            if (peek().getTokenType() == TokenType.UINT_LITERAL) {
                type = TokenType.INT;
                CurrentFnInstruction.add(new Instruction(Operation.push, peek().getValue()));
                next();
            } else if (peek().getTokenType() == TokenType.STRING_LITERAL) {
                GlobalVariable.add(peek().getValue().toString());
                globalOffset++;
                type = TokenType.INT;
                CurrentFnInstruction.add(new Instruction(Operation.push, (long)globalOffset-1));
                next();
            } else if (peek().getTokenType() == TokenType.DOUBLE_LITERAL) {
                type = TokenType.DOUBLE;
                CurrentFnInstruction.add(new Instruction(Operation.push, Double.doubleToRawLongBits((double)peek().getValue())));
                next();
            } else if (peek().getTokenType() == TokenType.CHAR_LITERAL) {
                type = TokenType.INT;
                CurrentFnInstruction.add(new Instruction(Operation.push, (long)(char)peek().getValue()));
                next();
            }
        }
        else if (check(TokenType.L_PAREN)) {
            type = analyseGroupExpression();
        }
        if (f) {
            Stack stack = new Stack();
            stack.push('#');
            Stack Nstack = new Stack<>();
            if (type != null) {
                Nstack.push(type);
            }
            while (check(TokenType.AS_KW) || check(TokenType.PLUS) || check(TokenType.MINUS) || check(TokenType.MUL) || check(TokenType.DIV) || check(TokenType.EQ) || check(TokenType.NEQ) || check(TokenType.LT) || check(TokenType.GT) || check(TokenType.LE) || check(TokenType.GE)) {
                OPGAnalyse(stack, Nstack);
                TokenType second_type = analyseExpression(false);

                if (second_type != null) {
                    Nstack.push(second_type);
                    second_type = null; //还原
                }

            }
            int sch = Symbol.indexOf(stack.peek());
            int ch = Symbol.indexOf(peek().getTokenType());
            while ((ch == -1 || SymbolMatrix[sch][ch] == 1) && stack.size() > 1) { //栈内大于当前 规约
                reduction(stack, Nstack);
            }
            type = (TokenType) Nstack.pop();
        }
        return type;
    }

    private void OPGAnalyse(Stack<TokenType> s, Stack Ns) throws TokenizeError {
        int sch = Symbol.indexOf(s.peek());
        int ch = Symbol.indexOf(peek().getTokenType());

        if (sch == -1 && ch == -1) {
            return ;
        } else if (sch == -1 || SymbolMatrix[sch][ch] == 0) {
            s.push(Symbol.get(ch));
            next();
        } else {
            while ((ch == -1 || SymbolMatrix[sch][ch] == 1) && s.size() > 1) {
                reduction(s, Ns);
            }
        }
    }

    private void reduction(Stack<TokenType> s, Stack<Object> Ns) {
        TokenType pop = s.pop();

        TokenType pop2 = (TokenType) Ns.pop();

        TokenType pop1 = (TokenType) Ns.pop();

        TokenType push = null;

        if (pop == TokenType.AS_KW) {
            if (pop1 == TokenType.DOUBLE || pop1 == TokenType.INT) {
                if (pop2 == TokenType.DOUBLE) {
                    push = TokenType.DOUBLE;
                    if(pop1 == TokenType.INT){
                        CurrentFnInstruction.add(new Instruction(Operation.itof));
                    }
                }
                if (pop2 == TokenType.INT) {
                    push = TokenType.INT;
                    if(pop1 == TokenType.DOUBLE){
                        CurrentFnInstruction.add(new Instruction(Operation.ftoi));
                    }
                }
            } else {
                System.exit(-1);
            }
        } else {
            if (pop1 != pop2) {
                System.exit(-1);
            }
            switch (pop) {
                case PLUS:
                    if(pop1 == TokenType.INT){
                        push = TokenType.INT;
                        CurrentFnInstruction.add(new Instruction(Operation.addi));
                    }else{
                        push = TokenType.DOUBLE;
                        CurrentFnInstruction.add(new Instruction(Operation.addf));
                    }
                    break;
                case MINUS:
                    if(pop1 == TokenType.INT){
                        push = TokenType.INT;
                        CurrentFnInstruction.add(new Instruction(Operation.subi));
                    }else{
                        push = TokenType.DOUBLE;
                        CurrentFnInstruction.add(new Instruction(Operation.subf));
                    }
                    break;
                case MUL:
                    if(pop1 == TokenType.INT){
                        push = TokenType.INT;
                        CurrentFnInstruction.add(new Instruction(Operation.muli));
                    }else{
                        push = TokenType.DOUBLE;
                        CurrentFnInstruction.add(new Instruction(Operation.mulf));
                    }
                    break;
                case DIV:
                    if(pop1 == TokenType.INT){
                        push = TokenType.INT;
                        CurrentFnInstruction.add(new Instruction(Operation.divi));
                    }else{
                        push = TokenType.DOUBLE;
                        CurrentFnInstruction.add(new Instruction(Operation.divf));
                    }
                    break;
                case EQ:
                    if(pop1 == TokenType.INT){
                        push = TokenType.BOOL;
                        CurrentFnInstruction.add(new Instruction(Operation.cmpi));
                        CurrentFnInstruction.add(new Instruction(Operation.not));
                    }else{
                        push = TokenType.BOOL;
                        CurrentFnInstruction.add(new Instruction(Operation.cmpf));
                        CurrentFnInstruction.add(new Instruction(Operation.not));
                    }
                    break;
                case NEQ:
                    if(pop1 == TokenType.INT){
                        push = TokenType.BOOL;
                        CurrentFnInstruction.add(new Instruction(Operation.cmpi));
                    }else{
                        push = TokenType.BOOL;
                        CurrentFnInstruction.add(new Instruction(Operation.cmpf));
                    }
                    break;
                case LT:
                    if(pop1 == TokenType.INT){
                        push = TokenType.BOOL;
                        CurrentFnInstruction.add(new Instruction(Operation.cmpi));
                        CurrentFnInstruction.add(new Instruction(Operation.setlt));
                    }else{
                        push = TokenType.BOOL;
                        CurrentFnInstruction.add(new Instruction(Operation.cmpf));
                        CurrentFnInstruction.add(new Instruction(Operation.setlt));
                    }
                    break;
                case GT:
                    if(pop1 == TokenType.INT){
                        push = TokenType.BOOL;
                        CurrentFnInstruction.add(new Instruction(Operation.cmpi));
                        CurrentFnInstruction.add(new Instruction(Operation.setgt));
                    }else{
                        push = TokenType.BOOL;
                        CurrentFnInstruction.add(new Instruction(Operation.cmpf));
                        CurrentFnInstruction.add(new Instruction(Operation.setgt));
                    }
                    break;
                case LE:
                    if(pop1 == TokenType.INT){
                        push = TokenType.BOOL;
                        CurrentFnInstruction.add(new Instruction(Operation.cmpi));
                        CurrentFnInstruction.add(new Instruction(Operation.setgt));
                        CurrentFnInstruction.add(new Instruction(Operation.not));
                    }else{
                        push = TokenType.BOOL;
                        CurrentFnInstruction.add(new Instruction(Operation.cmpf));
                        CurrentFnInstruction.add(new Instruction(Operation.setgt));
                        CurrentFnInstruction.add(new Instruction(Operation.not));
                    }
                    break;
                case GE:
                    if(pop1 == TokenType.INT){
                        push = TokenType.BOOL;
                        CurrentFnInstruction.add(new Instruction(Operation.cmpi));
                        CurrentFnInstruction.add(new Instruction(Operation.setlt));
                        CurrentFnInstruction.add(new Instruction(Operation.not));
                    }else{
                        push = TokenType.BOOL;
                        CurrentFnInstruction.add(new Instruction(Operation.cmpf));
                        CurrentFnInstruction.add(new Instruction(Operation.setlt));
                        CurrentFnInstruction.add(new Instruction(Operation.not));
                    }
                    break;
                default:
                    System.exit(-1);
            }
        }
        Ns.push(push);

    }

//    public static boolean belongToOperator(TokenType tokenType){
//        TokenType[] binary_operator = TokenType.values();
//        for(int i = operatorStart; i <= operatorEnd;i++){
//            if(tokenType== binary_operator[i]){
//                return true;
//            }
//        }
//        return false;
//    }


    private TokenType analyseNegateExpression() throws CompileError {
        expect(TokenType.MINUS);
        return analyseExpression(true);
    }

//    private Token analyseAssignExpression(SymbolBlock symbolBlock) throws CompileError {
//        expect(TokenType.ASSIGN);
//
//        return analyseExpression();
//    }

    private Token analyseASExpression(SymbolBlock symbolBlock) throws CompileError {
        expect(TokenType.AS_KW);

        return expectTyWithoutVoid();

    }

//    private Token analyseCallExpression(Token function) throws CompileError {
//
//        HashMap<String,SymbolEntry> funTable = new HashMap<>();
//
//        symbolTable.add(funTable);
//
//        expect(TokenType.L_PAREN);
//
//        SymbolFunction fun = symbolFunctionTable.get(function.getValueString());
//        int i = 0;
//        int l = fun.paramList.size();
//
//        Token param = null;
//
//        if(nextIsExpr()) {
//            i = addParam_i(fun, i, l);
//        }
//
//        peekedToken = peek();
//
//        while(peekedToken.getTokenType() == TokenType.COMMA){
//            next();
//            i = addParam_i(fun, i, l);
//        }
//
//        expect(TokenType.R_PAREN);
//
//        //TODO:no value
//        return new Token(fun.returnType,"",function.getStartPos(),function.getEndPos());
//    }

//    private int addParam_i(SymbolFunction fun, int i, int l) throws CompileError {
//        Token param;
//        if(i + 1 >= l)
//            throw new AnalyzeError(ErrorCode.ParamsOutOfRange,peek().getStartPos());
//        param = analyseExpression();
//        if(param.getTokenType() != fun.paramList.get(i).getType())
//            throw new AnalyzeError(ErrorCode.ParamTypeMismatched,param.getStartPos());
//        SymbolEntry param1 = new SymbolEntry(false,true,param.getValueString(),param.getTokenType(),getNextVariableOffset());
//        symbolTable.get(symbolTable.size() - 1).put(fun.paramList.get(i).getName(),param1);
//        new Instruction();
//        i++;
//        return i;
//    }

//    private Token analyseLiteralExpression() throws CompileError {
//        return expect(TokenType.UINT_LITERAL,TokenType.STRING_LITERAL,TokenType.DOUBLE_LITERAL);
//    }

//    private Token analyseIdentExpression() throws CompileError {
//        Token ident = expect(TokenType.IDENT);
//
//        peekedToken = peek();
//        if(peekedToken.getTokenType() == TokenType.L_PAREN) {
//            if(funIsDeclared(ident.getValueString()) == null)
//                throw new AnalyzeError(ErrorCode.NoFunction,ident.getStartPos());
//            return analyseCallExpression(ident);
//        }
//        else {
//            if(varIsDeclared(ident.getValueString(),ident.getStartPos()) == null)
//                throw new AnalyzeError(ErrorCode.NotDeclared,ident.getStartPos());
//            if (peekedToken.getTokenType() == TokenType.ASSIGN) {
//                Token result = analyseAssignExpression();
//                if(result.getTokenType() != ident.getTokenType())
//                    throw new AnalyzeError(ErrorCode.MismatchedAssignmentType,result.getStartPos());
//                ident.setValue(result.getValue());
//            }
//            return ident;
//            //TODO
//            new Instruction();
//        }
//
//    }

    private TokenType analyseGroupExpression() throws CompileError {
        expect(TokenType.L_PAREN);
        TokenType tokenType = analyseExpression(true);
        expect(TokenType.R_PAREN);
        return tokenType;
    }
}
