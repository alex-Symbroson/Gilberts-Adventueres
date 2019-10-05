package script;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import main.GALevel;
import main.GAObject;
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

    // EVAL

    // evaluate script statements until EOF or }
    public int eval(Map<String, Integer> context)
    {
        int n = 0;
        while (get() != Token.EOF && get().type != TokenType.RBRACE)
            n = evalCmp(context);
        return n;
    }

    private static Pattern INSERT = Pattern.compile("(^|[^\\\\])%(.*?)%");

    // evaluates expression at current script position
    // will end with script pointer directly after expression
    public int evalExpr(Map<String, Integer> context)
    {
        List<Character> pre_op = new ArrayList<>();
        Token t;
        while ((t = get()).type == TokenType.OP && "+ - ~ !".indexOf((String) t.value) >= 0)
        {
            next();
            pre_op.add(((String) t.value).charAt(0));
        }
        int n;

        t = next();
        TokenType type = t.type;
        if (t.type == TokenType.LPAREN)
        {
            n = evalCmp(context);
            assertType(next(), TokenType.RPAREN);
        } else if (type == TokenType.LBRACE)
        {
            Map<String, Integer> ncontext = new HashMap<>(context);
            n = eval(ncontext);
            ncontext.forEach((s, i) ->
            {
                if (context.containsKey(s)) context.put(s, i);
            });
            assertType(next(), TokenType.RBRACE);
        } else if (t.type == TokenType.INT)
            n = (Integer) t.value;
        else if (type == TokenType.IDENTIFIER)
        {
            int var_ptr = --pointer;
            skipVar();
            if (get().type == TokenType.ASSIGN)
            {
                next();
                n = evalCmp(context);
                int ptr = pointer;
                pointer = var_ptr;
                setVar(n, context);
                pointer = ptr;
            } else
            {
                int ptr = pointer;
                pointer = var_ptr;
                n = getVar(context);
                pointer = ptr;
            }
        } else if (type == TokenType.BUILTIN_FUNC)
        {
            assertType(next(), TokenType.LPAREN);
            Token arg = next();
            assertType(next(), TokenType.RPAREN);
            if ("text".equals(t.value) && assertType(arg, TokenType.STRING))
            {
                String text = arg.value.toString();
                Matcher m = INSERT.matcher(text);
                text = m.replaceAll(res ->
                {
                    Integer i = context.get(res.group(2));
                    if (i == null) throw new ScriptException("Unknown variable in text at " + arg.pos);
                    return Integer.toString(i);
                });
                write_text.accept(text);
            }
            if ("warp".equals(t.value) && assertType(arg, TokenType.IDENTIFIER))
                warp_to_level.accept(arg.value.toString());
            // TODO give, has, take
            n = -1;
        } else if (type == TokenType.IF)
        {
            assertType(next(), TokenType.LPAREN);
            boolean test = evalCmp(context) != 0;
            assertType(next(), TokenType.RPAREN);
            n = 0;
            if (test)
                n = evalCmp(context);
            else
                skipCmp();
            if (get().type == TokenType.ELSE)
            {
                next();
                if (!test)
                    n = evalCmp(context);
                else
                    skipCmp();
            }
        } else if (type == TokenType.WHILE)
        {
            assertType(next(), TokenType.LPAREN);
            int test_ptr = pointer;
            boolean test = evalCmp(context) != 0;
            assertType(next(), TokenType.RPAREN);

            int count = 0;
            n = 0;
            while (test)
            {
                n = evalCmp(context);
                pointer = test_ptr;
                test = evalCmp(context) != 0;
                next();
                ++count;
                if (count >= 0xFFFFFF) throw new ScriptException("Too many iterations in while() at " + get().pos);
            }
            skipCmp();
        } else
            throw new ScriptException("Not a valid statement at " + t.pos);

        Collections.reverse(pre_op);
        for (char c : pre_op)
        {
            if (c == '-')
                n = -n;
            else if (c == '~')
                n = ~n;
            else if (c == '!') n = n == 0 ? -1 : 0;
            // ignore '+' ("n = n;")
        }
        return n;
    }

    // eval == < <= > >= !=
    public int evalCmp(Map<String, Integer> context)
    {
        int a = evalSum(context);
        Token t = get();
        if (t.type == TokenType.OP)
        {
            int index = "== < <= > >= !=".indexOf(t.value.toString());
            if (index == -1) throw new ScriptException("Not a valid operator at " + t.pos);
            next();
            int b = evalSum(context);
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
    private int evalSum(Map<String, Integer> context)
    {
        int n = evalProd(context);
        Token t;
        while ((t = get()).type == TokenType.OP && "+-&|^".indexOf(t.value.toString()) != -1)
        {
            next();
            int b = evalProd(context);
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
    private int evalProd(Map<String, Integer> context)
    {
        int n = evalExpr(context);
        Token t;
        while ((t = get()).type == TokenType.OP && "*/%".indexOf(t.value.toString()) != -1)
        {
            next();
            int b = evalExpr(context);
            switch (t.value.toString().charAt(0)) {
            case '*':
                n *= b;
                break;
            case '/':
                if (b == 0) throw new ScriptException("Divisor at " + t.pos + " cannot be 0!");
                n /= b;
                break;
            case '%':
                if (b == 0) throw new ScriptException("Divisor at " + t.pos + " cannot be 0!");
                n %= b;
            }
        }
        return n;
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
                    return object.isVisible() ? -1 : 0;
                else if ("state".equals(t.value))
                    return object.getState();
                else
                    throw new ScriptException("Unknown built-in variable at " + t.pos);
            } else if (t.type == TokenType.BUILTIN_VAR && "state".equals(t.value))
                return level.getState();
            else
                throw new ScriptException("Not a valid token after period at " + t.pos);
        } else
        {
            Integer i = context.get(t.value.toString());
            if (i == null) throw new ScriptException("Unknown variable " + t.value.toString() + " at " + t.pos);
            return i;
        }
    }

    private void setVar(int value, Map<String, Integer> context)
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
                if (object == null) throw new ScriptException("Unknown object at " + t.pos);

                t = next();
                assertType(t, TokenType.BUILTIN_VAR);
                if ("visible".equals(t.value))
                    object.setVisible(value != 0);
                else if ("state".equals(t.value))
                    object.setState(value);
                else
                    throw new ScriptException("Unknown built-in variable at " + t.pos);
            } else if (t.type == TokenType.BUILTIN_VAR && "state".equals(t.value))
                level.setState(value);
            else
                throw new ScriptException("Not a valid token after period at " + t.pos);
        } else
        {
            context.put(t.value.toString(), value);
        }
    }

    // SKIP

    // skip over expression at current script position
    // will end with script pointer directly after expression
    public void skipExpr()
    {
        Token t;
        while ((t = get()).type == TokenType.OP && "+ - ~ !".indexOf((String) t.value) >= 0)
            next();

        t = next();
        TokenType type = t.type;
        if (t.type == TokenType.LPAREN)
        {
            skipCmp();
            assertType(next(), TokenType.RPAREN);
        } else if (type == TokenType.LBRACE)
        {
            while (get() != Token.EOF && get().type != TokenType.RBRACE)
                skipCmp();
            assertType(next(), TokenType.RBRACE);
        } else if (type == TokenType.IDENTIFIER)
        {
            --pointer;
            skipVar();
            if (get().type == TokenType.ASSIGN)
            {
                next();
                skipCmp();
            }
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
            skipCmp();
            assertType(next(), TokenType.RPAREN);
            skipCmp();
            if (get().type == TokenType.ELSE)
            {
                next();
                skipCmp();
            }
        } else if (type == TokenType.WHILE)
        {
            assertType(next(), TokenType.LPAREN);
            skipCmp();
            assertType(next(), TokenType.RPAREN);
            skipCmp();
        } else if (t.type != TokenType.INT) throw new ScriptException("Not a valid expression at " + t.pos);
    }

    // skip == < <= > >= !=
    public void skipCmp()
    {
        skipSum();
        Token t = get();
        if (t.type == TokenType.OP)
        {
            int index = "== < <= > >= !=".indexOf(t.value.toString());
            if (index == -1) throw new ScriptException("Not a valid operator at " + t.pos);
            next();
            skipSum();
        }
    }

    // skip + - & | ^
    private void skipSum()
    {
        skipProd();
        Token t;
        while ((t = get()).type == TokenType.OP && "+-&|^".indexOf(t.value.toString()) != -1)
        {
            next();
            skipProd();
        }
    }

    // skip * / %
    private void skipProd()
    {
        skipExpr();
        Token t;
        while ((t = get()).type == TokenType.OP && "*/%".indexOf(t.value.toString()) != -1)
        {
            next();
            skipExpr();
        }
    }

    private void skipVar()
    {
        assertType(next(), TokenType.IDENTIFIER);
        if (get().type == TokenType.PERIOD)
        {
            next();
            Token t = next();
            if (t.type == TokenType.IDENTIFIER)
            {
                assertType(next(), TokenType.PERIOD);
                assertType(next(), TokenType.BUILTIN_VAR);
            } else if (t.type != TokenType.BUILTIN_VAR || !"state".equals(t.value))
                throw new ScriptException("Not a valid token after period at " + t.pos);
        }
    }

    // MISC

    // current token
    private Token get()
    {
        return script.get(pointer);
    }

    // current token, also advances pointer
    private Token next()
    {
        return script.get(pointer++);
    }

    private boolean assertType(Token token, TokenType type)
    {
        if (token.type == type)
            return true;
        else
            throw new ScriptException("Token at " + token.pos + " (" + token.type + ") should be " + type);
    }

    public void test(Script script)
    {
        this.script = script;
        this.pointer = 0;
        while (get() != Token.EOF && get().type != TokenType.RBRACE)
            skipCmp();
    }

    public void reset(Script script)
    {
        this.script = script;
        this.pointer = 0;
    }

    public void eval(Script script)
    {
        reset(script);
        eval(new HashMap<>());
    }

    public ScriptEvaluator(Function<String, GALevel> get_level, Consumer<String> warp_to_level, Consumer<String> text)
    {
        this.get_level = get_level;
        this.warp_to_level = warp_to_level;
        this.write_text = text;
    }

    public Consumer<String> setWriteText(Consumer<String> text)
    {
        Consumer<String> old = write_text;
        write_text = text;
        return old;
    }

    public void setCurrentLevel(GALevel level)
    {
        current_level = level;
    }

    public GAObject getCurrentObject()
    {
        return current_object;
    }

    public void setCurrentObject(GAObject object)
    {
        current_object = object;
    }
}
