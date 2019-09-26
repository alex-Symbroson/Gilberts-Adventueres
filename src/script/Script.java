package script;

import java.util.ArrayList;

import main.GAObject;

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

        evaluator.test(this);

        return true;
    }

    public void eval(GAObject object)
    {
        evaluator.setCurrentObject(object);
        evaluator.eval(this);
    }
}
