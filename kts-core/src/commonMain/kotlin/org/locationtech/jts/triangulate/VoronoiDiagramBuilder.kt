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

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.CoordinateArrays
import org.locationtech.jts.geom.Envelope
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.triangulate.quadedge.QuadEdgeSubdivision

/**
 * A utility class which creates Voronoi Diagrams
 * from collections of points.
 * The diagram is returned as a [org.locationtech.jts.geom.GeometryCollection] of [org.locationtech.jts.geom.Polygon]s
 * representing the faces of the Voronoi diagram.
 * The faces are clipped to the larger of:
 *
 *  * an envelope supplied by [setClipEnvelope]
 *  * an envelope determined by the input sites
 *
 * The `userData` attribute of each face `Polygon` is set to
 * the `Coordinate`  of the corresponding input site.
 * This allows using a `Map` to link faces to data associated with sites.
 *
 * @author Martin Davis
 */
open class VoronoiDiagramBuilder {

    private var siteCoords: Collection<Coordinate>? = null
    private var tolerance = 0.0
    private var subdiv: QuadEdgeSubdivision? = null
    private var clipEnv: Envelope? = null
    private var diagramEnv: Envelope? = null

    /**
     * Sets the sites (point or vertices) which will be diagrammed.
     * All vertices of the given geometry will be used as sites.
     *
     * @param geom the geometry from which the sites will be extracted.
     */
    fun setSites(geom: Geometry) {
        // remove any duplicate points (they will cause the triangulation to fail)
        siteCoords = DelaunayTriangulationBuilder.extractUniqueCoordinates(geom)
    }

    /**
     * Sets the sites (point or vertices) which will be diagrammed
     * from a collection of [Coordinate]s.
     *
     * @param coords a collection of Coordinates.
     */
    fun setSites(coords: Collection<Coordinate>) {
        // remove any duplicate points (they will cause the triangulation to fail)
        siteCoords = DelaunayTriangulationBuilder.unique(CoordinateArrays.toCoordinateArray(coords))
    }

    /**
     * Sets the envelope to clip the diagram to.
     * The diagram will be clipped to the larger
     * of this envelope or an envelope surrounding the sites.
     *
     * @param clipEnv the clip envelope.
     */
    fun setClipEnvelope(clipEnv: Envelope) {
        this.clipEnv = clipEnv
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

        var env = clipEnv
        if (env == null) {
            /*
             * If no user-provided clip envelope,
             * use one which encloses all the sites,
             * with a 50% buffer around the edges.
             */
            env = DelaunayTriangulationBuilder.envelope(siteCoords!!)
            // add a 50% buffer around the sites envelope
            val expandBy = env.getDiameter()
            env.expandBy(expandBy)
        }
        diagramEnv = env

        val vertices = DelaunayTriangulationBuilder.toVertices(siteCoords!!)
        val sd = QuadEdgeSubdivision(env, tolerance)
        subdiv = sd
        val triangulator = IncrementalDelaunayTriangulator(sd)
        /*
         * Avoid creating very narrow triangles along triangulation boundary.
         * These otherwise can cause malformed Voronoi cells.
         */
        triangulator.forceConvex(false)
        triangulator.insertSites(vertices)
    }

    /**
     * Gets the [QuadEdgeSubdivision] which models the computed diagram.
     *
     * @return the subdivision containing the triangulation
     */
    fun getSubdivision(): QuadEdgeSubdivision {
        create()
        return subdiv!!
    }

    /**
     * Gets the faces of the computed diagram as a [org.locationtech.jts.geom.GeometryCollection]
     * of [org.locationtech.jts.geom.Polygon]s, clipped as specified.
     *
     * The `userData` attribute of each face `Polygon` is set to
     * the `Coordinate`  of the corresponding input site.
     * This allows using a `Map` to link faces to data associated with sites.
     *
     * @param geomFact the geometry factory to use to create the output
     * @return a `GeometryCollection` containing the face `Polygon`s of the diagram
     */
    fun getDiagram(geomFact: GeometryFactory): Geometry {
        create()
        val polys = subdiv!!.getVoronoiDiagram(geomFact)

        //-- clip polys to diagramEnv
        return clipGeometryCollection(polys, diagramEnv!!)
    }

    companion object {
        private fun clipGeometryCollection(geom: Geometry, clipEnv: Envelope): Geometry {
            val clipPoly = geom.getFactory().toGeometry(clipEnv)
            val clipped = ArrayList<Geometry>()
            for (i in 0 until geom.getNumGeometries()) {
                val g = geom.getGeometryN(i)
                var result: Geometry? = null
                // don't clip unless necessary
                if (clipEnv.contains(g.getEnvelopeInternal()))
                    result = g
                else if (clipEnv.intersects(g.getEnvelopeInternal())) {
                    result = clipPoly.intersection(g)
                    // keep vertex key info
                    result.setUserData(g.getUserData())
                }

                if (result != null && !result.isEmpty()) {
                    clipped.add(result)
                }
            }
            return geom.getFactory().createGeometryCollection(GeometryFactory.toGeometryArray(clipped))
        }
    }
}
