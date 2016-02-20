/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2003-2004 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowledgement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgement may appear in the software itself,
 *    if and wherever such third-party acknowledgements normally appear.
 *
 * 4. The names "The Jakarta Project", "Commons", and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */
package org.apache.commons.collections.collection;

import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.collections.BoundedCollection;
import org.apache.commons.collections.iterators.UnmodifiableIterator;

/**
 * <code>UnmodifiableBoundedCollection</code> decorates another <code>BoundedCollection</code>
 * to ensure it can't be altered.
 * <p>
 * If a BoundedCollection is first wrapped in some other collection decorator,
 * such as synchronized or predicated, the BoundedCollection nature is lost.
 * The factory on this class will attempt to retrieve the bounded nature by
 * examining the package scope variables.
 *
 * @since Commons Collections 3.0
 * @version $Revision: 1.1 $ $Date: 2004/01/16 18:51:36 $
 * 
 * @author Stephen Colebourne
 */
public final class UnmodifiableBoundedCollection extends AbstractCollectionDecorator implements BoundedCollection {
    
    /**
     * Factory method to create an unmodifiable bounded collection.
     * 
     * @param coll  the <code>BoundedCollection</code> to decorate, must not be null
     * @throws IllegalArgumentException if bag is null
     */
    public static BoundedCollection decorate(BoundedCollection coll) {
        return new UnmodifiableBoundedCollection(coll);
    }
    
    /**
     * Factory method to create an unmodifiable bounded collection.
     * <p>
     * This method is capable of drilling down through up to 1000 other decorators 
     * to find a suitable BoundedCollection.
     * 
     * @param coll  the <code>BoundedCollection</code> to decorate, must not be null
     * @throws IllegalArgumentException if bag is null
     */
    public static BoundedCollection decorateUsing(Collection coll) {
        if (coll == null) {
            throw new IllegalArgumentException("The collection must not be null");
        }
        
        // handle decorators
        for (int i = 0; i < 1000; i++) {  // counter to prevent infinite looping
            if (coll instanceof BoundedCollection) {
                break;  // normal loop exit
            } else if (coll instanceof AbstractCollectionDecorator) {
                coll = ((AbstractCollectionDecorator) coll).collection;
            } else if (coll instanceof SynchronizedCollection) {
                coll = ((SynchronizedCollection) coll).collection;
            } else {
                break;  // normal loop exit
            }
        }
            
        if (coll instanceof BoundedCollection == false) {
            throw new IllegalArgumentException("The collection is not a bounded collection");
        }
        return new UnmodifiableBoundedCollection((BoundedCollection) coll);
    }    
    
    /**
     * Constructor that wraps (not copies).
     * 
     * @param coll  the collection to decorate, must not be null
     * @throws IllegalArgumentException if coll is null
     */
    private UnmodifiableBoundedCollection(BoundedCollection coll) {
        super(coll);
    }

    //-----------------------------------------------------------------------
    public Iterator iterator() {
        return UnmodifiableIterator.decorate(getCollection().iterator());
    }

    public boolean add(Object object) {
        throw new UnsupportedOperationException();
    }

    public boolean addAll(Collection coll) {
        throw new UnsupportedOperationException();
    }

    public void clear() {
        throw new UnsupportedOperationException();
    }

    public boolean remove(Object object) {
        throw new UnsupportedOperationException();
    }

    public boolean removeAll(Collection coll) {
        throw new UnsupportedOperationException();
    }

    public boolean retainAll(Collection coll) {
        throw new UnsupportedOperationException();
    }

    //-----------------------------------------------------------------------    
    public boolean isFull() {
        return ((BoundedCollection) collection).isFull();
    }

    public int maxSize() {
        return ((BoundedCollection) collection).maxSize();
    }
    
}
