package miniplc0java.analyser;

import miniplc0java.error.AnalyzeError;
import miniplc0java.error.CompileError;
import miniplc0java.error.ErrorCode;
import miniplc0java.error.ExpectedTokenError;
import miniplc0java.error.TokenizeError;
import miniplc0java.instruction.Instruction;
import miniplc0java.instruction.Operation;
import miniplc0java.tokenizer.Token;
import miniplc0java.tokenizer.TokenType;
import miniplc0java.tokenizer.Tokenizer;
import miniplc0java.util.Pos;

import java.util.*;

public final class Analyser {

    Tokenizer tokenizer;
    ArrayList<Instruction> instructions;

    private static int operatorStart = 15;
    private static int operatorEnd = 24;

    /** 当前偷看的 token */
    Token peekedToken = null;

    /** 符号表 */
    HashMap<String, SymbolEntry> symbolTable = new HashMap<>();

    /** 下一个变量的栈偏移 */
    int nextOffset = 0;

    public Analyser(Tokenizer tokenizer) {
        this.tokenizer = tokenizer;
        this.instructions = new ArrayList<>();
    }

    public List<Instruction> analyse() throws CompileError {
        analyseProgram();
        return instructions;
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
     * @param isInitialized 是否已赋值
     * @param isConstant    是否是常量
     * @param curPos        当前 token 的位置（报错用）
     * @throws AnalyzeError 如果重复定义了则抛异常
     */
    private void addSymbol(String name, boolean isInitialized, boolean isConstant, Pos curPos) throws AnalyzeError {
        if (this.symbolTable.get(name) != null) {
            throw new AnalyzeError(ErrorCode.DuplicateDeclaration, curPos);
        } else {
            this.symbolTable.put(name, new SymbolEntry(isConstant, isInitialized, getNextVariableOffset()));
        }
    }

    /**
     * 设置符号为已赋值
     * 
     * @param name   符号名称
     * @param curPos 当前位置（报错用）
     * @throws AnalyzeError 如果未定义则抛异常
     */
    private void initializeSymbol(String name, Pos curPos) throws AnalyzeError {
        var entry = this.symbolTable.get(name);
        if (entry == null) {
            throw new AnalyzeError(ErrorCode.NotDeclared, curPos);
        } else {
            entry.setInitialized(true);
        }
    }

    /**
     * 获取变量在栈上的偏移
     * 
     * @param name   符号名
     * @param curPos 当前位置（报错用）
     * @return 栈偏移
     * @throws AnalyzeError
     */
    private int getOffset(String name, Pos curPos) throws AnalyzeError {
        var entry = this.symbolTable.get(name);
        if (entry == null) {
            throw new AnalyzeError(ErrorCode.NotDeclared, curPos);
        } else {
            return entry.getStackOffset();
        }
    }

    /**
     * 获取变量是否是常量
     * 
     * @param name   符号名
     * @param curPos 当前位置（报错用）
     * @return 是否为常量
     * @throws AnalyzeError
     */
    private boolean isConstant(String name, Pos curPos) throws AnalyzeError {
        var entry = this.symbolTable.get(name);
        if (entry == null) {
            throw new AnalyzeError(ErrorCode.NotDeclared, curPos);
        } else {
            return entry.isConstant();
        }
    }

    private void analyseProgram() throws CompileError {

        while(nextIsDeclStmt() || nextIsFunction()){
            if(nextIsFunction())
                analyseFunction();
            else if(nextIsDeclStmt())
                analyseDeclStmt();
        }

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

    private void analyseMain() throws CompileError {
        // 主过程 -> 常量声明 变量声明 语句序列
        throw new Error("Not implemented");
    }

    private void analyseConstantDeclaration() throws CompileError {
        // 示例函数，示例如何解析常量声明
        // 常量声明 -> 常量声明语句*

        // 如果下一个 token 是 const 就继续
        while (nextIf(TokenType.Const) != null) {
            // 常量声明语句 -> 'const' 变量名 '=' 常表达式 ';'

            // 变量名
            var nameToken = expect(TokenType.Ident);

            // 加入符号表
            String name = (String) nameToken.getValue();
            addSymbol(name, true, true, nameToken.getStartPos());

            // 等于号
            expect(TokenType.Equal);

            // 常表达式
            var value = analyseConstantExpression();

            // 分号
            expect(TokenType.Semicolon);

            // 这里把常量值直接放进栈里，位置和符号表记录的一样。
            // 更高级的程序还可以把常量的值记录下来，遇到相应的变量直接替换成这个常数值，
            // 我们这里就先不这么干了。
            instructions.add(new Instruction(Operation.LIT, value));
        }
    }

    private void analyseVariableDeclaration() throws CompileError {
        // 变量声明 -> 变量声明语句*

        // 如果下一个 token 是 var 就继续
        while (nextIf(TokenType.Var) != null) {
            // 变量声明语句 -> 'var' 变量名 ('=' 表达式)? ';'

            // 变量名

            // 变量初始化了吗
            boolean initialized = false;

            // 下个 token 是等于号吗？如果是的话分析初始化

            // 分析初始化的表达式

            // 分号
            expect(TokenType.Semicolon);

            // 加入符号表，请填写名字和当前位置（报错用）
            String name = /* 名字 */ null;
            addSymbol(name, false, false, /* 当前位置 */ null);

            // 如果没有初始化的话在栈里推入一个初始值
            if (!initialized) {
                instructions.add(new Instruction(Operation.LIT, 0));
            }
        }
    }

    private void analyseFunction() throws CompileError {
        expect(TokenType.FN_KW);

        expect(TokenType.IDENT);

        expect(TokenType.L_PAREN);

        if(nextIsFunctionParam())
            analyseFunctionParamList();

        expect(TokenType.R_PAREN);

        expect(TokenType.ARROW);

        expectTy();

        analyseBlockStmt();

    }

    private boolean nextIsFunctionParam() throws TokenizeError {
        peekedToken = peek();
        return peekedToken.getTokenType() == TokenType.CONST_KW ||
                peekedToken.getTokenType() == TokenType.IDENT;
    }

    private void analyseFunctionParamList() throws CompileError {
        analyseFunctionParm();

        peekedToken = peek();

        while(peekedToken.getTokenType() == TokenType.COMMA){
            next();
            analyseFunctionParm();
            peekedToken = peek();
        }
    }

    private void analyseFunctionParm() throws CompileError {
        peekedToken = peek();
        if(peekedToken.getTokenType() == TokenType.CONST_KW)
            expect(TokenType.CONST_KW);

        expect(TokenType.IDENT);

        expect(TokenType.COLON);

        expectTy();

    }

    private void expectTy() throws CompileError {
        expect(TokenType.INT,TokenType.VOID);
    }

    private void analyseStatement() throws CompileError {

        if(nextIsExpr())
            analyseExprStmt();
        else {
            peekedToken = peek();
            switch (peekedToken.getTokenType()) {
                case LET_KW:
                case CONST_KW:
                    analyseDeclStmt();
                    break;
                case IF_KW:
                    analyseIfStmt();
                    break;
                case WHILE_KW:
                    analyseWhileStmt();
                    break;
                case RETURN_KW:
                    analyseReturnStmt();
                    break;
                case L_BRACE:
                    analyseBlockStmt();
                    break;
                case SEMICOLON:
                    analyseEmptyStmt();
                    break;
                default:
                    throw new AnalyzeError(ErrorCode.InvalidInput,peekedToken.getStartPos());
            }
        }

    }

    private void analyseExprStmt() throws CompileError {
        analyseExpression();

        expect(TokenType.SEMICOLON);
    }

    private void analyseDeclStmt() throws CompileError {
        peekedToken = peek();
        if(peekedToken.getTokenType() == TokenType.LET_KW)
            analyseLetDeclStmt();
        else if(peekedToken.getTokenType() == TokenType.CONST_KW)
            analyseConstDeclStmt();
        else
            throw new AnalyzeError(ErrorCode.InvalidInput,peekedToken.getStartPos());
    }


    private void analyseIfStmt() throws CompileError {
        expect(TokenType.IF_KW);

        analyseExpression();

        analyseBlockStmt();

        peekedToken = peek();
        if(peekedToken.getTokenType() == TokenType.ELSE_KW){
            next();
            peekedToken = peek();
            if(peekedToken.getTokenType() == TokenType.L_BRACE)
                analyseBlockStmt();
            else if(peekedToken.getTokenType() == TokenType.IF_KW)
                analyseIfStmt();
            else
                throw new AnalyzeError(ErrorCode.InvalidInput,peekedToken.getStartPos());
        }
    }

    private void analyseConstDeclStmt() throws CompileError {
        expect(TokenType.CONST_KW);

        expect(TokenType.IDENT);

        expect(TokenType.COLON);

        expectTy();

        expect(TokenType.ASSIGN);

        analyseExpression();

        expect(TokenType.SEMICOLON);
    }

    private void analyseLetDeclStmt() throws CompileError {
        expect(TokenType.LET_KW);

        expect(TokenType.IDENT);

        expect(TokenType.COLON);

        expect(TokenType.INT,TokenType.VOID);

        peekedToken = peek();
        while(peekedToken.getTokenType() == TokenType.ASSIGN){
            next();
            analyseExpression();
            peekedToken = peek();
        }

        expect(TokenType.SEMICOLON);
    }


    private void analyseWhileStmt() throws CompileError {
        expect(TokenType.WHILE_KW);

        analyseExpression();

        analyseBlockStmt();
    }


    private void analyseReturnStmt() throws CompileError {
        expect(TokenType.RETURN_KW);

        if(nextIsExpr())
            analyseExpression();

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
                return true;
            default:
                return false;
        }
    }


    private void analyseBlockStmt() throws CompileError {
        expect(TokenType.L_BRACE);

        while(nextIsStmt()){
            analyseStatement();
        }
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
            case L_BRACE:
            case SEMICOLON:
                return true;
            default:
                return false;
        }
    }

    
    private void analyseExpression() throws CompileError {
        peekedToken = peek();
        switch (peekedToken.getTokenType()){
            case IDENT:
                analyseIdentExpression();
                break;
            case MINUS:
                analyseNegateExpression();
                break;
            case L_PAREN:
                analyseGroupExpression();
                break;
            default:
                analyseLiteralExpression();
        }

        peekedToken = peek();
        while(belongToOperator(peekedToken.getTokenType()) || peekedToken.getTokenType() == TokenType.AS_KW) {
            if (belongToOperator(peekedToken.getTokenType())) {
                next();
                analyseExpression();
            } else if (peekedToken.getTokenType() == TokenType.AS_KW) {
                analyseASExpression();
            }
            peekedToken = peek();
        }

    }

    private boolean belongToOperator(TokenType tokenType){
        TokenType[] binary_operator = TokenType.values();
        for(int i = operatorStart; i <= operatorEnd;i++){
            if(tokenType== binary_operator[i]){
                return true;
            }
        }
        return false;
    }

    private void analyseAssignmentStatement() throws CompileError {
        // 赋值语句 -> 标识符 '=' 表达式 ';'

        // 分析这个语句

        // 标识符是什么？
        String name = null;
        var symbol = symbolTable.get(name);
        if (symbol == null) {
            // 没有这个标识符
            throw new AnalyzeError(ErrorCode.NotDeclared, /* 当前位置 */ null);
        } else if (symbol.isConstant) {
            // 标识符是常量
            throw new AnalyzeError(ErrorCode.AssignToConstant, /* 当前位置 */ null);
        }
        // 设置符号已初始化
        initializeSymbol(name, null);

        // 把结果保存
        var offset = getOffset(name, null);
        instructions.add(new Instruction(Operation.STO, offset));
    }

    private void analyseOutputStatement() throws CompileError {
        // 输出语句 -> 'print' '(' 表达式 ')' ';'

        expect(TokenType.Print);
        expect(TokenType.LParen);

        analyseExpression();

        expect(TokenType.RParen);
        expect(TokenType.Semicolon);

        instructions.add(new Instruction(Operation.WRT));
    }

    private void analyseItem() throws CompileError {
        // 项 -> 因子 (乘法运算符 因子)*

        // 因子

        while (true) {
            // 预读可能是运算符的 token
            Token op = null;

            // 运算符

            // 因子

            // 生成代码
            if (op.getTokenType() == TokenType.Mult) {
                instructions.add(new Instruction(Operation.MUL));
            } else if (op.getTokenType() == TokenType.Div) {
                instructions.add(new Instruction(Operation.DIV));
            }
        }
    }

    private void analyseFactor() throws CompileError {
        // 因子 -> 符号? (标识符 | 无符号整数 | '(' 表达式 ')')

        boolean negate;
        if (nextIf(TokenType.Minus) != null) {
            negate = true;
            // 计算结果需要被 0 减
            instructions.add(new Instruction(Operation.LIT, 0));
        } else {
            nextIf(TokenType.Plus);
            negate = false;
        }

        if (check(TokenType.Ident)) {
            // 是标识符

            // 加载标识符的值
            String name = /* 快填 */ null;
            var symbol = symbolTable.get(name);
            if (symbol == null) {
                // 没有这个标识符
                throw new AnalyzeError(ErrorCode.NotDeclared, /* 当前位置 */ null);
            } else if (!symbol.isInitialized) {
                // 标识符没初始化
                throw new AnalyzeError(ErrorCode.NotInitialized, /* 当前位置 */ null);
            }
            var offset = getOffset(name, null);
            instructions.add(new Instruction(Operation.LOD, offset));
        } else if (check(TokenType.Uint)) {
            // 是整数
            // 加载整数值
            int value = 0;
            instructions.add(new Instruction(Operation.LIT, value));
        } else if (check(TokenType.LParen)) {
            // 是表达式
            // 调用相应的处理函数
        } else {
            // 都不是，摸了
            throw new ExpectedTokenError(List.of(TokenType.Ident, TokenType.Uint, TokenType.LParen), next());
        }

        if (negate) {
            instructions.add(new Instruction(Operation.SUB));
        }
        throw new Error("Not implemented");
    }

    private void analyseOperatorExpression(){

    }

    private void analyseNegateExpression() throws CompileError {
        expect(TokenType.MINUS);

        analyseExpression();
    }

    private void analyseAssignExpression() throws CompileError {
        expect(TokenType.ASSIGN);

        analyseExpression();
    }

    private void analyseASExpression() throws CompileError {
        expect(TokenType.AS_KW);

        expectTy();

    }

    private void analyseCallExpression() throws CompileError {

        expect(TokenType.L_PAREN);

        analyseExpression();

        peekedToken = peek();

        while(peekedToken.getTokenType() == TokenType.COMMA){
            next();
            analyseExpression();
        }

        expect(TokenType.R_PAREN);

    }

    private void analyseLiteralExpression() throws CompileError {
        expect(TokenType.UINT_LITERAL,TokenType.STRING_LITERAL);
    }

    private void analyseIdentExpression() throws CompileError {
        expect(TokenType.IDENT);

        peekedToken = peek();
        if(peekedToken.getTokenType() == TokenType.L_PAREN)
            analyseCallExpression();
        else if(peekedToken.getTokenType() == TokenType.ASSIGN)
            analyseAssignExpression();

    }

    private void analyseGroupExpression() throws CompileError {
        expect(TokenType.L_PAREN);

        analyseExpression();

        expect(TokenType.R_PAREN);
    }
}
