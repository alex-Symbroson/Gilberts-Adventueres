package script;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import main.GALevel;
import main.GAObject;
import main.Main;
import script.Token.TokenType;

public class ScriptEvaluator
{
    private Function<String, GALevel> get_level;
    private Consumer<String> warp_to_level;
    private Consumer<String> write_text;

    private GALevel current_level;
    private GAObject current_object;

    private Script script;
    private int pointer;

    // current token
    Token get()
    {
        return script.get(pointer);
    }

    // current token, also advances pointer
    Token next()
    {
        return script.get(pointer++);
    }

    // evaluate script statements until EOF or }
    public void eval(Map<String, Integer> context)
    {
        while (get() != Token.EOF && get().type != TokenType.RBRACE)
            evalStatement(context);
    }

    // skips statement at current script position
    // will end with script pointer directly after statement
    public void skipStatement()
    {
        Token t = next();
        TokenType type = t.type;
        if (type == TokenType.LBRACE)
            while (get() != Token.EOF && get().type != TokenType.RBRACE)
                skipStatement();
        else if (type == TokenType.IDENTIFIER)
        {
            --pointer;
            // TODO skip properly
            setVar(null);
            assertType(next(), TokenType.ASSIGN);
            skipExpr();
        } else if (type == TokenType.BUILTIN_FUNC)
        {
            assertType(next(), TokenType.LPAREN);
            Token arg = next();
            assertType(next(), TokenType.RPAREN);
            if ("text".equals(t.value))
                assertType(arg, TokenType.STRING);
            else if ("warp".equals(t.value) || "give".equals(t.value) || "take".equals(t.value))
                assertType(arg, TokenType.IDENTIFIER);
            else
                throw new ScriptException("Unknown built-in function at " + t.pos);
        } else if (type == TokenType.IF)
        {
            assertType(next(), TokenType.LPAREN);
            skipExpr();
            assertType(next(), TokenType.RPAREN);
            skipStatement();
            if (get().type == TokenType.ELSE)
            {
                next();
                skipStatement();
            }
        } else
            throw new ScriptException("Not a valid statement at " + t.pos);
    }

    private static Pattern INSERT = Pattern.compile("[^\\\\]%(.*?)%");

    // evaluates statement at current script position
    // will end with script pointer directly after statement
    public void evalStatement(Map<String, Integer> context)
    {
        Token t = next();
        TokenType type = t.type;
        if (type == TokenType.LBRACE)
            eval(new HashMap<>(context));
        else if (type == TokenType.IDENTIFIER)
        {
            --pointer;
            IntConsumer var = setVar(context);
            assertType(next(), TokenType.ASSIGN);
            var.accept(evalExpr(context));
        } else if (type == TokenType.BUILTIN_FUNC && !"has".equals(t.value))
        {
            assertType(next(), TokenType.LPAREN);
            Token arg = next();
            assertType(next(), TokenType.RPAREN);
            if ("text".equals(t.value) && assertType(arg, TokenType.STRING))
            {
                String text = arg.value.toString();
                Matcher m = INSERT.matcher(text);
                text = m.replaceAll(
                        res -> Main.isValidIdentifier(res.group(1)) ? Integer.toString(context.get(res.group(1)))
                                : res.group());
                write_text.accept(text);
            }
            if ("warp".equals(t.value) && assertType(arg, TokenType.IDENTIFIER))
                warp_to_level.accept(arg.value.toString());
            // TODO give, take
        } else if (type == TokenType.IF)
        {
            assertType(next(), TokenType.LPAREN);
            boolean test = evalExpr(context) != 0;
            assertType(next(), TokenType.RPAREN);
            if (test)
                evalStatement(context);
            else
                skipStatement();
            if (get().type == TokenType.ELSE)
            {
                next();
                if (!test)
                    evalStatement(context);
                else
                    skipStatement();
            }
        } else
            throw new ScriptException("Not a valid statement at " + t.pos);
    }

    public void skipExpr()
    {
        return;
    }

    // eval == < <= > >= !=
    public int evalExpr(Map<String, Integer> context)
    {
        int a = evalExprSum(context);
        Token t = get();
        if (t.type == TokenType.OP)
        {
            int index = "== < <= > >= !=".indexOf(t.value.toString());
            if (index == -1) throw new ScriptException("Not a valid operator at " + t.pos);
            next();
            int b = evalExprSum(context);
            boolean res;
            switch (index) {
            case 0:
                res = a == b;
                break;
            case 3:
                res = a < b;
                break;
            case 5:
                res = a <= b;
                break;
            case 8:
                res = a > b;
                break;
            case 10:
                res = a >= b;
                break;
            case 13:
                res = a != b;
                break;
            default:
                res = false;
            }
            return res ? -1 : 0;
        } else
            return a;
    }

    // eval + - & | ^
    private int evalExprSum(Map<String, Integer> context)
    {
        int n = evalExprProd(context);
        Token t;
        while ((t = get()).type == TokenType.OP && "+-&|^".indexOf(t.value.toString()) != -1)
        {
            next();
            int b = evalExprProd(context);
            switch (t.value.toString().charAt(0)) {
            case '+':
                n += b;
                break;
            case '-':
                n -= b;
                break;
            case '&':
                n &= b;
                break;
            case '|':
                n |= b;
                break;
            case '^':
                n ^= b;
            }
        }
        return n;
    }

    // eval * / %
    private int evalExprProd(Map<String, Integer> context)
    {
        int n = evalExprFactor(context);
        Token t;
        while ((t = get()).type == TokenType.OP && "*/%".indexOf(t.value.toString()) != -1)
        {
            next();
            int b = evalExprFactor(context);
            switch (t.value.toString().charAt(0)) {
            case '*':
                n *= b;
                break;
            case '/':
                n /= b;
                break;
            case '%':
                n %= b;
            }
        }
        return n;
    }

    private int evalExprFactor(Map<String, Integer> context)
    {
        List<Boolean> pre_op = new ArrayList<>();
        Token t;
        while ((t = get()).type == TokenType.OP && "-".equals(t.value) || "~".equals(t.value))
        {
            next();
            pre_op.add("-".equals(t.value));
        }
        t = get();
        if (t.type == TokenType.LPAREN)
        {
            next();
            int n = evalExpr(context);
            assertType(next(), TokenType.RPAREN);
            return n;
        } else if (t.type == TokenType.INT)
        {
            next();
            return (Integer) t.value;
        } else if (t.type == TokenType.IDENTIFIER) return getVar(context);
        // TODO has, take
        throw new ScriptException("Illegal token for factor at " + t.pos);
    }

    private int getVar(Map<String, Integer> context)
    {
        Token t = next();
        assertType(t, TokenType.IDENTIFIER);
        if (get().type == TokenType.PERIOD)
        {
            GALevel level = "_".equals(t.value) ? current_level : get_level.apply(t.value.toString());
            Objects.requireNonNull(level);

            next();
            t = next();
            if (t.type == TokenType.IDENTIFIER)
            {
                assertType(next(), TokenType.PERIOD);
                GAObject object = level == current_level && "_".equals(t.value) ? current_object
                        : level.getObject(t.value.toString());
                Objects.requireNonNull(object);

                t = next();
                assertType(t, TokenType.BUILTIN_VAR);
                if ("visible".equals(t.value))
                    return object.visibleIntProperty().get();
                else if ("state".equals(t.value))
                    return object.getState();
                else
                    throw new ScriptException("Unknown built-in variable at " + t.pos);
            } else if (t.type == TokenType.BUILTIN_VAR && "state".equals(t.value))
                return level.getState();
            else
                throw new ScriptException("Not a valid token after period at " + t.pos);
        } else
            return context.get(t.value.toString());
    }

    private IntConsumer setVar(Map<String, Integer> context)
    {
        Token t = next();
        assertType(t, TokenType.IDENTIFIER);
        if (get().type == TokenType.PERIOD)
        {
            GALevel level = "_".equals(t.value) ? current_level : get_level.apply(t.value.toString());
            Objects.requireNonNull(level);

            next();
            t = next();
            if (t.type == TokenType.IDENTIFIER)
            {
                assertType(next(), TokenType.PERIOD);
                GAObject object = level == current_level && "_".equals(t.value) ? current_object
                        : level.getObject(t.value.toString());
                Objects.requireNonNull(object);

                t = next();
                assertType(t, TokenType.BUILTIN_VAR);
                if ("visible".equals(t.value))
                    return object.visibleIntProperty()::set;
                else if ("state".equals(t.value))
                    return object.stateProperty()::set;
                else
                    throw new ScriptException("Unknown built-in variable at " + t.pos);
            } else if (t.type == TokenType.BUILTIN_VAR && "state".equals(t.value))
                return level.stateProperty()::set;
            else
                throw new ScriptException("Not a valid token after period at " + t.pos);
        } else
        {
            String var = t.value.toString();
            return n -> context.put(var, n);
        }
    }

    private boolean assertType(Token token, TokenType type)
    {
        if (token.type == type)
            return true;
        else
            throw new ScriptException("Token at " + token.pos + " (" + token.type + ") should be " + type);
    }

    public void eval(Script script)
    {
        this.script = script;
        this.pointer = 0;
        eval(new HashMap<>());
    }

    public ScriptEvaluator(Function<String, GALevel> get_level, Consumer<String> warp_to_level, Consumer<String> text)
    {
        this.get_level = get_level;
        this.warp_to_level = warp_to_level;
        this.write_text = text;
    }

    public void setCurrentLevel(GALevel level)
    {
        current_level = level;
    }

    public void setCurrentObject(GAObject object)
    {
        current_object = object;
    }
}
