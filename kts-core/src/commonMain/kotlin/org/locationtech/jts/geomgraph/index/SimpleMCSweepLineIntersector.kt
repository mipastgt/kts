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
package org.locationtech.jts.geomgraph.index

import org.locationtech.jts.geomgraph.Edge

/**
 * Finds all intersections in one or two sets of edges,
 * using an x-axis sweepline algorithm in conjunction with Monotone Chains.
 * While still O(n^2) in the worst case, this algorithm
 * drastically improves the average-case time.
 * The use of MonotoneChains as the items in the index
 * seems to offer an improvement in performance over a sweep-line alone.
 *
 * @version 1.7
 */
class SimpleMCSweepLineIntersector : EdgeSetIntersector() {

  private val events: MutableList<SweepLineEvent> = ArrayList()
  // statistics information
  private var nOverlaps = 0

  override fun computeIntersections(edges: List<*>, si: SegmentIntersector, testAllSegments: Boolean) {
    if (testAllSegments)
      addEdges(edges, null)
    else
      addEdges(edges)
    computeIntersections(si)
  }

  override fun computeIntersections(edges0: List<*>, edges1: List<*>, si: SegmentIntersector) {
    addEdges(edges0, edges0)
    addEdges(edges1, edges1)
    computeIntersections(si)
  }

  private fun addEdges(edges: List<*>) {
    val i = edges.iterator()
    while (i.hasNext()) {
      val edge = i.next() as Edge
      // edge is its own group
      addEdge(edge, edge)
    }
  }

  private fun addEdges(edges: List<*>, edgeSet: Any?) {
    val i = edges.iterator()
    while (i.hasNext()) {
      val edge = i.next() as Edge
      addEdge(edge, edgeSet)
    }
  }

  private fun addEdge(edge: Edge, edgeSet: Any?) {
    val mce = edge.getMonotoneChainEdge()
    val startIndex = mce.getStartIndexes()
    for (i in 0 until startIndex.size - 1) {
      val mc = MonotoneChain(mce, i)
      val insertEvent = SweepLineEvent(edgeSet, mce.getMinX(i), mc)
      events.add(insertEvent)
      events.add(SweepLineEvent(mce.getMaxX(i), insertEvent))
    }
  }

  /**
   * Because Delete Events have a link to their corresponding Insert event,
   * it is possible to compute exactly the range of events which must be
   * compared to a given Insert event object.
   */
  private fun prepareEvents() {
    events.sort()
    // set DELETE event indexes
    for (i in events.indices) {
      val ev = events[i]
      if (ev.isDelete()) {
        ev.getInsertEvent()!!.setDeleteEventIndex(i)
      }
    }
  }

  private fun computeIntersections(si: SegmentIntersector) {
    nOverlaps = 0
    prepareEvents()

    for (i in events.indices) {
      val ev = events[i]
      if (ev.isInsert()) {
        processOverlaps(i, ev.getDeleteEventIndex(), ev, si)
      }
      if (si.isDone()) {
        break
      }
    }
  }

  private fun processOverlaps(start: Int, end: Int, ev0: SweepLineEvent, si: SegmentIntersector) {
    val mc0 = ev0.getObject() as MonotoneChain
    /**
     * Since we might need to test for self-intersections,
     * include current INSERT event object in list of event objects to test.
     * Last index can be skipped, because it must be a Delete event.
     */
    for (i in start until end) {
      val ev1 = events[i]
      if (ev1.isInsert()) {
        val mc1 = ev1.getObject() as MonotoneChain
        // don't compare edges in same group, if labels are present
        if (!ev0.isSameLabel(ev1)) {
          mc0.computeIntersections(mc1, si)
          nOverlaps++
        }
      }
    }
  }
}
