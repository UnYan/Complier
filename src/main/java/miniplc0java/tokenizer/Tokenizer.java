package miniplc0java.tokenizer;

import miniplc0java.error.TokenizeError;
import miniplc0java.error.ErrorCode;
import miniplc0java.util.Pos;

import java.awt.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Tokenizer {

    private static int identStart = 1;
    private static int identEnd = 10;
    private StringIter it;

    public Tokenizer(StringIter it) {
        this.it = it;
    }

    // 这里本来是想实现 Iterator<Token> 的，但是 Iterator 不允许抛异常，于是就这样了
    /**
     * 获取下一个 Token
     * 
     * @return
     * @throws TokenizeError 如果解析有异常则抛出
     */
    public Token nextToken() throws TokenizeError {
        it.readAll();

        // 跳过之前的所有空白字符
        skipSpaceCharacters();

        if (it.isEOF()) {
            return new Token(TokenType.EOF, "", it.currentPos(), it.currentPos());
        }

        char peek = it.peekChar();
        if(peek == '"')
            return lexSTRING();
        else if(peek == '\'') {
            return lexCHAR();
        }
        else if (Character.isDigit(peek)) {
            return lexUIntORDouble();
        } else if (Character.isAlphabetic(peek)) {
            return lexIdentOrKeyword();
        }
        else if(peek == '_') {
            return lexIdentOrKeyword();
        }
        else {
            return lexOperatorOrUnknown();
        }
    }



    private Token lexSTRING() throws TokenizeError{
        Pos start = it.currentPos();
        Pos end ;
        char peek = it.peekChar();
        String escape_sequence = "\\[\\\\\"'nrt]";
        String string_regular_char = "[^\"\\\\\r\n\t]";
        StringBuilder STRING_LITERAL = new StringBuilder();
        if(it.peekChar() != '"') {
            throw new TokenizeError(ErrorCode.InvalidInput, it.previousPos());
        }
        it.nextChar();
        peek = it.peekChar();
        while(Pattern.matches(string_regular_char,String.valueOf(peek))
                || peek == '\\'){
            char now ;
            if(peek == '\\'){
                it.nextChar();
                peek = it.peekChar();
                switch (peek){
                    case '\\':
                        now = '\\';
                        break;
                    case '\"':
                        now = '\"';
                        break;
                    case '\'':
                        now = '\'';
                        break;
                    case 'n':
                        now = '\n';
                        break;
                    case 'r':
                        now = '\r';
                        break;
                    case 't':
                        now = '\t';
                        break;
                    default:
                        throw new TokenizeError(ErrorCode.InvalidInput, it.previousPos());
                }
                STRING_LITERAL.append(now);
                it.nextChar();
            }
            else
                STRING_LITERAL.append(it.nextChar());
            peek = it.peekChar();
        }
        if(it.peekChar() == '"') {
            it.nextChar();
            end = it.currentPos();
            return new Token(TokenType.STRING_LITERAL, STRING_LITERAL, start, end);
        }
        else
            throw new TokenizeError(ErrorCode.InvalidInput, it.previousPos());
    }

    private Token lexCHAR() throws TokenizeError {
        //token.setLength(0);
        String token = "";
        Pos start = it.currentPos();

        it.nextChar();
        char peek = it.peekChar();

        if(peek != '\'' && peek != '\\'){
            Token toke = new Token(TokenType.CHAR_LITERAL, it.nextChar(), start, it.currentPos());
        }
        else if(peek == '\\'){
            it.nextChar();
            Pattern escape_sequence = Pattern.compile("[\\\\\"'nrt]");
            peek = it.peekChar();
            Matcher mat = escape_sequence.matcher(String.valueOf(peek));
            boolean matches = mat.matches();
            char cur ;

            if(matches) {
                if (peek == '\\') {
                    cur = '\\';
                } else if (peek == 'n') {
                    cur = '\n';
                } else if (peek == 't') {
                    cur = '\t';
                } else if (peek == '"') {
                    cur = '"';
                } else if (peek == 'r') {
                    cur = '\r';
                } else if (peek == '\'') {
                    cur = '\'';
                } else {
                    throw new TokenizeError(ErrorCode.InvalidInput, it.previousPos());
                }
                it.nextChar();
                return new Token(TokenType.CHAR_LITERAL, cur, start, it.currentPos());
            }
        }
        return null;
    }

    private Token lexUIntORDouble() throws TokenizeError {
        // 请填空：
        // 直到查看下一个字符不是数字为止:
        // -- 前进一个字符，并存储这个字符
        //
        // 解析存储的字符串为无符号整数
        // 解析成功则返回无符号整数类型的token，否则返回编译错误
        //
        // Token 的 Value 应填写数字的值
        Pos start = it.currentPos();
        Pos end ;
        char peek = it.peekChar();
        StringBuilder num = new StringBuilder() ;
        while(Character.isDigit(peek)){
            num.append(it.nextChar());
            peek = it.peekChar();
        }
        if(peek == '.'){
            num.append(it.nextChar());
            peek = it.peekChar();
            if(Character.isDigit(peek)) {
                while (Character.isDigit(peek)) {
                    num.append(it.nextChar());
                    peek = it.peekChar();
                }
                if(peek == 'e' || peek == 'E'){
                    num.append(it.nextChar());
                    peek = it.peekChar();
                    if(peek == '+' || peek == '-'){
                        num.append(it.nextChar());
                        peek = it.peekChar();
                    }
                    if(Character.isDigit(peek)) {
                        while (Character.isDigit(peek)) {
                            num.append(it.nextChar());
                            peek = it.peekChar();
                        }
                        end = it.currentPos();
                        return new Token(TokenType.DOUBLE_LITERAL, Double.valueOf(num.toString()), start, end);
                    }
                    else
                        throw new TokenizeError(ErrorCode.InvalidInput,it.previousPos());
                }
                else {
                    end = it.currentPos();
                    return new Token(TokenType.DOUBLE_LITERAL, Double.valueOf(num.toString()), start, end);
                }
            }
            else
                throw new TokenizeError(ErrorCode.InvalidInput,it.previousPos());

        }
        if(num.length() != 0) {
            end = it.currentPos();
            return new Token(TokenType.UINT_LITERAL, Long.valueOf(num.toString()), start, end);
        }
        return null;

    }

    private Token lexIdentOrKeyword() throws TokenizeError {
        // 请填空：
        // 直到查看下一个字符不是数字或字母为止:
        // -- 前进一个字符，并存储这个字符
        //
        // 尝试将存储的字符串解释为关键字
        // -- 如果是关键字，则返回关键字类型的 token
        // -- 否则，返回标识符
        //
        // Token 的 Value 应填写标识符或关键字的字符串
        Pos start = it.currentPos();
        Pos end ;
        StringBuilder token = new StringBuilder();
        char peek = it.peekChar();
        while(Character.isAlphabetic(peek) || Character.isDigit(peek) || peek == '_'){
            token.append(it.nextChar());
            peek = it.peekChar();
        }
        end = it.currentPos();
//        if("int".equals(token.toString()))
//            return new Token(TokenType.INT, token, start, end);
//        if("void".equals(token.toString()))
//            return new Token(TokenType.VOID, token, start, end);
//        if("string".equals(token.toString()))
//            return new Token(TokenType.STRING, token, start, end);
//        if("double".equals(token.toString()))
//            return new Token(TokenType.DOUBLE, token, start, end);
        TokenType[] KW = TokenType.values();
        for(int i = identStart; i <= identEnd;i++){
            if(KW[i].toString().equalsIgnoreCase(token.toString())) {
                return new Token(KW[i], token.toString(), start, end);
            }
        }
        return new Token(TokenType.IDENT, token.toString(), start, end);
    }

    //ok
    private Token lexOperatorOrUnknown() throws TokenizeError {
        switch (it.nextChar()) {
            case '+':
                return new Token(TokenType.PLUS, '+', it.previousPos(), it.currentPos());

            case '-':
                // 填入返回语句
                if(it.peekChar() == '>'){
                    it.nextChar();
                    return new Token(TokenType.ARROW, "->", it.previousPos(), it.currentPos());
                }
                else
                    return new Token(TokenType.MINUS, '-', it.previousPos(), it.currentPos());
//                throw new Error("Not implemented");

            case '*':
                // 填入返回语句
                return new Token(TokenType.MUL, '*', it.previousPos(), it.currentPos());

            case '/':
                // 填入返回语句
                return new Token(TokenType.DIV, '/', it.previousPos(), it.currentPos());
//                throw new Error("Not implemented");

            // 填入更多状态和返回语句
            case '=':
                if(it.peekChar() == '=') {
                    it.nextChar();
                    return new Token(TokenType.EQ, "==", it.previousPos(), it.currentPos());
                }
                else
                    return new Token(TokenType.ASSIGN, "=", it.previousPos(), it.currentPos());
            case '!':
                if(it.peekChar() == '='){
                    it.nextChar();
                    return new Token(TokenType.NEQ, "!=", it.previousPos(), it.currentPos());
                }
                else
                    throw new TokenizeError(ErrorCode.InvalidInput, it.previousPos());

            case '<':
                if(it.peekChar() == '='){
                    it.nextChar();
                    return new Token(TokenType.LE, "<=",it.previousPos(), it.currentPos());
                }
                else
                    return new Token(TokenType.LT, '<',it.previousPos(), it.currentPos());

            case '>':
                if(it.peekChar() == '='){
                    it.nextChar();
                    return new Token(TokenType.GE, ">=",it.previousPos(), it.currentPos());
                }
                else
                    return new Token(TokenType.GT, '>',it.previousPos(), it.currentPos());

            case '(':
                return new Token(TokenType.L_PAREN, '(',it.previousPos(), it.currentPos());

            case ')':
                return new Token(TokenType.R_PAREN, ')',it.previousPos(), it.currentPos());

            case '{':
                return new Token(TokenType.L_BRACE, '{',it.previousPos(), it.currentPos());

            case '}':
                return new Token(TokenType.R_BRACE, '}',it.previousPos(), it.currentPos());

            case ',':
                return new Token(TokenType.COMMA, ',',it.previousPos(), it.currentPos());

            case ':':
                return new Token(TokenType.COLON, ':',it.previousPos(), it.currentPos());

            case ';':
                return new Token(TokenType.SEMICOLON, ';',it.previousPos(), it.currentPos());

            default:
                // 不认识这个输入，摸了
                throw new TokenizeError(ErrorCode.InvalidInput, it.previousPos());
        }
    }

    private void skipSpaceCharacters() {
        while (!it.isEOF() && Character.isWhitespace(it.peekChar())) {
            it.nextChar();
        }
    }
}
