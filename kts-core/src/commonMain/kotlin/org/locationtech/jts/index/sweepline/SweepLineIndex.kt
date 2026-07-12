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
package org.locationtech.jts.index.sweepline

/**
 * A sweepline implements a sorted index on a set of intervals.
 * It is used to compute all overlaps between the interval in the index.
 *
 * @version 1.7
 */
class SweepLineIndex {

  private val events: MutableList<SweepLineEvent> = ArrayList()
  private var indexBuilt = false
  // statistics information
  private var nOverlaps = 0

  fun add(sweepInt: SweepLineInterval) {
    val insertEvent = SweepLineEvent(sweepInt.getMin(), null, sweepInt)
    events.add(insertEvent)
    events.add(SweepLineEvent(sweepInt.getMax(), insertEvent, sweepInt))
  }

  /**
   * Because Delete Events have a link to their corresponding Insert event,
   * it is possible to compute exactly the range of events which must be
   * compared to a given Insert event object.
   */
  private fun buildIndex() {
    if (indexBuilt) return
    events.sort()
    for (i in events.indices) {
      val ev = events[i]
      if (ev.isDelete()) {
        ev.getInsertEvent()!!.setDeleteEventIndex(i)
      }
    }
    indexBuilt = true
  }

  fun computeOverlaps(action: SweepLineOverlapAction) {
    nOverlaps = 0
    buildIndex()

    for (i in events.indices) {
      val ev = events[i]
      if (ev.isInsert()) {
        processOverlaps(i, ev.getDeleteEventIndex(), ev.getInterval(), action)
      }
    }
  }

  private fun processOverlaps(start: Int, end: Int, s0: SweepLineInterval, action: SweepLineOverlapAction) {
    /**
     * Since we might need to test for self-intersections,
     * include current insert event object in list of event objects to test.
     * Last index can be skipped, because it must be a Delete event.
     */
    for (i in start until end) {
      val ev = events[i]
      if (ev.isInsert()) {
        val s1 = ev.getInterval()
        action.overlap(s0, s1)
        nOverlaps++
      }
    }
  }
}
