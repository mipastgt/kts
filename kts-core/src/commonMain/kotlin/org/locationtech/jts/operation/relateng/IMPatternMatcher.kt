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
import org.locationtech.jts.geom.Envelope
import org.locationtech.jts.geom.IntersectionMatrix
import org.locationtech.jts.geom.Location

/**
 * A predicate that matches a DE-9IM pattern.
 *
 * @author Martin Davis
 *
 */
internal class IMPatternMatcher(private val imPattern: String) : IMPredicate() {

    private val patternMatrix: IntersectionMatrix = IntersectionMatrix(imPattern)

    override fun name(): String {
        return "IMPattern"
    }

    override fun init(envA: Envelope, envB: Envelope) {
        super.init(dimA, dimB)
        //-- if pattern specifies any non-E/non-E interaction, envelopes must not be disjoint
        val requiresInteraction = requireInteraction(patternMatrix)
        val isDisjoint = envA.disjoint(envB)
        setValueIf(false, requiresInteraction && isDisjoint)
    }

    override fun requireInteraction(): Boolean {
        return requireInteraction(patternMatrix)
    }

    override fun isDetermined(): Boolean {
        /*
         * Matrix entries only increase in dimension as topology is computed.
         * The predicate can be short-circuited (as false) if
         * any computed entry is greater than the mask value.
         */
        for (i in 0..2) {
            for (j in 0..2) {
                val patternEntry = patternMatrix.get(i, j)

                if (patternEntry == Dimension.DONTCARE)
                    continue

                val matrixVal = getDimension(i, j)

                //-- mask entry TRUE requires a known matrix entry
                if (patternEntry == Dimension.TRUE) {
                    if (matrixVal < 0)
                        return false
                } else if (matrixVal > patternEntry) {
                    //-- result is known (false) if matrix entry has exceeded mask
                    return true
                }
            }
        }
        return false
    }

    override fun valueIM(): Boolean {
        val `val` = intMatrix.matches(imPattern)
        return `val`
    }

    override fun toString(): String {
        return name() + "(" + imPattern + ")"
    }

    companion object {
        private fun requireInteraction(im: IntersectionMatrix): Boolean {
            val requiresInteraction =
                isInteraction(im.get(Location.INTERIOR, Location.INTERIOR)) ||
                    isInteraction(im.get(Location.INTERIOR, Location.BOUNDARY)) ||
                    isInteraction(im.get(Location.BOUNDARY, Location.INTERIOR)) ||
                    isInteraction(im.get(Location.BOUNDARY, Location.BOUNDARY))
            return requiresInteraction
        }

        private fun isInteraction(imDim: Int): Boolean {
            return imDim == Dimension.TRUE || imDim >= Dimension.P
        }
    }
}
