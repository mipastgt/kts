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

import org.locationtech.jts.geom.Dimension
import org.locationtech.jts.geom.IntersectionMatrix
import org.locationtech.jts.geom.Location

/**
 * A base class for predicates which are
 * determined using entries in a [IntersectionMatrix].
 *
 * @author Martin Davis
 *
 */
internal abstract class IMPredicate : BasicPredicate() {

    protected var dimA = 0
    protected var dimB = 0
    protected var intMatrix: IntersectionMatrix = IntersectionMatrix()

    init {
        //-- E/E is always dim = 2
        intMatrix.set(Location.EXTERIOR, Location.EXTERIOR, Dimension.A)
    }

    override fun init(dimA: Int, dimB: Int) {
        this.dimA = dimA
        this.dimB = dimB
    }

    override fun updateDimension(locA: Int, locB: Int, dimension: Int) {
        //-- only record an increased dimension value
        if (isDimChanged(locA, locB, dimension)) {
            intMatrix.set(locA, locB, dimension)
            //-- set value if predicate value can be known
            if (isDetermined()) {
                setValue(valueIM())
            }
        }
    }

    fun isDimChanged(locA: Int, locB: Int, dimension: Int): Boolean {
        return dimension > intMatrix.get(locA, locB)
    }

    /**
     * Tests whether predicate evaluation can be short-circuited
     * due to the current state of the matrix providing
     * enough information to determine the predicate value.
     *
     * @return true if the predicate value is determined
     */
    protected abstract fun isDetermined(): Boolean

    /**
     * Tests whether the exterior of the specified input geometry
     * is intersected by any part of the other input.
     *
     * @param isA the input geometry
     * @return true if the input geometry exterior is intersected
     */
    protected fun intersectsExteriorOf(isA: Boolean): Boolean {
        return if (isA) {
            isIntersects(Location.EXTERIOR, Location.INTERIOR) ||
                isIntersects(Location.EXTERIOR, Location.BOUNDARY)
        } else {
            isIntersects(Location.INTERIOR, Location.EXTERIOR) ||
                isIntersects(Location.BOUNDARY, Location.EXTERIOR)
        }
    }

    protected fun isIntersects(locA: Int, locB: Int): Boolean {
        return intMatrix.get(locA, locB) >= Dimension.P
    }

    fun isKnown(locA: Int, locB: Int): Boolean {
        return intMatrix.get(locA, locB) != DIM_UNKNOWN
    }

    fun isDimension(locA: Int, locB: Int, dimension: Int): Boolean {
        return intMatrix.get(locA, locB) == dimension
    }

    fun getDimension(locA: Int, locB: Int): Int {
        return intMatrix.get(locA, locB)
    }

    /**
     * Sets the final value based on the state of the IM.
     */
    override fun finish() {
        setValue(valueIM())
    }

    /**
     * Gets the value of the predicate according to the current
     * intersection matrix state.
     *
     * @return the current predicate value
     */
    protected abstract fun valueIM(): Boolean

    override fun toString(): String {
        return name() + ": " + intMatrix
    }

    companion object {
        fun isDimsCompatibleWithCovers(dim0: Int, dim1: Int): Boolean {
            //- allow Points coveredBy zero-length Lines
            if (dim0 == Dimension.P && dim1 == Dimension.L)
                return true
            return dim0 >= dim1
        }

        private const val DIM_UNKNOWN = Dimension.DONTCARE
    }
}
