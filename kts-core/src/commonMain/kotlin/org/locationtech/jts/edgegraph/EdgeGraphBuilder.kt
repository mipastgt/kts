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

package org.locationtech.jts.edgegraph

import kotlin.jvm.JvmStatic

import org.locationtech.jts.geom.CoordinateSequence
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryComponentFilter
import org.locationtech.jts.geom.LineString

/**
 * Builds an edge graph from geometries containing edges.
 *
 * @author mdavis
 */
class EdgeGraphBuilder {

    private val graph = EdgeGraph()

    fun getGraph(): EdgeGraph {
        return graph
    }

    /**
     * Adds the edges of a Geometry to the graph.
     * May be called multiple times.
     * Any dimension of Geometry may be added; the constituent edges are
     * extracted.
     *
     * @param geometry geometry to be added
     */
    fun add(geometry: Geometry) {
        geometry.apply(object : GeometryComponentFilter {
            override fun filter(geom: Geometry) {
                if (geom is LineString) {
                    add(geom)
                }
            }
        })
    }

    /**
     * Adds the edges in a collection of [Geometry]s to the graph.
     * May be called multiple times.
     * Any dimension of Geometry may be added.
     *
     * @param geometries the geometries to be added
     */
    fun add(geometries: Collection<*>) {
        for (o in geometries) {
            val geometry = o as Geometry
            add(geometry)
        }
    }

    private fun add(lineString: LineString) {
        val seq = lineString.getCoordinateSequence()
        for (i in 1 until seq.size()) {
            graph.addEdge(seq.getCoordinate(i - 1), seq.getCoordinate(i))
        }
    }

    companion object {
        @JvmStatic
        fun build(geoms: Collection<*>): EdgeGraph {
            val builder = EdgeGraphBuilder()
            builder.add(geoms)
            return builder.getGraph()
        }
    }
}
