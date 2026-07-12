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
package org.locationtech.jts.triangulate.tri

import kotlin.jvm.JvmStatic
import kotlin.jvm.JvmField

import org.locationtech.jts.algorithm.Orientation
import org.locationtech.jts.algorithm.RobustLineIntersector
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.Polygon
import org.locationtech.jts.geom.Triangle
import org.locationtech.jts.io.WKTWriter
import org.locationtech.jts.util.Assert

/**
 * A memory-efficient representation of a triangle in a triangulation.
 * Contains three vertices, and links to adjacent Tris for each edge.
 * Tris are constructed independently, and if needed linked
 * into a triangulation using [TriangulationBuilder].
 *
 * @author Martin Davis
 */
open class Tri(p0: Coordinate, p1: Coordinate, p2: Coordinate) {

  @JvmField
  protected var p0: Coordinate = p0
  @JvmField
  protected var p1: Coordinate = p1
  @JvmField
  protected var p2: Coordinate = p2

  /**
   * triN is the adjacent triangle across the edge pN - pNN.
   * pNN is the next vertex CW from pN.
   */
  @JvmField
  protected var tri0: Tri? = null
  @JvmField
  protected var tri1: Tri? = null
  @JvmField
  protected var tri2: Tri? = null

  /**
   * Sets the adjacent triangles.
   */
  fun setAdjacent(tri0: Tri?, tri1: Tri?, tri2: Tri?) {
    this.tri0 = tri0
    this.tri1 = tri1
    this.tri2 = tri2
  }

  /**
   * Sets the triangle adjacent to the edge originating
   * at a given vertex.
   */
  fun setAdjacent(pt: Coordinate, tri: Tri) {
    val index = getIndex(pt)
    setTri(index, tri)
    // TODO: validate that tri is adjacent at the edge specified
  }

  /**
   * Sets the triangle adjacent to an edge.
   */
  fun setTri(edgeIndex: Int, tri: Tri?) {
    when (edgeIndex) {
      0 -> { tri0 = tri; return }
      1 -> { tri1 = tri; return }
      2 -> { tri2 = tri; return }
    }
    throw IllegalArgumentException(INVALID_TRI_INDEX)
  }

  private fun setCoordinates(p0: Coordinate, p1: Coordinate, p2: Coordinate) {
    this.p0 = p0
    this.p1 = p1
    this.p2 = p2
  }

  /**
   * Splits a triangle by a point located inside the triangle.
   *
   * @param p the point to insert
   * @return the new triangle whose 0'th vertex is p
   */
  fun split(p: Coordinate): Tri {
    val tt0 = Tri(p, p0, p1)
    val tt1 = Tri(p, p1, p2)
    val tt2 = Tri(p, p2, p0)
    tt0.setAdjacent(tt2, tri0, tt1)
    tt1.setAdjacent(tt0, tri1, tt2)
    tt2.setAdjacent(tt1, tri2, tt0)
    return tt0
  }

  /**
   * Interchanges the vertices of this triangle and a neighbor
   * so that their common edge
   * becomes the the other diagonal of the quadrilateral they form.
   *
   * @param index the index of the adjacent tri to flip with
   */
  fun flip(index: Int) {
    val tri = getAdjacent(index)!!
    val index1 = tri.getIndex(this)

    val adj0 = getCoordinate(index)
    val adj1 = getCoordinate(next(index))
    val opp0 = getCoordinate(oppVertex(index))
    val opp1 = tri.getCoordinate(oppVertex(index1))

    flip(tri, index, index1, adj0, adj1, opp0, opp1)
  }

  private fun flip(tri: Tri, index0: Int, index1: Int, adj0: Coordinate, adj1: Coordinate, opp0: Coordinate, opp1: Coordinate) {
    this.setCoordinates(opp1, opp0, adj0)
    tri.setCoordinates(opp0, opp1, adj1)
    /**
     *  Order: 0: opp0-adj0 edge, 1: opp0-adj1 edge,
     *  2: opp1-adj0 edge, 3: opp1-adj1 edge
     */
    val adjacent = getAdjacentTris(tri, index0, index1)
    this.setAdjacent(tri, adjacent[0], adjacent[2])
    //--- update the adjacent triangles with new adjacency
    if (adjacent[2] != null) {
      adjacent[2]!!.replace(tri, this)
    }
    tri.setAdjacent(this, adjacent[3], adjacent[1])
    if (adjacent[1] != null) {
      adjacent[1]!!.replace(this, tri)
    }
  }

  /**
   * Replaces an adjacent triangle with a different one.
   */
  private fun replace(triOld: Tri, triNew: Tri) {
    if (tri0 != null && tri0 === triOld) {
      tri0 = triNew
    } else if (tri1 != null && tri1 === triOld) {
      tri1 = triNew
    } else if (tri2 != null && tri2 === triOld) {
      tri2 = triNew
    }
  }

  /**
   * Computes the degree of a Tri vertex, which is the number of tris containing it.
   *
   * @param index the vertex index
   * @param triList the triangulation
   * @return the degree of the vertex
   */
  fun degree(index: Int, triList: List<Tri>): Int {
    val v = getCoordinate(index)
    var degree = 0
    for (tri in triList) {
      for (i in 0 until 3) {
        if (v.equals2D(tri.getCoordinate(i)))
          degree++
      }
    }
    return degree
  }

  /**
   * Removes this tri from the triangulation containing it.
   *
   * @param triList the triangulation
   */
  fun remove(triList: MutableList<out Tri>) {
    remove()
    @Suppress("UNCHECKED_CAST")
    (triList as MutableList<Tri>).remove(this)
  }

  /**
   * Removes this triangle from a triangulation.
   */
  fun remove() {
    remove(0)
    remove(1)
    remove(2)
  }

  private fun remove(index: Int) {
    val adj = getAdjacent(index) ?: return
    adj.setTri(adj.getIndex(this), null)
    setTri(index, null)
  }

  /**
   * Gets the triangles adjacent to the quadrilateral
   * formed by this triangle and an adjacent one.
   */
  private fun getAdjacentTris(triAdj: Tri, index: Int, indexAdj: Int): Array<Tri?> {
    val adj = arrayOfNulls<Tri>(4)
    adj[0] = getAdjacent(prev(index))
    adj[1] = getAdjacent(next(index))
    adj[2] = triAdj.getAdjacent(next(indexAdj))
    adj[3] = triAdj.getAdjacent(prev(indexAdj))
    return adj
  }

  /**
   * Validates that a tri is correct.
   */
  fun validate() {
    if (Orientation.CLOCKWISE != Orientation.index(p0, p1, p2)) {
      throw IllegalArgumentException("Tri is not oriented correctly")
    }

    validateAdjacent(0)
    validateAdjacent(1)
    validateAdjacent(2)
  }

  /**
   * Validates that the vertices of an adjacent linked triangle are correct.
   */
  fun validateAdjacent(index: Int) {
    val tri = getAdjacent(index) ?: return

    Assert.isTrue(this.isAdjacent(tri))
    Assert.isTrue(tri.isAdjacent(this))

    val e0 = getCoordinate(index)
    val e1 = getCoordinate(next(index))
    val indexNeighbor = tri.getIndex(this)
    val n0 = tri.getCoordinate(indexNeighbor)
    val n1 = tri.getCoordinate(next(indexNeighbor))
    Assert.isTrue(e0.equals2D(n1), "Edge coord not equal")
    Assert.isTrue(e1.equals2D(n0), "Edge coord not equal")

    //--- check that no edges cross
    val li = RobustLineIntersector()
    for (i in 0 until 3) {
      for (j in 0 until 3) {
        val p00 = getCoordinate(i)
        val p01 = getCoordinate(next(i))
        val p10 = tri.getCoordinate(j)
        val p11 = tri.getCoordinate(next(j))
        li.computeIntersection(p00, p01, p10, p11)
        Assert.isTrue(!li.isProper())
      }
    }
  }

  /**
   * Gets the coordinate for a vertex.
   *
   * @param index the vertex (edge) index
   * @return the vertex coordinate
   */
  fun getCoordinate(index: Int): Coordinate {
    when (index) {
      0 -> return p0
      1 -> return p1
      2 -> return p2
    }
    throw IllegalArgumentException(INVALID_TRI_INDEX)
  }

  /**
   * Gets the index of the triangle vertex which has a given coordinate (if any).
   *
   * @param p the coordinate to find
   * @return the vertex index, or -1 if it is not in the triangle
   */
  fun getIndex(p: Coordinate): Int {
    if (p0.equals2D(p))
      return 0
    if (p1.equals2D(p))
      return 1
    if (p2.equals2D(p))
      return 2
    return -1
  }

  /**
   * Gets the edge index which a triangle is adjacent to (if any).
   *
   * @param tri the tri to find
   * @return the index of the edge adjacent to the triangle, or -1 if not found
   */
  fun getIndex(tri: Tri?): Int {
    if (tri0 === tri)
      return 0
    if (tri1 === tri)
      return 1
    if (tri2 === tri)
      return 2
    return -1
  }

  /**
   * Gets the triangle adjacent to an edge.
   *
   * @param index the edge index
   * @return the adjacent triangle (may be null)
   */
  fun getAdjacent(index: Int): Tri? {
    when (index) {
      0 -> return tri0
      1 -> return tri1
      2 -> return tri2
    }
    throw IllegalArgumentException(INVALID_TRI_INDEX)
  }

  /**
   * Tests if this tri has any adjacent tris.
   */
  fun hasAdjacent(): Boolean {
    return hasAdjacent(0) ||
        hasAdjacent(1) || hasAdjacent(2)
  }

  /**
   * Tests if there is an adjacent triangle to an edge.
   */
  fun hasAdjacent(index: Int): Boolean {
    return null != getAdjacent(index)
  }

  /**
   * Tests if a triangle is adjacent to some edge of this triangle.
   */
  fun isAdjacent(tri: Tri): Boolean {
    return getIndex(tri) >= 0
  }

  /**
   * Computes the number of triangle adjacent to this triangle.
   */
  fun numAdjacent(): Int {
    var num = 0
    if (tri0 != null)
      num++
    if (tri1 != null)
      num++
    if (tri2 != null)
      num++
    return num
  }

  /**
   * Tests if a tri vertex is interior.
   */
  fun isInteriorVertex(index: Int): Boolean {
    var curr: Tri = this
    var currIndex = index
    do {
      val adj = curr.getAdjacent(currIndex) ?: return false
      val adjIndex = adj.getIndex(curr)
      if (adjIndex < 0) {
        throw IllegalStateException("Inconsistent adjacency - invalid triangulation")
      }
      curr = adj
      currIndex = next(adjIndex)
    } while (curr !== this)
    return true
  }

  /**
   * Tests if a tri contains a boundary edge.
   */
  fun isBorder(): Boolean {
    return isBoundary(0) || isBoundary(1) || isBoundary(2)
  }

  /**
   * Tests if an edge is on the boundary of a triangulation.
   */
  fun isBoundary(index: Int): Boolean {
    return !hasAdjacent(index)
  }

  /**
   * Computes a coordinate for the midpoint of a triangle edge.
   */
  fun midpoint(edgeIndex: Int): Coordinate {
    val pt0 = getCoordinate(edgeIndex)
    val pt1 = getCoordinate(next(edgeIndex))
    val midX = (pt0.getX() + pt1.getX()) / 2
    val midY = (pt0.getY() + pt1.getY()) / 2
    return Coordinate(midX, midY)
  }

  /**
   * Gets the area of the triangle.
   */
  fun getArea(): Double {
    return Triangle.area(p0, p1, p2)
  }

  /**
   * Gets the perimeter length of the triangle.
   */
  fun getLength(): Double {
    return Triangle.length(p0, p1, p2)
  }

  /**
   * Gets the length of an edge of the triangle.
   */
  fun getLength(edgeIndex: Int): Double {
    return getCoordinate(edgeIndex).distance(getCoordinate(next(edgeIndex)))
  }

  /**
   * Creates a [Polygon] representing this triangle.
   */
  fun toPolygon(geomFact: GeometryFactory): Polygon {
    return geomFact.createPolygon(
      geomFact.createLinearRing(arrayOf(p0.copy(), p1.copy(), p2.copy(), p0.copy())), null)
  }

  override fun toString(): String {
    return "POLYGON ((${WKTWriter.format(p0)}, ${WKTWriter.format(p1)}, " +
      "${WKTWriter.format(p2)}, ${WKTWriter.format(p0)}))"
  }

  companion object {
    private const val INVALID_TRI_INDEX = "Invalid Tri index"

    /**
     * Creates a [org.locationtech.jts.geom.GeometryCollection] of [Polygon]s
     * representing the triangles in a list.
     *
     * @param tris a collection of Tris
     * @param geomFact the GeometryFactory to use
     * @return the polygons for the triangles
     */
    @JvmStatic
    fun toGeometry(tris: Collection<Tri>, geomFact: GeometryFactory): Geometry {
      val geoms = arrayOfNulls<Geometry>(tris.size)
      var i = 0
      for (tri in tris) {
        geoms[i++] = tri.toPolygon(geomFact)
      }
      @Suppress("UNCHECKED_CAST")
      return geomFact.createGeometryCollection(geoms as Array<Geometry>)
    }

    /**
     * Computes the area of a set of Tris.
     *
     * @param triList a set of Tris
     * @return the total area of the triangles
     */
    @JvmStatic
    fun area(triList: List<Tri>): Double {
      var area = 0.0
      for (tri in triList) {
        area += tri.getArea()
      }
      return area
    }

    /**
     * Validates a list of Tris.
     *
     * @param triList the tris to validate
     */
    @JvmStatic
    fun validate(triList: List<Tri>) {
      for (tri in triList) {
        tri.validate()
      }
    }

    /**
     * Creates a triangle with the given vertices.
     *
     * @return the created triangle
     */
    @JvmStatic
    fun create(p0: Coordinate, p1: Coordinate, p2: Coordinate): Tri {
      return Tri(p0, p1, p2)
    }

    /**
     * Creates a triangle from an array with three vertex coordinates.
     *
     * @param pts the array of vertex coordinates
     * @return the created triangle
     */
    @JvmStatic
    fun create(pts: Array<Coordinate>): Tri {
      return Tri(pts[0], pts[1], pts[2])
    }

    /**
     * Computes the vertex or edge index which is the next one
     * (clockwise) around the triangle.
     */
    @JvmStatic
    fun next(index: Int): Int {
      when (index) {
        0 -> return 1
        1 -> return 2
        2 -> return 0
      }
      return -1
    }

    /**
     * Computes the vertex or edge index which is the previous one
     * (counter-clockwise) around the triangle.
     */
    @JvmStatic
    fun prev(index: Int): Int {
      when (index) {
        0 -> return 2
        1 -> return 0
        2 -> return 1
      }
      return -1
    }

    /**
     * Gets the index of the vertex opposite an edge.
     */
    @JvmStatic
    fun oppVertex(edgeIndex: Int): Int {
      return prev(edgeIndex)
    }

    /**
     * Gets the index of the edge opposite a vertex.
     */
    @JvmStatic
    fun oppEdge(vertexIndex: Int): Int {
      return next(vertexIndex)
    }
  }
}
