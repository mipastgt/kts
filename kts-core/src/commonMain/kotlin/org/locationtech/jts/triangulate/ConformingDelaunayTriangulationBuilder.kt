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
package org.locationtech.jts.triangulate

import org.locationtech.jts.util.TreeMap

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Envelope
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geom.util.LinearComponentExtracter
import org.locationtech.jts.triangulate.quadedge.QuadEdgeSubdivision
import org.locationtech.jts.triangulate.quadedge.Vertex

/**
 * A utility class which creates Conforming Delaunay Triangulations
 * from collections of points and linear constraints, and extract the resulting
 * triangulation edges or triangles as geometries.
 *
 * @author Martin Davis
 */
class ConformingDelaunayTriangulationBuilder {
    private var siteCoords: Collection<Coordinate>? = null
    private var constraintLines: Geometry? = null
    private var tolerance = 0.0
    private var subdiv: QuadEdgeSubdivision? = null

    private val constraintVertexMap: MutableMap<Coordinate, Vertex> = TreeMap()

    /**
     * Sets the sites (point or vertices) which will be triangulated.
     * All vertices of the given geometry will be used as sites.
     * The site vertices do not have to contain the constraint
     * vertices as well; any site vertices which are
     * identical to a constraint vertex will be removed
     * from the site vertex set.
     *
     * @param geom the geometry from which the sites will be extracted.
     */
    fun setSites(geom: Geometry) {
        siteCoords = DelaunayTriangulationBuilder.extractUniqueCoordinates(geom)
    }

    /**
     * Sets the linear constraints to be conformed to.
     * All linear components in the input will be used as constraints.
     * The constraint vertices do not have to be disjoint from
     * the site vertices.
     * The constraints must not contain duplicate segments (up to orientation).
     *
     * @param constraintLines the lines to constraint to
     */
    fun setConstraints(constraintLines: Geometry) {
        this.constraintLines = constraintLines
    }

    /**
     * Sets the snapping tolerance which will be used
     * to improved the robustness of the triangulation computation.
     * A tolerance of 0.0 specifies that no snapping will take place.
     *
     * @param tolerance the tolerance distance to use
     */
    fun setTolerance(tolerance: Double) {
        this.tolerance = tolerance
    }

    private fun create() {
        if (subdiv != null) return

        val siteEnv = DelaunayTriangulationBuilder.envelope(siteCoords!!)

        var segments: MutableList<Segment> = ArrayList()
        val cl = constraintLines
        if (cl != null) {
            siteEnv.expandToInclude(cl.getEnvelopeInternal())
            createVertices(cl)
            segments = createConstraintSegments(cl)
        }
        val sites = createSiteVertices(siteCoords!!)

        val cdt = ConformingDelaunayTriangulator(sites, tolerance)

        cdt.setConstraints(segments, ArrayList(constraintVertexMap.values))

        cdt.formInitialDelaunay()
        cdt.enforceConstraints()
        subdiv = cdt.getSubdivision()
    }

    private fun createSiteVertices(coords: Collection<*>): MutableList<ConstraintVertex> {
        val verts = ArrayList<ConstraintVertex>()
        for (o in coords) {
            val coord = o as Coordinate
            if (constraintVertexMap.containsKey(coord))
                continue
            verts.add(ConstraintVertex(coord))
        }
        return verts
    }

    private fun createVertices(geom: Geometry) {
        val coords = geom.getCoordinates()
        for (i in coords.indices) {
            val v: Vertex = ConstraintVertex(coords[i])
            constraintVertexMap.put(coords[i], v)
        }
    }

    /**
     * Gets the QuadEdgeSubdivision which models the computed triangulation.
     *
     * @return the subdivision containing the triangulation
     */
    fun getSubdivision(): QuadEdgeSubdivision {
        create()
        return subdiv!!
    }

    /**
     * Gets the edges of the computed triangulation as a [org.locationtech.jts.geom.MultiLineString].
     *
     * @param geomFact the geometry factory to use to create the output
     * @return the edges of the triangulation
     */
    fun getEdges(geomFact: GeometryFactory): Geometry {
        create()
        return subdiv!!.getEdges(geomFact)
    }

    /**
     * Gets the faces of the computed triangulation as a [org.locationtech.jts.geom.GeometryCollection]
     * of [org.locationtech.jts.geom.Polygon].
     *
     * @param geomFact the geometry factory to use to create the output
     * @return the faces of the triangulation
     */
    fun getTriangles(geomFact: GeometryFactory): Geometry {
        create()
        return subdiv!!.getTriangles(geomFact)
    }

    companion object {
        private fun createConstraintSegments(geom: Geometry): MutableList<Segment> {
            val lines = LinearComponentExtracter.getLines(geom)
            val constraintSegs = ArrayList<Segment>()
            val i = lines.iterator()
            while (i.hasNext()) {
                val line = i.next() as LineString
                createConstraintSegments(line, constraintSegs)
            }
            return constraintSegs
        }

        private fun createConstraintSegments(line: LineString, constraintSegs: MutableList<Segment>) {
            val coords = line.getCoordinates()
            for (i in 1 until coords.size) {
                constraintSegs.add(Segment(coords[i - 1], coords[i]))
            }
        }
    }
}
