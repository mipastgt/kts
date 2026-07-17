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
import org.locationtech.jts.algorithm.RobustLineIntersector
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.TopologyException
import org.locationtech.jts.io.WKTWriter

/**
 * Validates that a collection of [SegmentString]s is correctly noded.
 * Indexing is used to improve performance.
 * By default validation stops after a single
 * non-noded intersection is detected.
 * Alternatively, it can be requested to detect all intersections
 * by using [setFindAllIntersections].
 *
 *
 * @see NodingIntersectionFinder
 */
class FastNodingValidator
/**
 * Creates a new noding validator for a given set of linework.
 *
 * @param segStrings a collection of [SegmentString]s
 */
  (private val segStrings: Collection<*>) {

  private val li: LineIntersector = RobustLineIntersector()

  private var findAllIntersections = false
  private var segInt: NodingIntersectionFinder? = null
  private var valid = true

  fun setFindAllIntersections(findAllIntersections: Boolean) {
    this.findAllIntersections = findAllIntersections
  }

  /**
   * Gets a list of all intersections found.
   * Intersections are represented as [Coordinate]s.
   * List is empty if none were found.
   *
   * @return a list of Coordinate
   */
  fun getIntersections(): MutableList<Coordinate> {
    return segInt!!.getIntersections()
  }

  /**
   * Checks for an intersection and
   * reports if one is found.
   *
   * @return true if the arrangement contains an interior intersection
   */
  fun isValid(): Boolean {
    execute()
    return valid
  }

  /**
   * Returns an error message indicating the segments containing
   * the intersection.
   *
   * @return an error message documenting the intersection location
   */
  fun getErrorMessage(): String {
    if (valid) return "no intersections found"

    val intSegs = segInt!!.getIntersectionSegments()!!
    return ("found non-noded intersection between "
      + WKTWriter.toLineString(intSegs[0], intSegs[1])
      + " and "
      + WKTWriter.toLineString(intSegs[2], intSegs[3]))
  }

  /**
   * Checks for an intersection and throws
   * a TopologyException if one is found.
   *
   * @throws TopologyException if an intersection is found
   */
  fun checkValid() {
    execute()
    if (!valid)
      throw TopologyException(getErrorMessage(), segInt!!.getIntersection()!!)
  }

  private fun execute() {
    if (segInt != null)
      return
    checkInteriorIntersections()
  }

  private fun checkInteriorIntersections() {
    /*
     * MD - It may even be reliable to simply check whether
     * end segments (of SegmentStrings) have an interior intersection,
     * since noding should have split any true interior intersections already.
     */
    valid = true
    val segInt = NodingIntersectionFinder(li)
    this.segInt = segInt
    segInt.setFindAllIntersections(findAllIntersections)
    val noder = MCIndexNoder()
    noder.setSegmentIntersector(segInt)
    noder.computeNodes(segStrings)
    if (segInt.hasIntersection()) {
      valid = false
      return
    }
  }

  companion object {
    /**
     * Gets a list of all intersections found.
     * Intersections are represented as [Coordinate]s.
     * List is empty if none were found.
     *
     * @param segStrings a collection of SegmentStrings
     * @return a list of Coordinate
     */
    @JvmStatic
    fun computeIntersections(segStrings: Collection<*>): MutableList<Coordinate> {
      val nv = FastNodingValidator(segStrings)
      nv.setFindAllIntersections(true)
      nv.isValid()
      return nv.getIntersections()
    }
  }
}
