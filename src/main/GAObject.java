package main;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import script.Script;
import util.RangedIntegerProperty;

public class GAObject extends ImageView
{
    final String name;
    final int state_count;
    final Image[] sprites;
    final Script[] scripts;
    final boolean start_visible;

    private final IntegerProperty state;
    private final IntegerProperty visible_int;

    public GAObject(String name, boolean visible, int state_count, Image[] sprites, Script[] scripts, double x,
            double y)
    {
        this.name = name;
        this.state_count = state_count;
        this.sprites = sprites;
        this.scripts = scripts;
        setX(x);
        setY(y);
        setFitWidth(sprites[0].getWidth());
        setFitHeight(sprites[0].getHeight());

        start_visible = visible;

        visible_int = new SimpleIntegerProperty(visible ? 1 : 0);
        visibleProperty().bind(visible_int.isNotEqualTo(0));

        state = new RangedIntegerProperty(0, state_count);
        setImage(sprites[0]);
        state.addListener((state, old_s, new_s) -> setImage(sprites[new_s.intValue()]));

        setOnMouseClicked(ev -> scripts[getState()].eval(this));
    }

    public int getState()
    {
        return state.get();
    }

    public void setState(int new_state)
    {
        state.set(new_state);
    }

    public IntegerProperty visibleIntProperty()
    {
        return visible_int;
    }

    public IntegerProperty stateProperty()
    {
        return state;
    }
}
