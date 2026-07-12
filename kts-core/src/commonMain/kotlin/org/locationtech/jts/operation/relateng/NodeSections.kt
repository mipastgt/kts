/*
 * Copyright (c) 2024 Martin Davis.
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

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Geometry

internal class NodeSections(private val nodePt: Coordinate) {

    private val sections: MutableList<NodeSection> = ArrayList()

    fun getCoordinate(): Coordinate {
        return nodePt
    }

    fun addNodeSection(e: NodeSection) {
        sections.add(e)
    }

    fun hasInteractionAB(): Boolean {
        var isA = false
        var isB = false
        for (ns in sections) {
            if (ns.isA())
                isA = true
            else
                isB = true
            if (isA && isB)
                return true
        }
        return false
    }

    fun getPolygonal(isA: Boolean): Geometry? {
        for (ns in sections) {
            if (ns.isA() == isA) {
                val poly = ns.getPolygonal()
                if (poly != null)
                    return poly
            }
        }
        return null
    }

    fun createNode(): RelateNode {
        prepareSections()

        val node = RelateNode(nodePt)
        var i = 0
        while (i < sections.size) {
            val ns = sections[i]
            //-- if there multiple polygon sections incident at node convert them to maximal-ring structure
            if (ns.isArea() && hasMultiplePolygonSections(sections, i)) {
                val polySections = collectPolygonSections(sections, i)
                val nsConvert = PolygonNodeConverter.convert(polySections)
                node.addEdges(nsConvert)
                i += polySections.size
            } else {
                //-- the most common case is a line or a single polygon ring section
                node.addEdges(ns)
                i += 1
            }
        }
        return node
    }

    /**
     * Sorts the sections so that:
     *
     *  * lines are before areas
     *  * edges from the same polygon are contiguous
     *
     */
    private fun prepareSections() {
        sections.sort()
        //TODO: remove duplicate sections
    }

    companion object {
        private fun hasMultiplePolygonSections(sections: List<NodeSection>, i: Int): Boolean {
            //-- if last section can only be one
            if (i >= sections.size - 1)
                return false
            //-- check if there are at least two sections for same polygon
            val ns = sections[i]
            val nsNext = sections[i + 1]
            return ns.isSamePolygon(nsNext)
        }

        private fun collectPolygonSections(sections: List<NodeSection>, i: Int): MutableList<NodeSection> {
            var index = i
            val polySections = ArrayList<NodeSection>()
            //-- note ids are only unique to a geometry
            val polySection = sections[index]
            while (index < sections.size &&
                polySection.isSamePolygon(sections[index])
            ) {
                polySections.add(sections[index])
                index++
            }
            return polySections
        }
    }
}
