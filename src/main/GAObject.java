package main;

import javafx.beans.property.IntegerProperty;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import util.RangedIntegerProperty;

public class GAObject extends ImageView
{
    final String name;
    final int state_count;
    final Image[] sprites;
    // final Script[] script

    private IntegerProperty state;

    public GAObject(String name, boolean visible, int state_count, Image[] sprites, double x, double y)
    {
        this.name = name;
        this.state_count = state_count;
        this.sprites = sprites;
        setX(x);
        setY(y);
        setVisible(visible);

        state = new RangedIntegerProperty(0, state_count);
        setImage(sprites[0]);
        state.addListener((state, old_s, new_s) -> setImage(sprites[new_s.intValue()]));
    }

    public int getState()
    {
        return state.get();
    }

    public void setState(int new_state)
    {
        state.set(new_state);
    }

    public IntegerProperty stateProperty()
    {
        return state;
    }
}
