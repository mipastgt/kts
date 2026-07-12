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
package org.locationtech.jts.algorithm.hull

import kotlin.jvm.JvmStatic


import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Envelope
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryCollection
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.LinearRing
import org.locationtech.jts.geom.MultiPolygon
import org.locationtech.jts.geom.Polygon
import org.locationtech.jts.operation.overlayng.CoverageUnion
import org.locationtech.jts.triangulate.polygon.ConstrainedDelaunayTriangulator
import org.locationtech.jts.triangulate.tri.Tri

/**
 * Constructs a concave hull of a set of polygons, respecting
 * the polygons as constraints.
 * A concave hull is a concave or convex polygon containing all the input polygons,
 * whose vertices are a subset of the vertices in the input.
 * The computed hull "fills the gap" between the polygons,
 * and does not intersect their interior.
 *
 * The input polygons must form a valid [MultiPolygon]
 * (i.e. they must be non-overlapping and non-edge-adjacent).
 *
 * @author Martin Davis
 */
class ConcaveHullOfPolygons(polygons: Geometry) {

  private val inputPolygons: Geometry
  private var maxEdgeLength = 0.0
  private var maxEdgeLengthRatio = NOT_SPECIFIED.toDouble()
  private var isHolesAllowed = false
  private var isTight = false

  private val geomFactory: GeometryFactory
  private lateinit var polygonRings: Array<LinearRing>

  private lateinit var hullTris: MutableSet<Tri>
  private lateinit var borderTriQue: ArrayDeque<Tri>
  /**
   * Records the edge index of the longest border edge for border tris,
   * so it can be tested for length and possible removal.
   */
  private val borderEdgeMap: MutableMap<Tri, Int> = HashMap()

  init {
    if (!(polygons is Polygon || polygons is MultiPolygon)) {
      throw IllegalArgumentException("Input must be polygonal")
    }
    this.inputPolygons = polygons
    geomFactory = inputPolygons.getFactory()
  }

  /**
   * Sets the target maximum edge length for the concave hull.
   * The length value must be zero or greater.
   *
   * @param edgeLength a non-negative length
   */
  fun setMaximumEdgeLength(edgeLength: Double) {
    if (edgeLength < 0)
      throw IllegalArgumentException("Edge length must be non-negative")
    this.maxEdgeLength = edgeLength
    maxEdgeLengthRatio = NOT_SPECIFIED.toDouble()
  }

  /**
   * Sets the target maximum edge length ratio for the concave hull.
   * It is a value in the range 0 to 1.
   *
   * @param edgeLengthRatio a length factor value between 0 and 1
   */
  fun setMaximumEdgeLengthRatio(edgeLengthRatio: Double) {
    if (edgeLengthRatio < 0 || edgeLengthRatio > 1)
      throw IllegalArgumentException("Edge length ratio must be in range [0,1]")
    this.maxEdgeLengthRatio = edgeLengthRatio
  }

  /**
   * Sets whether holes are allowed in the concave hull polygon.
   *
   * @param isHolesAllowed true if holes are allowed in the result
   */
  fun setHolesAllowed(isHolesAllowed: Boolean) {
    this.isHolesAllowed = isHolesAllowed
  }

  /**
   * Sets whether the boundary of the hull polygon is kept
   * tight to the outer edges of the input polygons.
   *
   * @param isTight true if the boundary is kept tight
   */
  fun setTight(isTight: Boolean) {
    this.isTight = isTight
  }

  /**
   * Gets the computed concave hull.
   *
   * @return the concave hull
   */
  fun getHull(): Geometry {
    if (inputPolygons.isEmpty()) {
      return createEmptyHull()
    }
    buildHullTris()
    val hull = createHullGeometry(hullTris, true)
    return hull
  }

  /**
   * Gets the concave fill, which is the area between the input polygons,
   * subject to the concaveness control parameter.
   *
   * @return the concave fill
   */
  fun getFill(): Geometry {
    isTight = true
    if (inputPolygons.isEmpty()) {
      return createEmptyHull()
    }
    buildHullTris()
    val fill = createHullGeometry(hullTris, false)
    return fill
  }

  private fun createEmptyHull(): Geometry {
    return geomFactory.createPolygon()
  }

  private fun buildHullTris() {
    polygonRings = extractShellRings(inputPolygons)
    val frame = createFrame(inputPolygons.getEnvelopeInternal(), polygonRings, geomFactory)
    val cdt = ConstrainedDelaunayTriangulator(frame)
    val tris = cdt.getTriangles()

    val framePts = frame.getExteriorRing().getCoordinates()
    if (maxEdgeLengthRatio >= 0) {
      maxEdgeLength = computeTargetEdgeLength(tris, framePts, maxEdgeLengthRatio)
    }

    hullTris = removeFrameCornerTris(tris, framePts)

    removeBorderTris()
    if (isHolesAllowed) removeHoleTris()
  }

  private fun removeFrameCornerTris(tris: List<Tri>, frameCorners: Array<Coordinate>): MutableSet<Tri> {
    val hullTris = HashSet<Tri>()
    borderTriQue = ArrayDeque<Tri>()
    for (tri in tris) {
      val index = vertexIndex(tri, frameCorners)
      val isFrame = index != NOT_FOUND
      if (isFrame) {
        /**
         * Frame tris are adjacent to at most one border tri,
         * which is opposite the frame corner vertex.
         */
        val oppIndex = Tri.oppEdge(index)
        val oppTri = tri.getAdjacent(oppIndex)
        val isBorderTri = oppTri != null && !isFrameTri(oppTri, frameCorners)
        if (isBorderTri) {
          addBorderTri(tri, oppIndex)
        }
        //-- remove the frame tri
        tri.remove()
      } else {
        hullTris.add(tri)
      }
    }
    return hullTris
  }

  private fun removeBorderTris() {
    while (!borderTriQue.isEmpty()) {
      val tri = borderTriQue.removeFirst()
      //-- tri might have been removed already
      if (!hullTris.contains(tri)) {
        continue
      }
      if (isRemovable(tri)) {
        addBorderTris(tri)
        removeBorderTri(tri)
      }
    }
  }

  private fun removeHoleTris() {
    while (true) {
      val holeTri = findHoleSeedTri(hullTris) ?: return
      addBorderTris(holeTri)
      removeBorderTri(holeTri)
      removeBorderTris()
    }
  }

  private fun findHoleSeedTri(tris: MutableSet<Tri>): Tri? {
    for (tri in tris) {
      if (isHoleSeedTri(tri))
        return tri
    }
    return null
  }

  private fun isHoleSeedTri(tri: Tri): Boolean {
    if (isBorderTri(tri))
      return false
    for (i in 0 until 3) {
      if (tri.hasAdjacent(i) &&
          tri.getLength(i) > maxEdgeLength)
        return true
    }
    return false
  }

  private fun isBorderTri(tri: Tri): Boolean {
    for (i in 0 until 3) {
      if (!tri.hasAdjacent(i))
        return true
    }
    return false
  }

  private fun isRemovable(tri: Tri): Boolean {
    //-- remove non-bridging tris if keeping hull boundary tight
    if (isTight && isTouchingSinglePolygon(tri))
      return true

    //-- check if outside edge is longer than threshold
    if (borderEdgeMap.containsKey(tri)) {
      val borderEdgeIndex = borderEdgeMap[tri]!!
      val edgeLen = tri.getLength(borderEdgeIndex)
      if (edgeLen > maxEdgeLength)
        return true
    }
    return false
  }

  /**
   * Tests whether a triangle touches a single polygon at all vertices.
   *
   * @param tri
   * @return true if the tri touches a single polygon
   */
  private fun isTouchingSinglePolygon(tri: Tri): Boolean {
    val envTri = envelope(tri)
    for (ring in polygonRings) {
      //-- optimization heuristic: a touching tri must be in ring envelope
      if (ring.getEnvelopeInternal().intersects(envTri)) {
        if (hasAllVertices(ring, tri))
          return true
      }
    }
    return false
  }

  private fun addBorderTris(tri: Tri) {
    addBorderTri(tri, 0)
    addBorderTri(tri, 1)
    addBorderTri(tri, 2)
  }

  /**
   * Adds an adjacent tri to the current border.
   * The adjacent edge is recorded as the border edge for the tri.
   *
   * @param tri the tri adjacent to the tri to be added to the border
   * @param index the index of the adjacent tri
   */
  private fun addBorderTri(tri: Tri, index: Int) {
    val adj = tri.getAdjacent(index)
    if (adj == null)
      return
    borderTriQue.add(adj)
    val borderEdgeIndex = adj.getIndex(tri)
    borderEdgeMap.put(adj, borderEdgeIndex)
  }

  private fun removeBorderTri(tri: Tri) {
    tri.remove()
    hullTris.remove(tri)
    borderEdgeMap.remove(tri)
  }

  companion object {
    private const val FRAME_EXPAND_FACTOR = 4
    private const val NOT_SPECIFIED = -1
    private const val NOT_FOUND = -1

    /**
     * Computes a concave hull of set of polygons
     * using the target criterion of maximum edge length.
     *
     * @param polygons the input polygons
     * @param maxLength the target maximum edge length
     * @return the concave hull
     */
    @JvmStatic
    fun concaveHullByLength(polygons: Geometry, maxLength: Double): Geometry {
      return concaveHullByLength(polygons, maxLength, false, false)
    }

    /**
     * Computes a concave hull of set of polygons
     * using the target criterion of maximum edge length,
     * and allowing control over whether the hull boundary is tight
     * and can contain holes.
     *
     * @param polygons the input polygons
     * @param maxLength the target maximum edge length
     * @param isTight true if the hull should be tight to the outside of the polygons
     * @param isHolesAllowed true if holes are allowed in the hull polygon
     * @return the concave hull
     */
    @JvmStatic
    fun concaveHullByLength(polygons: Geometry, maxLength: Double,
        isTight: Boolean, isHolesAllowed: Boolean): Geometry {
      val hull = ConcaveHullOfPolygons(polygons)
      hull.setMaximumEdgeLength(maxLength)
      hull.setHolesAllowed(isHolesAllowed)
      hull.setTight(isTight)
      return hull.getHull()
    }

    /**
     * Computes a concave hull of set of polygons
     * using the target criterion of maximum edge length ratio.
     *
     * @param polygons the input polygons
     * @param lengthRatio the target maximum edge length ratio
     * @return the concave hull
     */
    @JvmStatic
    fun concaveHullByLengthRatio(polygons: Geometry, lengthRatio: Double): Geometry {
      return concaveHullByLengthRatio(polygons, lengthRatio, false, false)
    }

    /**
     * Computes a concave hull of set of polygons
     * using the target criterion of maximum edge length ratio,
     * and allowing control over whether the hull boundary is tight
     * and can contain holes.
     *
     * @param polygons the input polygons
     * @param lengthRatio the target maximum edge length ratio
     * @param isTight true if the hull should be tight to the outside of the polygons
     * @param isHolesAllowed true if holes are allowed in the hull polygon
     * @return the concave hull
     */
    @JvmStatic
    fun concaveHullByLengthRatio(polygons: Geometry, lengthRatio: Double,
        isTight: Boolean, isHolesAllowed: Boolean): Geometry {
      val hull = ConcaveHullOfPolygons(polygons)
      hull.setMaximumEdgeLengthRatio(lengthRatio)
      hull.setHolesAllowed(isHolesAllowed)
      hull.setTight(isTight)
      return hull.getHull()
    }

    /**
     * Computes a concave fill area between a set of polygons,
     * using the target criterion of maximum edge length.
     *
     * @param polygons the input polygons
     * @param maxLength the target maximum edge length
     * @return the concave fill
     */
    @JvmStatic
    fun concaveFillByLength(polygons: Geometry, maxLength: Double): Geometry {
      val hull = ConcaveHullOfPolygons(polygons)
      hull.setMaximumEdgeLength(maxLength)
      return hull.getFill()
    }

    /**
     * Computes a concave fill area between a set of polygons,
     * using the target criterion of maximum edge length ratio.
     *
     * @param polygons the input polygons
     * @param lengthRatio the target maximum edge length ratio
     * @return the concave fill
     */
    @JvmStatic
    fun concaveFillByLengthRatio(polygons: Geometry, lengthRatio: Double): Geometry {
      val hull = ConcaveHullOfPolygons(polygons)
      hull.setMaximumEdgeLengthRatio(lengthRatio)
      return hull.getFill()
    }

    private fun computeTargetEdgeLength(triList: List<Tri>,
        frameCorners: Array<Coordinate>,
        edgeLengthRatio: Double): Double {
      if (edgeLengthRatio == 0.0) return 0.0
      var maxEdgeLen = -1.0
      var minEdgeLen = -1.0
      for (tri in triList) {
        //-- don't include frame triangles
        if (isFrameTri(tri, frameCorners))
          continue

        for (i in 0 until 3) {
          //-- constraint edges are not used to determine ratio
          if (!tri.hasAdjacent(i))
            continue

          val len = tri.getLength(i)
          if (len > maxEdgeLen)
            maxEdgeLen = len
          if (minEdgeLen < 0 || len < minEdgeLen)
            minEdgeLen = len
        }
      }
      //-- if ratio = 1 ensure all edges are included
      if (edgeLengthRatio == 1.0)
        return 2 * maxEdgeLen

      return edgeLengthRatio * (maxEdgeLen - minEdgeLen) + minEdgeLen
    }

    private fun isFrameTri(tri: Tri, frameCorners: Array<Coordinate>): Boolean {
      val index = vertexIndex(tri, frameCorners)
      val isFrame = index >= 0
      return isFrame
    }

    /**
     * Get the tri vertex index of some point in a list,
     * or -1 if none are vertices.
     *
     * @param tri the tri to test for containing a point
     * @param pts the points to test
     * @return the vertex index of a point, or -1
     */
    private fun vertexIndex(tri: Tri, pts: Array<Coordinate>): Int {
      for (p in pts) {
        val index = tri.getIndex(p)
        if (index >= 0)
          return index
      }
      return NOT_FOUND
    }

    private fun hasAllVertices(ring: LinearRing, tri: Tri): Boolean {
      for (i in 0 until 3) {
        val v = tri.getCoordinate(i)
        if (!hasVertex(ring, v)) {
          return false
        }
      }
      return true
    }

    private fun hasVertex(ring: LinearRing, v: Coordinate): Boolean {
      for (i in 1 until ring.getNumPoints()) {
        if (v.equals2D(ring.getCoordinateN(i))) {
          return true
        }
      }
      return false
    }

    private fun envelope(tri: Tri): Envelope {
      val env = Envelope(tri.getCoordinate(0), tri.getCoordinate(1))
      env.expandToInclude(tri.getCoordinate(2))
      return env
    }

    /**
     * Creates a rectangular "frame" around the input polygons,
     * with the input polygons as holes in it.
     *
     * @param polygonsEnv
     * @param polygonRings
     * @param geomFactory
     * @return the frame polygon
     */
    private fun createFrame(polygonsEnv: Envelope, polygonRings: Array<LinearRing>, geomFactory: GeometryFactory): Polygon {
      val diam = polygonsEnv.getDiameter()
      val envFrame = polygonsEnv.copy()
      envFrame.expandBy(FRAME_EXPAND_FACTOR * diam)
      val frameOuter = geomFactory.toGeometry(envFrame) as Polygon
      val shell = frameOuter.getExteriorRing().copy() as LinearRing
      val frame = geomFactory.createPolygon(shell, polygonRings)
      return frame
    }

    private fun extractShellRings(polygons: Geometry): Array<LinearRing> {
      return Array(polygons.getNumGeometries()) { i ->
        val consPoly = polygons.getGeometryN(i) as Polygon
        consPoly.getExteriorRing().copy() as LinearRing
      }
    }
  }

  private fun createHullGeometry(hullTris: MutableSet<Tri>, isIncludeInput: Boolean): Geometry {
    if (!isIncludeInput && hullTris.isEmpty())
      return createEmptyHull()

    //-- union triangulation
    val triCoverage = Tri.toGeometry(hullTris, geomFactory)
    val fillGeometry = CoverageUnion.union(triCoverage)

    if (!isIncludeInput) {
      return fillGeometry
    }
    if (fillGeometry.isEmpty()) {
      return inputPolygons.copy()
    }
    //-- union with input polygons
    val geoms = arrayOf(fillGeometry, inputPolygons)
    val geomColl = geomFactory.createGeometryCollection(geoms)
    val hull = CoverageUnion.union(geomColl)
    return hull
  }
}
