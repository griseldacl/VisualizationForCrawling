/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001-2004 The Apache Software Foundation.  All rights
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
package org.apache.commons.collections.functors;

import java.io.Serializable;
import java.util.Collection;

import org.apache.commons.collections.Predicate;

/**
 * Predicate implementation that returns true if only one of the predicates return true.
 * 
 * @since Commons Collections 3.0
 * @version $Revision: 1.1 $ $Date: 2004/01/16 18:51:59 $
 *
 * @author Stephen Colebourne
 */
public final class OnePredicate implements Predicate, Serializable {

    /** Serial version UID */
    static final long serialVersionUID = -8125389089924745785L;
    
    /** The array of predicates to call */
    private final Predicate[] iPredicates;
    
    /**
     * Factory to create the predicate.
     * 
     * @param predicates  the predicates to check, cloned, not null
     * @return the <code>any</code> predicate
     * @throws IllegalArgumentException if the predicates array is null
     * @throws IllegalArgumentException if the predicates array has less than 2 elements
     * @throws IllegalArgumentException if any predicate in the array is null
     */
    public static Predicate getInstance(Predicate[] predicates) {
        FunctorUtils.validateMin2(predicates);
        predicates = FunctorUtils.copy(predicates);
        return new OnePredicate(predicates);
    }

    /**
     * Factory to create the predicate.
     * 
     * @param predicates  the predicates to check, cloned, not null
     * @return the <code>one</code> predicate
     * @throws IllegalArgumentException if the predicates array is null
     * @throws IllegalArgumentException if any predicate in the array is null
     * @throws IllegalArgumentException if the predicates array has less than 2 elements
     */
    public static Predicate getInstance(Collection predicates) {
        Predicate[] preds = FunctorUtils.validate(predicates);
        return new OnePredicate(preds);
    }

    /**
     * Constructor that performs no validation.
     * Use <code>getInstance</code> if you want that.
     * 
     * @param predicates  the predicates to check, not cloned, not null
     */
    public OnePredicate(Predicate[] predicates) {
        super();
        iPredicates = predicates;
    }

    /**
     * Return the predicate result.
     */
    public boolean evaluate(Object object) {
        boolean match = false;
        for (int i = 0; i < iPredicates.length; i++) {
            if (iPredicates[i].evaluate(object)) {
                if (match) {
                    return false;
                }
                match = true;
            }
        }
        return match;
    }
    
}
