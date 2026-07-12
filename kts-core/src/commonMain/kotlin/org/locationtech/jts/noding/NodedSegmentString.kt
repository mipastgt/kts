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
package org.locationtech.jts.noding

import kotlin.jvm.JvmStatic

import org.locationtech.jts.algorithm.LineIntersector
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.impl.CoordinateArraySequence
import org.locationtech.jts.io.WKTWriter

/**
 * Represents a list of contiguous line segments,
 * and supports noding the segments.
 * The line segments are represented by an array of [Coordinate]s.
 * Intended to optimize the noding of contiguous segments by
 * reducing the number of allocated objects.
 * [SegmentString]s can carry a context object, which is useful
 * for preserving topological or parentage information.
 * All noded substrings are initialized with the same context object.
 *
 *
 * For read-only applications use [BasicSegmentString],
 * which is (slightly) more lightweight.
 *
 * @version 1.7
 * @see BasicSegmentString
 */
class NodedSegmentString : NodableSegmentString {

  private val nodeList: SegmentNodeList = SegmentNodeList(this)
  private val pts: Array<Coordinate>
  private var data: Any?

  /**
   * Creates a instance from a list of vertices and optional data object.
   *
   * @param pts the vertices of the segment string
   * @param data the user-defined data of this segment string (may be null)
   */
  constructor(pts: Array<Coordinate>, data: Any?) {
    this.pts = pts
    this.data = data
  }

  /**
   * Creates a new instance from a [SegmentString].
   *
   * @param ss the segment string to use
   */
  constructor(ss: SegmentString) {
    this.pts = ss.getCoordinates()
    this.data = ss.getData()
  }

  /**
   * Gets the user-defined data for this segment string.
   *
   * @return the user-defined data
   */
  override fun getData(): Any? = data

  /**
   * Sets the user-defined data for this segment string.
   *
   * @param data an Object containing user-defined data
   */
  override fun setData(data: Any?) {
    this.data = data
  }

  fun getNodeList(): SegmentNodeList = nodeList
  override fun size(): Int = pts.size
  override fun getCoordinate(i: Int): Coordinate = pts[i]
  override fun getCoordinates(): Array<Coordinate> = pts

  /**
   * Gets a list of coordinates with all nodes included.
   *
   * @return an array of coordinates include nodes
   */
  fun getNodedCoordinates(): Array<Coordinate> {
    return nodeList.getSplitCoordinates()
  }

  override fun isClosed(): Boolean {
    return pts[0] == pts[pts.size - 1]
  }

  /**
   * Tests whether any nodes have been added.
   *
   * @return true if the segment string has nodes
   */
  fun hasNodes(): Boolean {
    return nodeList.size() > 0
  }

  /**
   * Gets the octant of the segment starting at vertex `index`.
   *
   * @param index the index of the vertex starting the segment.  Must not be
   * the last index in the vertex list
   * @return the octant of the segment at the vertex
   */
  fun getSegmentOctant(index: Int): Int {
    if (index == pts.size - 1) return -1
    return safeOctant(getCoordinate(index), getCoordinate(index + 1))
    //    return Octant.octant(getCoordinate(index), getCoordinate(index + 1));
  }

  private fun safeOctant(p0: Coordinate, p1: Coordinate): Int {
    if (p0.equals2D(p1)) return 0
    return Octant.octant(p0, p1)
  }

  /**
   * Adds EdgeIntersections for one or both
   * intersections found for a segment of an edge to the edge intersection list.
   */
  fun addIntersections(li: LineIntersector, segmentIndex: Int, geomIndex: Int) {
    for (i in 0 until li.getIntersectionNum()) {
      addIntersection(li, segmentIndex, geomIndex, i)
    }
  }

  /**
   * Add an SegmentNode for intersection intIndex.
   * An intersection that falls exactly on a vertex
   * of the SegmentString is normalized
   * to use the higher of the two possible segmentIndexes
   */
  fun addIntersection(li: LineIntersector, segmentIndex: Int, geomIndex: Int, intIndex: Int) {
    val intPt = li.getIntersection(intIndex).copy()
    addIntersection(intPt, segmentIndex)
  }

  /**
   * Adds an intersection node for a given point and segment to this segment string.
   *
   * @param intPt the location of the intersection
   * @param segmentIndex the index of the segment containing the intersection
   */
  override fun addIntersection(intPt: Coordinate, segmentIndex: Int) {
    addIntersectionNode(intPt, segmentIndex)
  }

  /**
   * Adds an intersection node for a given point and segment to this segment string.
   * If an intersection already exists for this exact location, the existing
   * node will be returned.
   *
   * @param intPt the location of the intersection
   * @param segmentIndex the index of the segment containing the intersection
   * @return the intersection node for the point
   */
  fun addIntersectionNode(intPt: Coordinate, segmentIndex: Int): SegmentNode {
    var normalizedSegmentIndex = segmentIndex
    //Debug.println("edge intpt: " + intPt + " dist: " + dist);
    // normalize the intersection point location
    val nextSegIndex = normalizedSegmentIndex + 1
    if (nextSegIndex < pts.size) {
      val nextPt = pts[nextSegIndex]
      //Debug.println("next pt: " + nextPt);

      // Normalize segment index if intPt falls on vertex
      // The check for point equality is 2D only - Z values are ignored
      if (intPt.equals2D(nextPt)) {
        //Debug.println("normalized distance");
        normalizedSegmentIndex = nextSegIndex
      }
    }
    /*
      Add the intersection point to edge intersection list.
     */
    val ei = nodeList.add(intPt, normalizedSegmentIndex)
    return ei
  }

  override fun toString(): String {
    return WKTWriter.toLineString(CoordinateArraySequence(pts))
  }

  companion object {
    /**
     * Gets the [SegmentString]s which result from splitting this string at node points.
     *
     * @param segStrings a Collection of NodedSegmentStrings
     * @return a Collection of NodedSegmentStrings representing the substrings
     */
    @JvmStatic
    fun getNodedSubstrings(segStrings: Collection<*>?): MutableList<NodedSegmentString> {
      val resultEdgelist = ArrayList<NodedSegmentString>()
      getNodedSubstrings(segStrings, resultEdgelist)
      return resultEdgelist
    }

    /**
     * Adds the noded [SegmentString]s which result from splitting this string at node points.
     *
     * @param segStrings a Collection of NodedSegmentStrings
     * @param resultEdgelist a List which will collect the NodedSegmentStrings representing the substrings
     */
    @JvmStatic
    fun getNodedSubstrings(segStrings: Collection<*>?, resultEdgelist: MutableCollection<in NodedSegmentString>) {
      for (segString in segStrings!!) {
        val ss = segString as NodedSegmentString
        ss.getNodeList().addSplitEdges(resultEdgelist)
      }
    }
  }
}
