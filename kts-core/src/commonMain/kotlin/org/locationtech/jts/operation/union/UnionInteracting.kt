/*
 * Copyright (c) 2016 Vivid Solutions.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.locationtech.jts.operation.union

import kotlin.jvm.JvmStatic

import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.util.GeometryCombiner

/**
 * Experimental code to union MultiPolygons
 * with processing limited to the elements which actually interact.
 *
 * Not currently used, since it doesn't seem to offer much of a performance advantage.
 *
 * @author mbdavis
 *
 */
class UnionInteracting(g0: Geometry, g1: Geometry) {

    private val geomFactory: GeometryFactory = g0.getFactory()

    private val g0: Geometry = g0
    private val g1: Geometry = g1

    private val interacts0: BooleanArray = BooleanArray(g0.getNumGeometries())
    private val interacts1: BooleanArray = BooleanArray(g1.getNumGeometries())

    fun union(): Geometry? {
        computeInteracting()

        // check for all interacting or none interacting!

        val int0 = extractElements(g0, interacts0, true)
        val int1 = extractElements(g1, interacts1, true)

        val union = int0.union(int1)

        val disjoint0 = extractElements(g0, interacts0, false)
        val disjoint1 = extractElements(g1, interacts1, false)

        val overallUnion = GeometryCombiner.combine(union, disjoint0, disjoint1)

        return overallUnion
    }

    private fun computeInteracting() {
        for (i in 0 until g0.getNumGeometries()) {
            val elem = g0.getGeometryN(i)
            interacts0[i] = computeInteracting(elem)
        }
    }

    private fun computeInteracting(elem0: Geometry): Boolean {
        var interactsWithAny = false
        for (i in 0 until g1.getNumGeometries()) {
            val elem1 = g1.getGeometryN(i)
            val interacts = elem1.getEnvelopeInternal().intersects(elem0.getEnvelopeInternal())
            if (interacts) interacts1[i] = true
            if (interacts)
                interactsWithAny = true
        }
        return interactsWithAny
    }

    private fun extractElements(
        geom: Geometry,
        interacts: BooleanArray,
        isInteracting: Boolean
    ): Geometry {
        val extractedGeoms = ArrayList<Geometry>()
        for (i in 0 until geom.getNumGeometries()) {
            val elem = geom.getGeometryN(i)
            if (interacts[i] == isInteracting)
                extractedGeoms.add(elem)
        }
        return geomFactory.buildGeometry(extractedGeoms)
    }

    companion object {
        @JvmStatic
        fun union(g0: Geometry, g1: Geometry): Geometry? {
            val uue = UnionInteracting(g0, g1)
            return uue.union()
        }
    }
}
