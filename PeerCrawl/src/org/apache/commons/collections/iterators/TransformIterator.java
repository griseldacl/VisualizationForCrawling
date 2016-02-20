/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999-2004 The Apache Software Foundation.  All rights
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
package org.apache.commons.collections.iterators;

import java.util.Iterator;

import org.apache.commons.collections.Transformer;

/** 
 * Decorates an iterator such that each element returned is transformed.
 *
 * @since Commons Collections 1.0
 * @version $Revision: 1.1 $ $Date: 2004/03/27 00:07:13 $
 * 
 * @author James Strachan
 * @author Stephen Colebourne
 */
public class TransformIterator implements Iterator {

    /** The iterator being used */
    private Iterator iterator;
    /** The transformer being used */
    private Transformer transformer;

    //-----------------------------------------------------------------------
    /**
     * Constructs a new <code>TransformIterator</code> that will not function
     * until the {@link #setIterator(Iterator) setIterator} method is 
     * invoked.
     */
    public TransformIterator() {
        super();
    }

    /**
     * Constructs a new <code>TransformIterator</code> that won't transform
     * elements from the given iterator.
     *
     * @param iterator  the iterator to use
     */
    public TransformIterator(Iterator iterator) {
        super();
        this.iterator = iterator;
    }

    /**
     * Constructs a new <code>TransformIterator</code> that will use the
     * given iterator and transformer.  If the given transformer is null,
     * then objects will not be transformed.
     *
     * @param iterator  the iterator to use
     * @param transformer  the transformer to use
     */
    public TransformIterator(Iterator iterator, Transformer transformer) {
        super();
        this.iterator = iterator;
        this.transformer = transformer;
    }

    //-----------------------------------------------------------------------
    public boolean hasNext() {
        return iterator.hasNext();
    }

    /**
     * Gets the next object from the iteration, transforming it using the
     * current transformer. If the transformer is null, no transformation
     * occurs and the object from the iterator is returned directly.
     * 
     * @return the next object
     * @throws java.util.NoSuchElementException if there are no more elements
     */
    public Object next() {
        return transform(iterator.next());
    }

    public void remove() {
        iterator.remove();
    }

    //-----------------------------------------------------------------------
    /** 
     * Gets the iterator this iterator is using.
     * 
     * @return the iterator.
     */
    public Iterator getIterator() {
        return iterator;
    }

    /** 
     * Sets the iterator for this iterator to use.
     * If iteration has started, this effectively resets the iterator.
     * 
     * @param iterator  the iterator to use
     */
    public void setIterator(Iterator iterator) {
        this.iterator = iterator;
    }

    //-----------------------------------------------------------------------
    /** 
     * Gets the transformer this iterator is using.
     * 
     * @return the transformer.
     */
    public Transformer getTransformer() {
        return transformer;
    }

    /** 
     * Sets the transformer this the iterator to use.
     * A null transformer is a no-op transformer.
     * 
     * @param transformer  the transformer to use
     */
    public void setTransformer(Transformer transformer) {
        this.transformer = transformer;
    }

    //-----------------------------------------------------------------------
    /**
     * Transforms the given object using the transformer.
     * If the transformer is null, the original object is returned as-is.
     *
     * @param source  the object to transform
     * @return the transformed object
     */
    protected Object transform(Object source) {
        if (transformer != null) {
            return transformer.transform(source);
        }
        return source;
    }
}
