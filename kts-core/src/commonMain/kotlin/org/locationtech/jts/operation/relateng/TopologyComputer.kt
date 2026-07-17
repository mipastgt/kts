/*
 * Copyright (c) 2022 Martin Davis.
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

import org.locationtech.jts.algorithm.PolygonNodeTopology
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Dimension
import org.locationtech.jts.geom.Location
import org.locationtech.jts.geom.Position

internal class TopologyComputer(
    private val predicate: TopologyPredicate,
    private val geomA: RelateGeometry,
    private val geomB: RelateGeometry
) {

    private val nodeMap: MutableMap<Coordinate, NodeSections> = HashMap()

    init {
        initExteriorDims()
    }

    /**
     * Determine a priori partial EXTERIOR topology based on dimensions.
     */
    private fun initExteriorDims() {
        val dimRealA = geomA.getDimensionReal()
        val dimRealB = geomB.getDimensionReal()

        /*
         * For P/L case, P exterior intersects L interior
         */
        if (dimRealA == Dimension.P && dimRealB == Dimension.L) {
            updateDim(Location.EXTERIOR, Location.INTERIOR, Dimension.L)
        } else if (dimRealA == Dimension.L && dimRealB == Dimension.P) {
            updateDim(Location.INTERIOR, Location.EXTERIOR, Dimension.L)
        } else if (dimRealA == Dimension.P && dimRealB == Dimension.A) {
            /*
             * For P/A case, the Area Int and Bdy intersect the Point exterior.
             */
            updateDim(Location.EXTERIOR, Location.INTERIOR, Dimension.A)
            updateDim(Location.EXTERIOR, Location.BOUNDARY, Dimension.L)
        } else if (dimRealA == Dimension.A && dimRealB == Dimension.P) {
            updateDim(Location.INTERIOR, Location.EXTERIOR, Dimension.A)
            updateDim(Location.BOUNDARY, Location.EXTERIOR, Dimension.L)
        } else if (dimRealA == Dimension.L && dimRealB == Dimension.A) {
            updateDim(Location.EXTERIOR, Location.INTERIOR, Dimension.A)
        } else if (dimRealA == Dimension.A && dimRealB == Dimension.L) {
            updateDim(Location.INTERIOR, Location.EXTERIOR, Dimension.A)
        } else if (dimRealA == Dimension.FALSE || dimRealB == Dimension.FALSE) {
            //-- cases where one geom is EMPTY
            if (dimRealA != Dimension.FALSE) {
                initExteriorEmpty(RelateGeometry.GEOM_A)
            }
            if (dimRealB != Dimension.FALSE) {
                initExteriorEmpty(RelateGeometry.GEOM_B)
            }
        }
    }

    private fun initExteriorEmpty(geomNonEmpty: Boolean) {
        val dimNonEmpty = getDimension(geomNonEmpty)
        when (dimNonEmpty) {
            Dimension.P ->
                updateDim(geomNonEmpty, Location.INTERIOR, Location.EXTERIOR, Dimension.P)
            Dimension.L -> {
                if (getGeometry(geomNonEmpty).hasBoundary()) {
                    updateDim(geomNonEmpty, Location.BOUNDARY, Location.EXTERIOR, Dimension.P)
                }
                updateDim(geomNonEmpty, Location.INTERIOR, Location.EXTERIOR, Dimension.L)
            }
            Dimension.A -> {
                updateDim(geomNonEmpty, Location.BOUNDARY, Location.EXTERIOR, Dimension.L)
                updateDim(geomNonEmpty, Location.INTERIOR, Location.EXTERIOR, Dimension.A)
            }
        }
    }

    private fun getGeometry(isA: Boolean): RelateGeometry {
        return if (isA) geomA else geomB
    }

    fun getDimension(isA: Boolean): Int {
        return getGeometry(isA).getDimension()
    }

    fun isAreaArea(): Boolean {
        return getDimension(RelateGeometry.GEOM_A) == Dimension.A &&
            getDimension(RelateGeometry.GEOM_B) == Dimension.A
    }

    /**
     * Indicates whether the input geometries require self-noding
     * for correct evaluation of specific spatial predicates.
     *
     * @return true if self-noding is required
     */
    fun isSelfNodingRequired(): Boolean {
        if (predicate.requireSelfNoding()) {
            if (geomA.isSelfNodingRequired() || geomB.isSelfNodingRequired())
                return true
        }
        return false
    }

    fun isExteriorCheckRequired(isA: Boolean): Boolean {
        return predicate.requireExteriorCheck(isA)
    }

    private fun updateDim(locA: Int, locB: Int, dimension: Int) {
        predicate.updateDimension(locA, locB, dimension)
    }

    private fun updateDim(isAB: Boolean, loc1: Int, loc2: Int, dimension: Int) {
        if (isAB) {
            updateDim(loc1, loc2, dimension)
        } else {
            // is ordered BA
            updateDim(loc2, loc1, dimension)
        }
    }

    fun isResultKnown(): Boolean {
        return predicate.isKnown()
    }

    fun getResult(): Boolean {
        return predicate.value()
    }

    /**
     * Finalize the evaluation.
     */
    fun finish() {
        predicate.finish()
    }

    private fun getNodeSections(nodePt: Coordinate): NodeSections {
        var node = nodeMap[nodePt]
        if (node == null) {
            node = NodeSections(nodePt)
            nodeMap[nodePt] = node
        }
        return node
    }

    fun addIntersection(a: NodeSection, b: NodeSection) {
        if (!a.isSameGeometry(b)) {
            updateIntersectionAB(a, b)
        }
        //-- add edges to node to allow full topology evaluation later
        addNodeSections(a, b)
    }

    /**
     * Update topology for an intersection between A and B.
     *
     * @param a the section for geometry A
     * @param b the section for geometry B
     */
    private fun updateIntersectionAB(a: NodeSection, b: NodeSection) {
        if (NodeSection.isAreaArea(a, b)) {
            updateAreaAreaCross(a, b)
        }
        updateNodeLocation(a, b)
    }

    /**
     * Updates topology for an AB Area-Area crossing node.
     *
     * @param a the section for geometry A
     * @param b the section for geometry B
     */
    private fun updateAreaAreaCross(a: NodeSection, b: NodeSection) {
        val isProper = NodeSection.isProper(a, b)
        if (isProper || PolygonNodeTopology.isCrossing(
                a.nodePt(),
                a.getVertex(0)!!, a.getVertex(1)!!,
                b.getVertex(0)!!, b.getVertex(1)!!
            )
        ) {
            updateDim(Location.INTERIOR, Location.INTERIOR, Dimension.A)
        }
    }

    /**
     * Updates topology for a node at an AB edge intersection.
     *
     * @param a the section for geometry A
     * @param b the section for geometry B
     */
    private fun updateNodeLocation(a: NodeSection, b: NodeSection) {
        val pt = a.nodePt()
        val locA = geomA.locateNode(pt, a.getPolygonal())
        val locB = geomB.locateNode(pt, b.getPolygonal())
        updateDim(locA, locB, Dimension.P)
    }

    private fun addNodeSections(ns0: NodeSection, ns1: NodeSection) {
        val sections = getNodeSections(ns0.nodePt())
        sections.addNodeSection(ns0)
        sections.addNodeSection(ns1)
    }

    fun addPointOnPointInterior(pt: Coordinate?) {
        updateDim(Location.INTERIOR, Location.INTERIOR, Dimension.P)
    }

    fun addPointOnPointExterior(isGeomA: Boolean, pt: Coordinate?) {
        updateDim(isGeomA, Location.INTERIOR, Location.EXTERIOR, Dimension.P)
    }

    fun addPointOnGeometry(isA: Boolean, locTarget: Int, dimTarget: Int, pt: Coordinate?) {
        updateDim(isA, Location.INTERIOR, locTarget, Dimension.P)
        when (dimTarget) {
            Dimension.P -> return
            Dimension.L -> {
                /*
                 * Because zero-length lines are handled,
                 * a point lying in the exterior of the line target
                 * may imply either P or L for the Exterior interaction
                 */
                return
            }
            Dimension.A -> {
                /*
                 * If a point intersects an area target, then the area interior and boundary
                 * must extend beyond the point and thus interact with its exterior.
                 */
                updateDim(isA, Location.EXTERIOR, Location.INTERIOR, Dimension.A)
                updateDim(isA, Location.EXTERIOR, Location.BOUNDARY, Dimension.L)
                return
            }
        }
        throw IllegalStateException("Unknown target dimension: $dimTarget")
    }

    /**
     * Add topology for a line end.
     *
     * @param isLineA the input containing the line end
     * @param locLineEnd the location of the line end (Interior or Boundary)
     * @param locTarget the location on the target geometry
     * @param dimTarget the dimension of the interacting target geometry element
     * @param pt the line end coordinate
     */
    fun addLineEndOnGeometry(isLineA: Boolean, locLineEnd: Int, locTarget: Int, dimTarget: Int, pt: Coordinate?) {
        //-- record topology at line end point
        updateDim(isLineA, locLineEnd, locTarget, Dimension.P)

        //-- Line and Area targets may have additional topology
        when (dimTarget) {
            Dimension.P -> return
            Dimension.L -> {
                addLineEndOnLine(isLineA, locLineEnd, locTarget, pt)
                return
            }
            Dimension.A -> {
                addLineEndOnArea(isLineA, locLineEnd, locTarget, pt)
                return
            }
        }
        throw IllegalStateException("Unknown target dimension: $dimTarget")
    }

    private fun addLineEndOnLine(isLineA: Boolean, locLineEnd: Int, locLine: Int, pt: Coordinate?) {
        /*
         * When a line end is in the EXTERIOR of a Line,
         * some length of the source Line INTERIOR
         * is also in the target Line EXTERIOR.
         */
        if (locLine == Location.EXTERIOR) {
            updateDim(isLineA, Location.INTERIOR, Location.EXTERIOR, Dimension.L)
        }
    }

    private fun addLineEndOnArea(isLineA: Boolean, locLineEnd: Int, locArea: Int, pt: Coordinate?) {
        if (locArea != Location.BOUNDARY) {
            /*
             * When a line end is in an Area INTERIOR or EXTERIOR
             * some length of the source Line Interior
             * AND the Exterior of the line
             * is also in that location of the target.
             */
            updateDim(isLineA, Location.INTERIOR, locArea, Dimension.L)
            updateDim(isLineA, Location.EXTERIOR, locArea, Dimension.A)
        }
    }

    /**
     * Adds topology for an area vertex interaction with a target geometry element.
     *
     * @param isAreaA the input that is the area
     * @param locArea the location on the area
     * @param locTarget the location on the target geometry element
     * @param dimTarget the dimension of the target geometry element
     * @param pt the point of interaction
     */
    fun addAreaVertex(isAreaA: Boolean, locArea: Int, locTarget: Int, dimTarget: Int, pt: Coordinate?) {
        if (locTarget == Location.EXTERIOR) {
            updateDim(isAreaA, Location.INTERIOR, Location.EXTERIOR, Dimension.A)
            /*
             * If area vertex is on Boundary further topology can be deduced
             * from the neighbourhood around the boundary vertex.
             */
            if (locArea == Location.BOUNDARY) {
                updateDim(isAreaA, Location.BOUNDARY, Location.EXTERIOR, Dimension.L)
                updateDim(isAreaA, Location.EXTERIOR, Location.EXTERIOR, Dimension.A)
            }
            return
        }
        when (dimTarget) {
            Dimension.P -> {
                addAreaVertexOnPoint(isAreaA, locArea, pt)
                return
            }
            Dimension.L -> {
                addAreaVertexOnLine(isAreaA, locArea, locTarget, pt)
                return
            }
            Dimension.A -> {
                addAreaVertexOnArea(isAreaA, locArea, locTarget, pt)
                return
            }
        }
        throw IllegalStateException("Unknown target dimension: $dimTarget")
    }

    /**
     * Updates topology for an area vertex (in Interior or on Boundary)
     * intersecting a point.
     *
     * @param isAreaA whether the area is the A input
     * @param locArea the location of the vertex in the area
     * @param pt the point at which topology is being updated
     */
    private fun addAreaVertexOnPoint(isAreaA: Boolean, locArea: Int, pt: Coordinate?) {
        //-- Assert: locArea != EXTERIOR
        //-- Assert: locTarget == INTERIOR
        /*
         * The vertex location intersects the Point.
         */
        updateDim(isAreaA, locArea, Location.INTERIOR, Dimension.P)
        /*
         * The area interior intersects the point's exterior neighbourhood.
         */
        updateDim(isAreaA, Location.INTERIOR, Location.EXTERIOR, Dimension.A)
        /*
         * If the area vertex is on the boundary,
         * the area boundary and exterior intersect the point's exterior neighbourhood
         */
        if (locArea == Location.BOUNDARY) {
            updateDim(isAreaA, Location.BOUNDARY, Location.EXTERIOR, Dimension.L)
            updateDim(isAreaA, Location.EXTERIOR, Location.EXTERIOR, Dimension.A)
        }
    }

    private fun addAreaVertexOnLine(isAreaA: Boolean, locArea: Int, locTarget: Int, pt: Coordinate?) {
        //-- Assert: locArea != EXTERIOR
        /*
         * If an area vertex intersects a line, all we know is the
         * intersection at that point.
         */
        updateDim(isAreaA, locArea, locTarget, Dimension.P)
        if (locArea == Location.INTERIOR) {
            /*
             * The area interior intersects the line's exterior neighbourhood.
             */
            updateDim(isAreaA, Location.INTERIOR, Location.EXTERIOR, Dimension.A)
        }
    }

    fun addAreaVertexOnArea(isAreaA: Boolean, locArea: Int, locTarget: Int, pt: Coordinate?) {
        if (locTarget == Location.BOUNDARY) {
            if (locArea == Location.BOUNDARY) {
                //-- B/B topology is fully computed later by node analysis
                updateDim(isAreaA, Location.BOUNDARY, Location.BOUNDARY, Dimension.P)
            } else {
                // locArea == INTERIOR
                updateDim(isAreaA, Location.INTERIOR, Location.INTERIOR, Dimension.A)
                updateDim(isAreaA, Location.INTERIOR, Location.BOUNDARY, Dimension.L)
                updateDim(isAreaA, Location.INTERIOR, Location.EXTERIOR, Dimension.A)
            }
        } else {
            //-- locTarget is INTERIOR or EXTERIOR
            updateDim(isAreaA, Location.INTERIOR, locTarget, Dimension.A)
            /*
             * If area vertex is on Boundary further topology can be deduced
             * from the neighbourhood around the boundary vertex.
             */
            if (locArea == Location.BOUNDARY) {
                updateDim(isAreaA, Location.BOUNDARY, locTarget, Dimension.L)
                updateDim(isAreaA, Location.EXTERIOR, locTarget, Dimension.A)
            }
        }
    }

    fun evaluateNodes() {
        for (nodeSections in nodeMap.values) {
            if (nodeSections.hasInteractionAB()) {
                evaluateNode(nodeSections)
                if (isResultKnown())
                    return
            }
        }
    }

    private fun evaluateNode(nodeSections: NodeSections) {
        val p = nodeSections.getCoordinate()
        val node = nodeSections.createNode()
        //-- Node must have edges for geom, but may also be in interior of a overlapping GC
        val isAreaInteriorA = geomA.isNodeInArea(p, nodeSections.getPolygonal(RelateGeometry.GEOM_A))
        val isAreaInteriorB = geomB.isNodeInArea(p, nodeSections.getPolygonal(RelateGeometry.GEOM_B))
        node.finish(isAreaInteriorA, isAreaInteriorB)
        evaluateNodeEdges(node)
    }

    private fun evaluateNodeEdges(node: RelateNode) {
        //TODO: collect distinct dim settings by using temporary matrix?
        for (e in node.getEdges()) {
            //-- An optimization to avoid updates for cases with a linear geometry
            if (isAreaArea()) {
                updateDim(
                    e.location(RelateGeometry.GEOM_A, Position.LEFT),
                    e.location(RelateGeometry.GEOM_B, Position.LEFT), Dimension.A
                )
                updateDim(
                    e.location(RelateGeometry.GEOM_A, Position.RIGHT),
                    e.location(RelateGeometry.GEOM_B, Position.RIGHT), Dimension.A
                )
            }
            updateDim(
                e.location(RelateGeometry.GEOM_A, Position.ON),
                e.location(RelateGeometry.GEOM_B, Position.ON), Dimension.L
            )
        }
    }
}
