/*
 *  PHEX - The pure-java Gnutella-servent.
 *  Copyright (C) 2001 - 2005 Phex Development Group
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * 
 *  --- CVS Information ---
 *  $Id: DoubleObj.java,v 1.5 2005/10/03 00:18:22 gregork Exp $
 */
package phex.common;

/**
 * Class that is similar to <code>Double</code> but is mutable.
 */
public class DoubleObj extends Number
{
    public double value;

    public DoubleObj()
    {
    }

    public DoubleObj( double v )
    {
        value = v;
    }

    public void setValue( double v )
    {
        this.value = v;
    }

    public double getValue()
    {
        return value;
    }

    public boolean equals( Object o )
    {
        if ( this == o )
        {
            return true;
        }
        else if ( ! ( o instanceof DoubleObj ) )
        {
            return false;
        }
        return value == ((DoubleObj)o).value;
    }

    public int hashCode()
    {
        return (int)value;
    }

    /**
     * Increases the double by inc and returns the new value.
     */
    public double inc( double inc )
    {
        value += inc;
        return value;
    }

    public String toString()
    {
        return String.valueOf( value );
    }

    /**
     * Returns the value of the specified number as an <code>int</code>.
     * This may involve rounding.
     *
     * @return  the numeric value represented by this object after conversion
     *          to type <code>int</code>.
     */
    public int intValue()
    {
        return (int)getValue();
    }

    /**
     * Returns the value of the specified number as a <code>long</code>.
     * This may involve rounding.
     *
     * @return  the numeric value represented by this object after conversion
     *          to type <code>long</code>.
     */
    public long longValue()
    {
        return (long)getValue();
    }

    /**
     * Returns the value of the specified number as a <code>float</code>.
     * This may involve rounding.
     *
     * @return  the numeric value represented by this object after conversion
     *          to type <code>float</code>.
     */
    public float floatValue()
    {
        return (float)getValue();
    }

    /**
     * Returns the value of the specified number as a <code>double</code>.
     * This may involve rounding.
     *
     * @return  the numeric value represented by this object after conversion
     *          to type <code>double</code>.
     */
    public double doubleValue()
    {
        return getValue();
    }

}
