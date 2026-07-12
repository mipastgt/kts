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

import org.locationtech.jts.algorithm.LineIntersector
import org.locationtech.jts.algorithm.RobustLineIntersector
import org.locationtech.jts.geom.PrecisionModel
import org.locationtech.jts.geom.TopologyException

/**
 * Nodes a set of [NodedSegmentString]s completely.
 * The set of segment strings is fully noded;
 * i.e. noding is repeated until no further
 * intersections are detected.
 *
 *
 * Iterated noding using a FLOATING precision model is not guaranteed to converge,
 * due to roundoff error.
 * This problem is detected and an exception is thrown.
 * Clients can choose to rerun the noding using a lower precision model.
 *
 * @version 1.7
 */
class IteratedNoder(pm: PrecisionModel) : Noder {

  private val pm: PrecisionModel = pm
  private val li: LineIntersector
  private var nodedSegStrings: Collection<*>? = null
  private var maxIter = MAX_ITER

  init {
    li = RobustLineIntersector()
    li.setPrecisionModel(pm)
  }

  /**
   * Sets the maximum number of noding iterations performed before
   * the noding is aborted.
   * Experience suggests that this should rarely need to be changed
   * from the default.
   * The default is MAX_ITER.
   *
   * @param maxIter the maximum number of iterations to perform
   */
  fun setMaximumIterations(maxIter: Int) {
    this.maxIter = maxIter
  }

  @Suppress("UNCHECKED_CAST")
  override fun getNodedSubstrings(): MutableCollection<*>? = nodedSegStrings as MutableCollection<*>?

  /**
   * Fully nodes a list of [SegmentString]s, i.e. performs noding iteratively
   * until no intersections are found between segments.
   * Maintains labelling of edges correctly through
   * the noding.
   *
   * @param segStrings a collection of SegmentStrings to be noded
   * @throws TopologyException if the iterated noding fails to converge.
   */
  override fun computeNodes(segStrings: Collection<*>?) {
    val numInteriorIntersections = IntArray(1)
    nodedSegStrings = segStrings
    var nodingIterationCount = 0
    var lastNodesCreated = -1
    do {
      node(nodedSegStrings, numInteriorIntersections)
      nodingIterationCount++
      val nodesCreated = numInteriorIntersections[0]

      /**
       * Fail if the number of nodes created is not declining.
       * However, allow a few iterations at least before doing this
       */
      if (lastNodesCreated > 0
        && nodesCreated >= lastNodesCreated
        && nodingIterationCount > maxIter
      ) {
        throw TopologyException(
          "Iterated noding failed to converge after "
            + nodingIterationCount + " iterations"
        )
      }
      lastNodesCreated = nodesCreated
    } while (lastNodesCreated > 0)
  }

  /**
   * Node the input segment strings once
   * and create the split edges between the nodes
   */
  private fun node(segStrings: Collection<*>?, numInteriorIntersections: IntArray) {
    val si = IntersectionAdder(li)
    val noder = MCIndexNoder()
    noder.setSegmentIntersector(si)
    noder.computeNodes(segStrings)
    nodedSegStrings = noder.getNodedSubstrings()
    numInteriorIntersections[0] = si.numInteriorIntersections
  }

  companion object {
    const val MAX_ITER = 5
  }
}
