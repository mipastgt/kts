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


import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Triangle
import org.locationtech.jts.triangulate.tri.Tri

/**
 * Tris which are used to form a concave hull.
 * If a Tri has an edge (or edges) with no adjacent tri
 * the tri is on the border of the triangulation.
 * The edge is a boundary edge.
 * The union of those edges
 * forms the (linear) boundary of the triangulation.
 * The triangulation area may be a Polygon or MultiPolygon, and may or may not contain holes.
 *
 * @author Martin Davis
 */
internal class HullTri(p0: Coordinate, p1: Coordinate, p2: Coordinate) :
    Tri(p0, p1, p2), Comparable<HullTri> {

  private var size: Double = lengthOfLongestEdge()
  private var marked = false

  fun getSize(): Double {
    return size
  }

  /**
   * Sets the size to be the length of the boundary edges.
   * This is used when constructing hull without holes,
   * by erosion from the triangulation border.
   */
  fun setSizeToBoundary() {
    size = lengthOfBoundary()
  }

  fun setSizeToLongestEdge() {
    size = lengthOfLongestEdge()
  }

  fun setSizeToCircumradius() {
    size = Triangle.circumradius(p2, p1, p0)
  }

  fun isMarked(): Boolean {
    return marked
  }

  fun setMarked(isMarked: Boolean) {
    this.marked = isMarked
  }

  fun isRemoved(): Boolean {
    return !hasAdjacent()
  }

  /**
   * Gets an index of a boundary edge, if there is one.
   *
   * @return a boundary edge index, or -1
   */
  fun boundaryIndex(): Int {
    if (isBoundary(0)) return 0
    if (isBoundary(1)) return 1
    if (isBoundary(2)) return 2
    return -1
  }

  /**
   * Gets the most CCW boundary edge index.
   * This assumes there is at least one non-boundary edge.
   *
   * @return the CCW boundary edge index
   */
  fun boundaryIndexCCW(): Int {
    val index = boundaryIndex()
    if (index < 0) return -1
    val prevIndex = Tri.prev(index)
    if (isBoundary(prevIndex)) {
      return prevIndex
    }
    return index
  }

  /**
   * Gets the most CW boundary edge index.
   * This assumes there is at least one non-boundary edge.
   *
   * @return the CW boundary edge index
   */
  fun boundaryIndexCW(): Int {
    val index = boundaryIndex()
    if (index < 0) return -1
    val nextIndex = Tri.next(index)
    if (isBoundary(nextIndex)) {
      return nextIndex
    }
    return index
  }

  /**
   * Tests if a tri is the only one connecting its 2 adjacents.
   * Assumes that the tri is on the border of the triangulation
   * and that the triangulation does not contain holes
   *
   * @return true if the tri is the only connection
   */
  fun isConnecting(): Boolean {
    val adj2Index = adjacent2VertexIndex()
    val isInterior = isInteriorVertex(adj2Index)
    return !isInterior
  }

  /**
   * Gets the index of a vertex which is adjacent to two other tris (if any).
   *
   * @return the vertex index, or -1
   */
  fun adjacent2VertexIndex(): Int {
    if (hasAdjacent(0) && hasAdjacent(1)) return 1
    if (hasAdjacent(1) && hasAdjacent(2)) return 2
    if (hasAdjacent(2) && hasAdjacent(0)) return 0
    return -1
  }

  /**
   * Tests whether some vertex of this Tri has degree = 1.
   * In this case it is not in any other Tris.
   *
   * @param triList the triangulation
   * @return true if a vertex has degree 1
   */
  fun isolatedVertexIndex(triList: List<HullTri>): Int {
    for (i in 0 until 3) {
      if (degree(i, triList) <= 1)
        return i
    }
    return -1
  }

  fun lengthOfLongestEdge(): Double {
    return Triangle.longestSideLength(p0, p1, p2)
  }

  fun lengthOfBoundary(): Double {
    var len = 0.0
    for (i in 0 until 3) {
      if (!hasAdjacent(i)) {
        len += getCoordinate(i).distance(getCoordinate(Tri.next(i)))
      }
    }
    return len
  }

  /**
   * Sorts tris in decreasing order.
   * Since PriorityQueues sort in *ascending* order,
   * to sort with the largest at the head,
   * smaller sizes must compare as greater than larger sizes.
   */
  override fun compareTo(o: HullTri): Int {
    /*
     * If size is identical compare areas to ensure a (more) deterministic ordering.
     * Larger areas sort before smaller ones.
     */
    if (size == o.size) {
      return -getArea().compareTo(o.getArea())
    }
    return -size.compareTo(o.size)
  }

  /**
   * Tests if this tri has a vertex which is in the boundary,
   * but not in a boundary edge.
   *
   * @return true if the tri touches the boundary at a vertex
   */
  fun hasBoundaryTouch(): Boolean {
    for (i in 0 until 3) {
      if (isBoundaryTouch(i))
        return true
    }
    return false
  }

  private fun isBoundaryTouch(index: Int): Boolean {
    //-- If vertex is in a boundary edge it is not a touch
    if (isBoundary(index)) return false
    if (isBoundary(Tri.prev(index))) return false
    //-- if vertex is not in interior it is on boundary
    return !isInteriorVertex(index)
  }

  companion object {
    fun findTri(triList: List<HullTri>, exceptTri: Tri?): HullTri? {
      for (tri in triList) {
        if (tri !== exceptTri) return tri
      }
      return null
    }

    fun isAllMarked(triList: List<HullTri>): Boolean {
      for (tri in triList) {
        if (!tri.isMarked())
          return false
      }
      return true
    }

    fun clearMarks(triList: List<HullTri>) {
      for (tri in triList) {
        tri.setMarked(false)
      }
    }

    fun markConnected(triStart: HullTri, exceptTri: Tri) {
      val queue: ArrayDeque<HullTri> = ArrayDeque<HullTri>()
      queue.add(triStart)
      while (!queue.isEmpty()) {
        val tri = queue.removeFirst()
        tri.setMarked(true)
        for (i in 0 until 3) {
          val adj = tri.getAdjacent(i) as HullTri?
          //-- don't connect thru this tri
          if (adj === exceptTri)
            continue
          if (adj != null && !adj.isMarked()) {
            queue.add(adj)
          }
        }
      }
    }

    /**
     * Tests if a triangulation is edge-connected, if a triangle is removed.
     * NOTE: this is a relatively slow operation.
     *
     * @param triList the triangulation
     * @param removedTri the triangle to remove
     * @return true if the triangulation is still connnected
     */
    fun isConnected(triList: List<HullTri>, removedTri: HullTri): Boolean {
      if (triList.size == 0) return false
      clearMarks(triList)
      val triStart = findTri(triList, removedTri) ?: return false
      markConnected(triStart, removedTri)
      removedTri.setMarked(true)
      return isAllMarked(triList)
    }
  }
}
