package script;

public class Token
{

    public static final Token EOF = new Token(TokenType.EOF, -1);

    final TokenType type;
    final int pos;
    final Object value;

    protected Token(TokenType type, int pos)
    {
        this(type, pos, null);
    }

    protected Token(TokenType type, int pos, Object value)
    {
        this.type = type;
        this.pos = pos;
        this.value = value;
    }

    public TokenType getType()
    {
        return type;
    }

    public String toString()
    {
        if (value != null)
            return type.toString() + "[" + value + "]";
        else
            return type.toString();
    }

    public static enum TokenType {
        IDENTIFIER, /* identifier */
        PERIOD, /* '.' */
        INT, /* int literal */
        STRING, /* string literal */
        BUILTIN_VAR, /* visible, state */
        BUILTIN_FUNC, /* give, has, take, text, warp */
        ASSIGN, /* '=' */
        VAR, IF, ELSE, /* keywords */
        OP, /* + - * / % & | ^ ~ == < <= > >= != */
        LPAREN, RPAREN, LBRACE, RBRACE, EOF
    }
}
