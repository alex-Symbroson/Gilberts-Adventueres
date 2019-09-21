package script;

import java.util.ArrayList;

import main.GAObject;
import script.Token.TokenType;

public class Script extends ArrayList<Token>
{
    private static final long serialVersionUID = 1L;

    private ScriptEvaluator evaluator;

    public Script(ScriptEvaluator eval)
    {
        this.evaluator = eval;
    }

    // do some simple validity tests
    public boolean testValid()
    {
        // check EOF
        if (get(size() - 1) != Token.EOF || stream().filter(t -> t == Token.EOF).count() != 1) return false;

        // check parentheses and curly braces matching and non-interfering ("( { ) }" invalid)
        int paren = 0, brace = 0;
        for (Token t : this)
        {
            switch (t.type) {
            case LPAREN:
                paren += 2;
            case RPAREN:
                --paren;
                if (brace != 0 || paren < 0) return false;
                break;
            case LBRACE:
                brace += 2;
            case RBRACE:
                --brace;
                if (paren != 0 || brace < 0) return false;
                break;
            default:
            }
        }

        // enforce some restrictions from grammar
        for (int i = 0; i < size() - 1; ++i)
        {
            TokenType t = get(i).type, tx = get(i + 1).type;
            // BUILTIN_FUNC ( ; IF (
            if ((t == TokenType.BUILTIN_FUNC || t == TokenType.IF) && tx != TokenType.LPAREN) return false;
            // ( '...' )
            if (t == TokenType.STRING && (i == 0 || get(i - 1).type != TokenType.LPAREN || tx != TokenType.RPAREN))
                return false;
            // var IDENTIFIER =
            if (t == TokenType.VAR
                    && (i == size() - 2 || tx != TokenType.IDENTIFIER || get(i + 2).type != TokenType.ASSIGN))
                return false;
            // IDENTIFIER . ; IDENTIFIER )
            if (t == TokenType.IDENTIFIER && tx != TokenType.PERIOD && tx != TokenType.RPAREN) return false;
            // . IDENTIFIER ; . visible ; . state
            if (t == TokenType.PERIOD && tx != TokenType.IDENTIFIER && tx != TokenType.BUILTIN_VAR) return false;
            // things after OP
            if (t == TokenType.OP && tx != TokenType.INT && tx != TokenType.OP && tx != TokenType.LPAREN
                    && tx != TokenType.BUILTIN_FUNC && tx != TokenType.IDENTIFIER)
                return false;
        }

        return true;
    }

    public void eval(GAObject object)
    {
        evaluator.setCurrentObject(object);
        evaluator.eval(this);
    }

    /*
     * Script subscript(int fromIndex, int toIndex) { Script sub = new Script(evaluator);
     * sub.addAll(this.subList(fromIndex, toIndex)); sub.add(Token.EOF);
     * 
     * return sub; }
     */
}
