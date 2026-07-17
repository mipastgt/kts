/*
 * Copyright (c) 2021 Martin Davis.
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
import kotlin.math.sqrt

import org.locationtech.jts.util.PriorityQueue

import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.triangulate.tri.Tri

/**
 * Constructs a concave hull of a set of points.
 * A concave hull is a concave or convex polygon containing all the input points,
 * whose vertices are a subset of the vertices in the input.
 *
 * The hull is constructed by removing border triangles
 * of the Delaunay Triangulation of the points,
 * as long as their "size" is larger than the target criterion.
 *
 * The computed hull is always a single connected [org.locationtech.jts.geom.Polygon]
 * (unless it is degenerate, in which case it will be a [org.locationtech.jts.geom.Point] or a [org.locationtech.jts.geom.LineString]).
 *
 * @author Martin Davis
 */
class ConcaveHull(geom: Geometry) {

  private val inputGeometry: Geometry = geom
  private var maxEdgeLengthRatio = -1.0
  private var alpha = -1.0
  private var isHolesAllowed = false

  private var criteriaType = PARAM_EDGE_LENGTH
  private var maxSizeInHull = 0.0
  private val geomFactory: GeometryFactory = geom.getFactory()

  /**
   * Sets the target maximum edge length for the concave hull.
   * The length value must be zero or greater.
   *
   * @param edgeLength a non-negative length
   *
   * @see uniformGridEdgeLength
   */
  fun setMaximumEdgeLength(edgeLength: Double) {
    if (edgeLength < 0)
      throw IllegalArgumentException("Edge length must be non-negative")
    this.maxSizeInHull = edgeLength
    maxEdgeLengthRatio = -1.0
    criteriaType = PARAM_EDGE_LENGTH
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
    criteriaType = PARAM_EDGE_LENGTH
  }

  /**
   * Sets the alpha parameter to compute an alpha shape of the input.
   * Alpha is the radius of the eroding disc.
   *
   * @param alpha the alpha radius
   */
  fun setAlpha(alpha: Double) {
    this.alpha = alpha
    maxSizeInHull = alpha
    criteriaType = PARAM_ALPHA
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
   * Gets the computed concave hull.
   *
   * @return the concave hull
   */
  fun getHull(): Geometry {
    if (inputGeometry.isEmpty()) {
      return geomFactory.createPolygon()
    }
    val triList = HullTriangulation.createDelaunayTriangulation(inputGeometry)
    setSize(triList)

    if (maxEdgeLengthRatio >= 0) {
      maxSizeInHull = computeTargetEdgeLength(triList, maxEdgeLengthRatio)
    }
    if (triList.isEmpty())
      return inputGeometry.convexHull()

    computeHull(triList)

    val hull = toGeometry(triList, geomFactory)
    return hull
  }

  private fun setSize(triList: MutableList<HullTri>) {
    for (tri in triList) {
      if (criteriaType == PARAM_EDGE_LENGTH) {
        tri.setSizeToLongestEdge()
      } else {
        tri.setSizeToCircumradius()
      }
    }
  }

  /**
   * Computes the concave hull using edge length as the target criterion.
   *
   * @param triList
   */
  private fun computeHull(triList: MutableList<HullTri>) {
    computeHullBorder(triList)
    if (isHolesAllowed) {
      computeHullHoles(triList)
    }
  }

  private fun computeHullBorder(triList: MutableList<HullTri>) {
    val queue = createBorderQueue(triList)
    // process tris in order of decreasing size (edge length or circumradius)
    while (!queue.isEmpty()) {
      val tri = queue.poll()!!

      if (isInHull(tri))
        break

      if (isRemovableBorder(tri)) {
        //-- the non-null adjacents are now on the border
        val adj0 = tri.getAdjacent(0) as HullTri?
        val adj1 = tri.getAdjacent(1) as HullTri?
        val adj2 = tri.getAdjacent(2) as HullTri?

        tri.remove(triList)

        //-- add border adjacents to queue
        addBorderTri(adj0, queue)
        addBorderTri(adj1, queue)
        addBorderTri(adj2, queue)
      }
    }
  }

  private fun createBorderQueue(triList: MutableList<HullTri>): PriorityQueue<HullTri> {
    val queue = PriorityQueue<HullTri>()
    for (tri in triList) {
      addBorderTri(tri, queue)
    }
    return queue
  }

  /**
   * Adds a Tri to the queue.
   * Only add tris with a single border edge,
   * since otherwise that would risk isolating a vertex if
   * the tri ends up being eroded from the hull.
   *
   * @param tri the Tri to add
   * @param queue the priority queue to add to
   */
  private fun addBorderTri(tri: HullTri?, queue: PriorityQueue<HullTri>) {
    if (tri == null) return
    if (tri.numAdjacent() != 2) return
    setSize(tri)
    queue.add(tri)
  }

  private fun setSize(tri: HullTri) {
    if (criteriaType == PARAM_EDGE_LENGTH)
      tri.setSizeToBoundary()
    else
      tri.setSizeToCircumradius()
  }

  /**
   * Tests if a tri is included in the hull.
   * Tris with size less than the maximum are included in the hull.
   *
   * @param tri the tri to test
   * @return true if the tri is included in the hull
   */
  private fun isInHull(tri: HullTri): Boolean {
    return tri.getSize() < maxSizeInHull
  }

  private fun computeHullHoles(triList: MutableList<HullTri>) {
    val candidateHoles = findCandidateHoles(triList, maxSizeInHull)
    // remove tris in order of decreasing size (edge length)
    for (tri in candidateHoles) {
      if (tri.isRemoved() ||
          tri.isBorder() ||
          tri.hasBoundaryTouch())
        continue
      removeHole(triList, tri)
    }
  }

  /**
   * Erodes a hole starting at a given triangle,
   * and eroding all adjacent triangles with boundary edge length above target.
   * @param triList the triangulation
   * @param triHole triangle which is a hole
   */
  private fun removeHole(triList: MutableList<HullTri>, triHole: HullTri) {
    val queue = PriorityQueue<HullTri>()
    queue.add(triHole)

    while (!queue.isEmpty()) {
      val tri = queue.poll()!!

      if (tri !== triHole && isInHull(tri))
        break

      if (tri === triHole || isRemovableHole(tri)) {
        //-- the non-null adjacents are now on the border
        val adj0 = tri.getAdjacent(0) as HullTri?
        val adj1 = tri.getAdjacent(1) as HullTri?
        val adj2 = tri.getAdjacent(2) as HullTri?

        tri.remove(triList)

        //-- add border adjacents to queue
        addBorderTri(adj0, queue)
        addBorderTri(adj1, queue)
        addBorderTri(adj2, queue)
      }
    }
  }

  private fun isRemovableBorder(tri: HullTri): Boolean {
    /*
     * Tri must have exactly 2 adjacent tris (i.e. a single boundary edge).
     */
    if (tri.numAdjacent() != 2) return false
    /*
     * The tri cannot be removed if it is connecting, because
     * this would create more than one result polygon.
     */
    return !tri.isConnecting()
  }

  private fun isRemovableHole(tri: HullTri): Boolean {
    /*
     * Tri must have exactly 2 adjacent tris (i.e. a single boundary edge).
     */
    if (tri.numAdjacent() != 2) return false
    /*
     * Ensure removal does not disconnect hull area.
     */
    return !tri.hasBoundaryTouch()
  }

  private fun toGeometry(triList: MutableList<HullTri>, geomFactory: GeometryFactory): Geometry {
    if (!isHolesAllowed) {
      return HullTriangulation.traceBoundaryPolygon(triList, geomFactory)
    }
    //-- in case holes are present use union (slower but handles holes)
    return HullTriangulation.union(triList, geomFactory)
  }

  companion object {
    private const val PARAM_EDGE_LENGTH = 1
    private const val PARAM_ALPHA = 2

    /**
     * Computes the approximate edge length of
     * a uniform square grid having the same number of
     * points as a geometry and the same area as its convex hull.
     *
     * @param geom a geometry
     * @return the approximate uniform grid length
     */
    @JvmStatic
    fun uniformGridEdgeLength(geom: Geometry): Double {
      val areaCH = geom.convexHull().getArea()
      val numPts = geom.getNumPoints()
      return sqrt(areaCH / numPts)
    }

    /**
     * Computes a concave hull of the vertices in a geometry
     * using the target criterion of maximum edge length.
     *
     * @param geom the input geometry
     * @param maxLength the target maximum edge length
     * @return the concave hull
     */
    @JvmStatic
    fun concaveHullByLength(geom: Geometry, maxLength: Double): Geometry {
      return concaveHullByLength(geom, maxLength, false)
    }

    /**
     * Computes a concave hull of the vertices in a geometry
     * using the target criterion of maximum edge length,
     * and optionally allowing holes.
     *
     * @param geom the input geometry
     * @param maxLength the target maximum edge length
     * @param isHolesAllowed whether holes are allowed in the result
     * @return the concave hull
     */
    @JvmStatic
    fun concaveHullByLength(geom: Geometry, maxLength: Double, isHolesAllowed: Boolean): Geometry {
      val hull = ConcaveHull(geom)
      hull.setMaximumEdgeLength(maxLength)
      hull.setHolesAllowed(isHolesAllowed)
      return hull.getHull()
    }

    /**
     * Computes a concave hull of the vertices in a geometry
     * using the target criterion of maximum edge length ratio.
     *
     * @param geom the input geometry
     * @param lengthRatio the target edge length factor
     * @return the concave hull
     */
    @JvmStatic
    fun concaveHullByLengthRatio(geom: Geometry, lengthRatio: Double): Geometry {
      return concaveHullByLengthRatio(geom, lengthRatio, false)
    }

    /**
     * Computes a concave hull of the vertices in a geometry
     * using the target criterion of maximum edge length factor,
     * and optionally allowing holes.
     *
     * @param geom the input geometry
     * @param lengthRatio the target maximum edge length ratio
     * @param isHolesAllowed whether holes are allowed in the result
     * @return the concave hull
     */
    @JvmStatic
    fun concaveHullByLengthRatio(geom: Geometry, lengthRatio: Double, isHolesAllowed: Boolean): Geometry {
      val hull = ConcaveHull(geom)
      hull.setMaximumEdgeLengthRatio(lengthRatio)
      hull.setHolesAllowed(isHolesAllowed)
      return hull.getHull()
    }

    /**
     * Computes the alpha shape of a geometry as a polygon.
     *
     * @param geom the input geometry
     * @param alpha the radius of the eroding disc
     * @param isHolesAllowed whether holes are allowed in the result
     * @return the alpha shape polygon
     */
    @JvmStatic
    fun alphaShape(geom: Geometry, alpha: Double, isHolesAllowed: Boolean): Geometry {
      val hull = ConcaveHull(geom)
      hull.setAlpha(alpha)
      hull.setHolesAllowed(isHolesAllowed)
      return hull.getHull()
    }

    private fun computeTargetEdgeLength(triList: MutableList<HullTri>, edgeLengthRatio: Double): Double {
      if (edgeLengthRatio == 0.0) return 0.0
      var maxEdgeLen = -1.0
      var minEdgeLen = -1.0
      for (tri in triList) {
        for (i in 0 until 3) {
          val len = tri.getCoordinate(i).distance(tri.getCoordinate(Tri.next(i)))
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

    /**
     * Finds tris which may be the start of holes.
     *
     * @param triList
     * @param maxSizeInHull maximum tri size which is not in a hole
     * @return
     */
    private fun findCandidateHoles(triList: MutableList<HullTri>, maxSizeInHull: Double): MutableList<HullTri> {
      val candidates = ArrayList<HullTri>()
      for (tri in triList) {
        //-- tris below the size threshold are in the hull, so NOT in a hole
        if (tri.getSize() < maxSizeInHull) continue

        val isTouchingBoundary = tri.isBorder() || tri.hasBoundaryTouch()
        if (!isTouchingBoundary) {
          candidates.add(tri)
        }
      }
      // sort by HullTri comparator - larger sizes first
      candidates.sort()
      return candidates
    }
  }
}
