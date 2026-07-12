/*
 * Copyright (c) 2023 Martin Davis.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.locationtech.jts.operation.relateng

import org.locationtech.jts.geom.Envelope
import org.locationtech.jts.geom.Location

/**
 * The base class for relate topological predicates
 * with a boolean value.
 * Implements tri-state logic for the predicate value,
 * to detect when the final value has been determined.
 *
 * @author Martin Davis
 *
 */
internal abstract class BasicPredicate : TopologyPredicate {

    private var value = UNKNOWN

    override fun isKnown(): Boolean {
        return isKnown(value)
    }

    override fun value(): Boolean {
        return toBoolean(value)
    }

    /**
     * Updates the predicate value to the given state
     * if it is currently unknown.
     *
     * @param val the predicate value to update
     */
    protected fun setValue(`val`: Boolean) {
        //-- don't change already-known value
        if (isKnown())
            return
        value = toValue(`val`)
    }

    protected fun setValue(`val`: Int) {
        //-- don't change already-known value
        if (isKnown())
            return
        value = `val`
    }

    protected fun setValueIf(value: Boolean, cond: Boolean) {
        if (cond)
            setValue(value)
    }

    protected fun require(cond: Boolean) {
        if (!cond)
            setValue(false)
    }

    protected fun requireCovers(a: Envelope, b: Envelope) {
        require(a.covers(b))
    }

    companion object {
        private const val UNKNOWN = -1
        private const val FALSE = 0
        private const val TRUE = 1

        private fun isKnown(value: Int): Boolean {
            return value > UNKNOWN
        }

        private fun toBoolean(value: Int): Boolean {
            return value == TRUE
        }

        private fun toValue(`val`: Boolean): Int {
            return if (`val`) TRUE else FALSE
        }

        /**
         * Tests if two geometries intersect
         * based on an interaction at given locations.
         *
         * @param locA the location on geometry A
         * @param locB the location on geometry B
         * @return true if the geometries intersect
         */
        fun isIntersection(locA: Int, locB: Int): Boolean {
            //-- i.e. some location on both geometries intersects
            return locA != Location.EXTERIOR && locB != Location.EXTERIOR
        }
    }
}
