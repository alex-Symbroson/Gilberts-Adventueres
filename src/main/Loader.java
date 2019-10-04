package main;

import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import org.json.JSONObject;

import javafx.scene.image.Image;
import script.Script;
import script.ScriptEvaluator;
import script.ScriptException;
import script.ScriptLexer;
import script.Token;
import script.Token.TokenType;

public class Loader
{
    private final URL res_levels, res_img;
    private final ScriptLexer lexer;
    private final ScriptEvaluator eval;

    public Loader(URL res_levels, URL res_img, ScriptEvaluator eval)
    {
        this.res_levels = res_levels;
        this.res_img = res_img;
        lexer = new ScriptLexer(null);
        this.eval = eval;
    }

    public Script loadScript(String script_string)
    {
        lexer.yyreset(new StringReader(script_string));

        Script script = new Script(eval);
        try
        {
            Token t;
            do
            {
                script.add(t = lexer.yylex());
            } while (t.getType() != TokenType.EOF);
        } catch (IOException e)
        {
            e.printStackTrace();
            script.add(Token.EOF);
        }

        if (!script.testValid())
        {
            throw new ScriptException("Script lexed from \"" + script_string + "\" not valid");
        }

        return script;
    }

    public JSONObject loadLevelJson(String file)
    {
        URL level_file = null;
        try
        {
            level_file = new URL(res_levels, file + ".json");
        } catch (MalformedURLException e)
        {
            e.printStackTrace();
        }

        JSONObject json;
        try
        {
            String src = new String(level_file.openStream().readAllBytes());
            json = new JSONObject(src);
        } catch (IOException e)
        {
            e.printStackTrace();
            json = new JSONObject("{ \"error\": \"java.io.IOException\"}");
        }
        return json;
    }

    public Image loadImage(String file) throws IOException
    {
        Image img = new Image((new URL(res_img, file + ".png")).openStream());

        if (img.errorProperty().get()) throw new IOException(img.getException());

        return img;
    }

    @Deprecated
    public Image loadBackground(Path path, double width, double height) throws IOException
    {
        Image img = new Image(Files.newInputStream(path), width, height, true, false);

        if (img.errorProperty().get()) throw new IOException(img.getException());

        return img;
    }
}
