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
package org.locationtech.jts.coverage

import org.locationtech.jts.algorithm.Orientation
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.CoordinateArrays
import org.locationtech.jts.geom.Envelope
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geom.LinearRing
import org.locationtech.jts.geom.Polygon
import org.locationtech.jts.geom.util.PolygonExtracter
import org.locationtech.jts.noding.BasicSegmentString

internal class CoverageRing private constructor(pts: Array<Coordinate>, private val interiorOnRight: Boolean) : BasicSegmentString(pts, null) {

  private val invalid: BooleanArray = BooleanArray(size() - 1)
  private val matched: BooleanArray = BooleanArray(size() - 1)

  fun getEnvelope(start: Int, end: Int): Envelope {
    val env = Envelope()
    for (i in start until end) {
      env.expandToInclude(getCoordinate(i))
    }
    return env
  }

  /**
   * Reports if the ring has canonical orientation,
   * with the polygon interior on the right (shell is CW).
   *
   * @return true if the polygon interior is on the right
   */
  fun isInteriorOnRight(): Boolean {
    return interiorOnRight
  }

  /**
   * Marks a segment as invalid.
   *
   * @param i the segment index
   */
  fun markInvalid(i: Int) {
    invalid[i] = true
  }

  /**
   * Marks a segment as valid.
   *
   * @param i the segment index
   */
  fun markMatched(i: Int) {
    matched[i] = true
  }

  /**
   * Tests if all segments in the ring have known status
   * (matched or invalid).
   *
   * @return true if all segments have known status
   */
  fun isKnown(): Boolean {
    for (i in matched.indices) {
      if (!(matched[i] && invalid[i]))
        return false
    }
    return true
  }

  /**
   * Tests if a segment is marked invalid.
   *
   * @param index the segment index
   * @return true if the segment is invalid
   */
  fun isInvalid(index: Int): Boolean {
    return invalid[index]
  }

  /**
   * Tests whether all segments are invalid.
   *
   * @return true if all segments are invalid
   */
  fun isInvalid(): Boolean {
    for (i in invalid.indices) {
      if (!invalid[i])
        return false
    }
    return true
  }

  /**
   * Tests whether any segment is invalid.
   *
   * @return true if some segment is invalid
   */
  fun hasInvalid(): Boolean {
    for (i in invalid.indices) {
      if (invalid[i])
        return true
    }
    return false
  }

  /**
   * Tests whether the matched/invalid state of a ring segment is known.
   *
   * @param i the index of the ring segment
   * @return true if the segment state is known
   */
  fun isKnown(i: Int): Boolean {
    return matched[i] || invalid[i]
  }

  /**
   * Finds the previous vertex in the ring which is distinct from a given coordinate value.
   *
   * @param index the index to start the search
   * @param pt a coordinate value (which may not be a ring vertex)
   * @return the previous distinct vertex in the ring
   */
  fun findVertexPrev(index: Int, pt: Coordinate): Coordinate {
    var iPrev = index
    var prev = getCoordinate(iPrev)
    while (pt.equals2D(prev)) {
      iPrev = prev(iPrev)
      prev = getCoordinate(iPrev)
    }
    return prev
  }

  /**
   * Finds the next vertex in the ring which is distinct from a given coordinate value.
   *
   * @param index the index to start the search
   * @param pt a coordinate value (which may not be a ring vertex)
   * @return the next distinct vertex in the ring
   */
  fun findVertexNext(index: Int, pt: Coordinate): Coordinate {
    //-- safe, since index is always the start of a segment
    var iNext = index + 1
    var next = getCoordinate(iNext)
    while (pt.equals2D(next)) {
      iNext = next(iNext)
      next = getCoordinate(iNext)
    }
    return next
  }

  /**
   * Gets the index of the previous segment in the ring.
   *
   * @param index a segment index
   * @return the index of the previous segment
   */
  fun prev(index: Int): Int {
    if (index == 0)
      return size() - 2
    return index - 1
  }

  /**
   * Gets the index of the next segment in the ring.
   *
   * @param index a segment index
   * @return the index of the next segment
   */
  fun next(index: Int): Int {
    if (index < size() - 2)
      return index + 1
    return 0
  }

  fun createInvalidLines(geomFactory: GeometryFactory, lines: MutableList<LineString>) {
    //-- empty case
    if (!hasInvalid()) {
      return
    }
    //-- entire ring case
    if (isInvalid()) {
      val line = createLine(0, size() - 1, geomFactory)
      lines.add(line)
      return
    }

    //-- find first end after index 0, to allow wrap-around
    var startIndex = findInvalidStart(0)
    val firstEndIndex = findInvalidEnd(startIndex)
    var endIndex = firstEndIndex
    while (true) {
      startIndex = findInvalidStart(endIndex)
      endIndex = findInvalidEnd(startIndex)
      val line = createLine(startIndex, endIndex, geomFactory)
      lines.add(line)
      if (endIndex == firstEndIndex)
        break
    }
  }

  private fun findInvalidStart(index: Int): Int {
    var idx = index
    while (!isInvalid(idx)) {
      idx = nextMarkIndex(idx)
    }
    return idx
  }

  private fun findInvalidEnd(index: Int): Int {
    var idx = nextMarkIndex(index)
    while (isInvalid(idx)) {
      idx = nextMarkIndex(idx)
    }
    return idx
  }

  private fun nextMarkIndex(index: Int): Int {
    if (index >= invalid.size - 1) {
      return 0
    }
    return index + 1
  }

  /**
   * Creates a line from a sequence of ring segments between startIndex and endIndex (inclusive).
   *
   * @return a line representing the section
   */
  private fun createLine(startIndex: Int, endIndex: Int, geomFactory: GeometryFactory): LineString {
    val pts = if (endIndex < startIndex)
      extractSectionWrap(startIndex, endIndex)
    else
      extractSection(startIndex, endIndex)
    return geomFactory.createLineString(pts)
  }

  private fun extractSection(startIndex: Int, endIndex: Int): Array<Coordinate> {
    val size = endIndex - startIndex + 1
    val pts = arrayOfNulls<Coordinate>(size)
    var ipts = 0
    for (i in startIndex..endIndex) {
      pts[ipts++] = getCoordinate(i).copy()
    }
    @Suppress("UNCHECKED_CAST")
    return pts as Array<Coordinate>
  }

  private fun extractSectionWrap(startIndex: Int, endIndex: Int): Array<Coordinate> {
    val size = endIndex + (size() - startIndex)
    val pts = arrayOfNulls<Coordinate>(size)
    var index = startIndex
    for (i in 0 until size) {
      pts[i] = getCoordinate(index).copy()
      index = nextMarkIndex(index)
    }
    @Suppress("UNCHECKED_CAST")
    return pts as Array<Coordinate>
  }

  companion object {
    fun createRings(geom: Geometry): List<CoverageRing> {
      @Suppress("UNCHECKED_CAST")
      val polygons = PolygonExtracter.getPolygons(geom) as List<Polygon>
      return createRings(polygons)
    }

    fun createRings(polygons: List<Polygon>): List<CoverageRing> {
      val rings: MutableList<CoverageRing> = ArrayList()
      for (poly in polygons) {
        createRings(poly, rings)
      }
      return rings
    }

    private fun createRings(poly: Polygon, rings: MutableList<CoverageRing>) {
      if (poly.isEmpty())
        return
      addRing(poly.getExteriorRing(), true, rings)
      for (i in 0 until poly.getNumInteriorRing()) {
        addRing(poly.getInteriorRingN(i), false, rings)
      }
    }

    private fun addRing(ring: LinearRing, isShell: Boolean, rings: MutableList<CoverageRing>) {
      if (ring.isEmpty())
        return
      rings.add(createRing(ring, isShell))
    }

    private fun createRing(ring: LinearRing, isShell: Boolean): CoverageRing {
      var pts = ring.getCoordinates()
      if (CoordinateArrays.hasRepeatedOrInvalidPoints(pts)) {
        pts = CoordinateArrays.removeRepeatedOrInvalidPoints(pts)
      }
      val isCCW = Orientation.isCCW(pts)
      val isInteriorOnRight = if (isShell) !isCCW else isCCW
      return CoverageRing(pts, isInteriorOnRight)
    }

    /**
     * Tests if all rings have known status (matched or invalid)
     * for all segments.
     *
     * @param rings a list of rings
     * @return true if all ring segments have known status
     */
    fun isKnown(rings: List<CoverageRing>): Boolean {
      for (ring in rings) {
        if (!ring.isKnown())
          return false
      }
      return true
    }
  }
}
