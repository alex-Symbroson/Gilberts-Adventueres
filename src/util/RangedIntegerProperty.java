package util;

import javafx.beans.property.SimpleIntegerProperty;

public class RangedIntegerProperty extends SimpleIntegerProperty
{
    private final int range;

    /**
     * The constructor of {@code IntegerProperty}
     * 
     * @param range
     *            the range of possible values
     */
    public RangedIntegerProperty(int range)
    {
        super();
        this.range = range;
    }

    /**
     * The constructor of {@code IntegerProperty}
     *
     * @param initialValue
     *            the initial value of the wrapped value
     * @param range
     *            the range of possible values
     */
    public RangedIntegerProperty(int initialValue, int range)
    {
        super(initialValue);
        this.range = range;
    }

    /**
     * The constructor of {@code IntegerProperty}
     *
     * @param bean
     *            the bean of this {@code IntegerProperty}
     * @param name
     *            the name of this {@code IntegerProperty}
     * @param range
     *            the range of possible values
     */
    public RangedIntegerProperty(Object bean, String name, int range)
    {
        super(bean, name);
        this.range = range;
    }

    /**
     * The constructor of {@code IntegerProperty}
     *
     * @param bean
     *            the bean of this {@code IntegerProperty}
     * @param name
     *            the name of this {@code IntegerProperty}
     * @param initialValue
     *            the initial value of the wrapped value
     * @param range
     *            the range of possible values
     */
    public RangedIntegerProperty(Object bean, String name, int initialValue, int range)
    {
        super(bean, name, initialValue);
        this.range = range;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void set(int newValue)
    {
        if (newValue < 0 || newValue >= range) throw new IllegalArgumentException();

        super.set(newValue);
    }
}
