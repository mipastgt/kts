/*
 * Copyright (c) 2019 Martin Davis.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.locationtech.jts.operation.overlayng

import org.locationtech.jts.algorithm.LineIntersector
import org.locationtech.jts.algorithm.Orientation
import org.locationtech.jts.algorithm.RobustLineIntersector
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.CoordinateArrays
import org.locationtech.jts.geom.Envelope
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryCollection
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geom.LinearRing
import org.locationtech.jts.geom.MultiLineString
import org.locationtech.jts.geom.MultiPolygon
import org.locationtech.jts.geom.Polygon
import org.locationtech.jts.geom.PrecisionModel
import org.locationtech.jts.noding.IntersectionAdder
import org.locationtech.jts.noding.MCIndexNoder
import org.locationtech.jts.noding.NodedSegmentString
import org.locationtech.jts.noding.Noder
import org.locationtech.jts.noding.SegmentString
import org.locationtech.jts.noding.ValidatingNoder
import org.locationtech.jts.noding.snapround.SnapRoundingNoder

/**
 * Builds a set of noded, unique, labelled Edges from
 * the edges of the two input geometries.
 *
 * @author mdavis
 */
class EdgeNodingBuilder(private val pm: PrecisionModel?, private val customNoder: Noder?) {

  private val inputEdges: MutableList<NodedSegmentString> = ArrayList()

  private var clipEnv: Envelope? = null
  private var clipper: RingClipper? = null
  private var limiter: LineLimiter? = null

  private val hasEdges = BooleanArray(2)

  /**
   * Gets a noder appropriate for the precision model supplied.
   */
  private fun getNoder(): Noder {
    if (customNoder != null) return customNoder
    if (OverlayUtil.isFloating(pm))
      return createFloatingPrecisionNoder(IS_NODING_VALIDATED)
    return createFixedPrecisionNoder(pm)
  }

  fun setClipEnvelope(clipEnv: Envelope) {
    this.clipEnv = clipEnv
    clipper = RingClipper(clipEnv)
    limiter = LineLimiter(clipEnv)
  }

  /**
   * Reports whether there are noded edges
   * for the given input geometry.
   *
   * @param geomIndex index of input geometry
   * @return true if there are edges for the geometry
   */
  fun hasEdgesFor(geomIndex: Int): Boolean {
    return hasEdges[geomIndex]
  }

  /**
   * Creates a set of labelled Edges
   * representing the fully noded edges of the input geometries.
   *
   * @param geom0 the first geometry
   * @param geom1 the second geometry
   * @return the noded, merged, labelled edges
   */
  fun build(geom0: Geometry?, geom1: Geometry?): MutableList<Edge> {
    add(geom0, 0)
    add(geom1, 1)
    val nodedEdges = node(inputEdges)

    /**
     * Merge the noded edges to eliminate duplicates.
     * Labels are combined.
     */
    val mergedEdges = EdgeMerger.merge(nodedEdges)
    return mergedEdges
  }

  /**
   * Nodes a set of segment strings and creates [Edge]s from the result.
   *
   * @param segStrings the segment strings to node
   * @return the created edges
   */
  private fun node(segStrings: List<NodedSegmentString>): MutableList<Edge> {
    val noder = getNoder()
    noder.computeNodes(segStrings)

    @Suppress("UNCHECKED_CAST")
    val nodedSS = noder.getNodedSubstrings() as Collection<SegmentString>
    val edges = createEdges(nodedSS)
    return edges
  }

  private fun createEdges(segStrings: Collection<SegmentString>): MutableList<Edge> {
    val edges = ArrayList<Edge>()
    for (ss in segStrings) {
      val pts = ss.getCoordinates()

      //-- don't create edges from collapsed lines
      if (Edge.isCollapsed(pts))
        continue

      val info = ss.getData() as EdgeSourceInfo
      //-- Record that a non-collapsed edge exists for the parent geometry
      hasEdges[info.getIndex()] = true
      edges.add(Edge(ss.getCoordinates(), info))
    }
    return edges
  }

  private fun add(g: Geometry?, geomIndex: Int) {
    if (g == null || g.isEmpty()) return

    if (isClippedCompletely(g.getEnvelopeInternal()))
      return

    if (g is Polygon) addPolygon(g, geomIndex)
    // LineString also handles LinearRings
    else if (g is LineString) addLine(g, geomIndex)
    else if (g is MultiLineString) addCollection(g, geomIndex)
    else if (g is MultiPolygon) addCollection(g, geomIndex)
    else if (g is GeometryCollection) addGeometryCollection(g, geomIndex, g.getDimension())
    // ignore Point geometries - they are handled elsewhere
  }

  private fun addCollection(gc: GeometryCollection, geomIndex: Int) {
    for (i in 0 until gc.getNumGeometries()) {
      val g = gc.getGeometryN(i)
      add(g, geomIndex)
    }
  }

  private fun addGeometryCollection(gc: GeometryCollection, geomIndex: Int, expectedDim: Int) {
    for (i in 0 until gc.getNumGeometries()) {
      val g = gc.getGeometryN(i)
      // check for mixed-dimension input, which is not supported
      if (g.getDimension() != expectedDim) {
        throw IllegalArgumentException("Overlay input is mixed-dimension")
      }
      add(g, geomIndex)
    }
  }

  private fun addPolygon(poly: Polygon, geomIndex: Int) {
    val shell = poly.getExteriorRing()
    addPolygonRing(shell, false, geomIndex)

    for (i in 0 until poly.getNumInteriorRing()) {
      val hole = poly.getInteriorRingN(i)

      // Holes are topologically labelled opposite to the shell, since
      // the interior of the polygon lies on their opposite side
      // (on the left, if the hole is oriented CW)
      addPolygonRing(hole, true, geomIndex)
    }
  }

  /**
   * Adds a polygon ring to the graph.
   * Empty rings are ignored.
   */
  private fun addPolygonRing(ring: LinearRing, isHole: Boolean, index: Int) {
    // don't add empty rings
    if (ring.isEmpty()) return

    if (isClippedCompletely(ring.getEnvelopeInternal()))
      return

    val pts = clip(ring)

    /**
     * Don't add edges that collapse to a point
     */
    if (pts.size < 2) {
      return
    }

    val depthDelta = computeDepthDelta(ring, isHole)
    val info = EdgeSourceInfo(index, depthDelta, isHole)
    addEdge(pts, info)
  }

  /**
   * Tests whether a geometry (represented by its envelope)
   * lies completely outside the clip extent(if any).
   *
   * @param env the geometry envelope
   * @return true if the geometry envelope is outside the clip extent.
   */
  private fun isClippedCompletely(env: Envelope): Boolean {
    if (clipEnv == null) return false
    return clipEnv!!.disjoint(env)
  }

  /**
   * If a clipper is present,
   * clip the line to the clip extent.
   * Otherwise, remove duplicate points from the ring.
   *
   * @param ring the line to clip
   * @return the points in the clipped line
   */
  private fun clip(ring: LinearRing): Array<Coordinate> {
    val pts = ring.getCoordinates()
    val env = ring.getEnvelopeInternal()

    /**
     * If no clipper or ring is completely contained then no need to clip.
     * But repeated points must be removed to ensure correct noding.
     */
    if (clipper == null || clipEnv!!.covers(env)) {
      return removeRepeatedPoints(ring)
    }

    return clipper!!.clip(pts)
  }

  /**
   * Adds a line geometry, limiting it if enabled,
   * and otherwise removing repeated points.
   *
   * @param line the line to add
   * @param geomIndex the index of the parent geometry
   */
  private fun addLine(line: LineString, geomIndex: Int) {
    // don't add empty lines
    if (line.isEmpty()) return

    if (isClippedCompletely(line.getEnvelopeInternal()))
      return

    if (isToBeLimited(line)) {
      val sections = limit(line)
      for (pts in sections) {
        addLine(pts, geomIndex)
      }
    } else {
      val ptsNoRepeat = removeRepeatedPoints(line)
      addLine(ptsNoRepeat, geomIndex)
    }
  }

  private fun addLine(pts: Array<Coordinate>, geomIndex: Int) {
    /**
     * Don't add edges that collapse to a point
     */
    if (pts.size < 2) {
      return
    }

    val info = EdgeSourceInfo(geomIndex)
    addEdge(pts, info)
  }

  private fun addEdge(pts: Array<Coordinate>, info: EdgeSourceInfo) {
    val ss = NodedSegmentString(pts, info)
    inputEdges.add(ss)
  }

  /**
   * Tests whether it is worth limiting a line.
   *
   * @param line line to test
   * @return true if the line should be limited
   */
  private fun isToBeLimited(line: LineString): Boolean {
    val pts = line.getCoordinates()
    if (limiter == null || pts.size <= MIN_LIMIT_PTS) {
      return false
    }
    val env = line.getEnvelopeInternal()
    /**
     * If line is completely contained then no need to limit
     */
    if (clipEnv!!.covers(env)) {
      return false
    }
    return true
  }

  /**
   * If limiter is provided,
   * limit the line to the clip envelope.
   *
   * @param line the line to clip
   * @return the point sections in the clipped line
   */
  private fun limit(line: LineString): List<Array<Coordinate>> {
    val pts = line.getCoordinates()
    return limiter!!.limit(pts)
  }

  companion object {
    /**
     * Limiting is skipped for Lines with few vertices,
     * to avoid additional copying.
     */
    private const val MIN_LIMIT_PTS = 20

    /**
     * Indicates whether floating precision noder output is validated.
     */
    private const val IS_NODING_VALIDATED = true

    private fun createFixedPrecisionNoder(pm: PrecisionModel?): Noder {
      val noder = SnapRoundingNoder(pm!!)
      return noder
    }

    private fun createFloatingPrecisionNoder(doValidation: Boolean): Noder {
      val mcNoder = MCIndexNoder()
      val li: LineIntersector = RobustLineIntersector()
      mcNoder.setSegmentIntersector(IntersectionAdder(li))

      var noder: Noder = mcNoder
      if (doValidation) {
        noder = ValidatingNoder(mcNoder)
      }
      return noder
    }

    /**
     * Removes any repeated points from a linear component.
     *
     * @param line the line to process
     * @return the points of the line with repeated points removed
     */
    private fun removeRepeatedPoints(line: LineString): Array<Coordinate> {
      val pts = line.getCoordinates()
      return CoordinateArrays.removeRepeatedPoints(pts)
    }

    private fun computeDepthDelta(ring: LinearRing, isHole: Boolean): Int {
      /**
       * Compute the orientation of the ring, to
       * allow assigning side interior/exterior labels correctly.
       */
      val isCCW = Orientation.isCCW(ring.getCoordinateSequence())
      /**
       * Compute whether ring is in canonical orientation or not.
       * Canonical orientation for the overlay process is
       * Shells : CW, Holes: CCW
       */
      var isOriented = true
      if (!isHole)
        isOriented = !isCCW
      else {
        isOriented = isCCW
      }
      /**
       * Depth delta can now be computed.
       */
      val depthDelta = if (isOriented) 1 else -1
      return depthDelta
    }
  }
}
