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
 *  $Id: IntObj.java,v 1.6 2005/10/03 00:18:22 gregork Exp $
 */
package phex.common;

/**
 * Class that is similar to <code>Integer</code> but is mutable.
 */
public class IntObj extends Number implements Comparable
{
    public int value;

    public IntObj()
    {
    }

    public IntObj( int v )
    {
        value = v;
    }

    public void setValue( int v )
    {
        this.value = v;
    }

    public int getValue()
    {
        return value;
    }

    public boolean equals( Object o )
    {
        if ( this == o )
        {
            return true;
        }
        else if ( ! ( o instanceof IntObj ) )
        {
            return false;
        }
        return value == ((IntObj)o).value;
    }

    public int hashCode()
    {
        return value;
    }

    /**
     * Increases the integer by inc and returns the new value.
     */
    public int inc( int inc )
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
        return getValue();
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
        return getValue();
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
        return getValue();
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
    
    /**
     * Compares two <code>Integer</code> objects numerically.
     *
     * @param   anotherInteger   the <code>Integer</code> to be compared.
     * @return  the value <code>0</code> if this <code>Integer</code> is
     *      equal to the argument <code>Integer</code>; a value less than
     *      <code>0</code> if this <code>Integer</code> is numerically less
     *      than the argument <code>Integer</code>; and a value greater 
     *      than <code>0</code> if this <code>Integer</code> is numerically
     *       greater than the argument <code>Integer</code> (signed
     *       comparison).
     */
    public int compareTo(IntObj anotherIntObj)
    {
        int thisVal = this.value;
        int anotherVal = anotherIntObj.value;
        return (thisVal<anotherVal ? -1 : (thisVal==anotherVal ? 0 : 1));
    }

    /**
     * Compares this <code>Integer</code> object to another object.
     * If the object is an <code>Integer</code>, this function behaves
     * like <code>compareTo(Integer)</code>.  Otherwise, it throws a
     * <code>ClassCastException</code> (as <code>Integer</code>
     * objects are only comparable to other <code>Integer</code>
     * objects).
     *
     * @param   o the <code>Object</code> to be compared.
     * @return  the value <code>0</code> if the argument is a 
     *      <code>Integer</code> numerically equal to this 
     *      <code>Integer</code>; a value less than <code>0</code> 
     *      if the argument is a <code>Integer</code> numerically 
     *      greater than this <code>Integer</code>; and a value 
     *      greater than <code>0</code> if the argument is a 
     *      <code>Integer</code> numerically less than this 
     *      <code>Integer</code>.
     * @exception <code>ClassCastException</code> if the argument is not an
     *        <code>Integer</code>.
     * @see     java.lang.Comparable
     */
    public int compareTo(Object o)
    {
        return compareTo((IntObj)o);
    }
}