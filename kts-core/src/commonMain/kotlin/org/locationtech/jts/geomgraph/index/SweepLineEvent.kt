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

class SweepLineEvent : Comparable<Any?> {

  private var label: Any? = null    // used for red-blue intersection detection
  private var xValue = 0.0
  private var eventType = 0
  private var insertEvent: SweepLineEvent? = null // null if this is an INSERT event
  private var deleteEventIndex = 0
  private var obj: Any? = null

  /**
   * Creates an INSERT event.
   *
   * @param label the edge set label for this object
   * @param x the event location
   * @param obj the object being inserted
   */
  constructor(label: Any?, x: Double, obj: Any?) {
    this.eventType = INSERT
    this.label = label
    xValue = x
    this.obj = obj
  }

  /**
   * Creates a DELETE event.
   *
   * @param x the event location
   * @param insertEvent the corresponding INSERT event
   */
  constructor(x: Double, insertEvent: SweepLineEvent?) {
    eventType = DELETE
    xValue = x
    this.insertEvent = insertEvent
  }

  fun isInsert(): Boolean = eventType == INSERT
  fun isDelete(): Boolean = eventType == DELETE
  fun getInsertEvent(): SweepLineEvent? = insertEvent
  fun getDeleteEventIndex(): Int = deleteEventIndex
  fun setDeleteEventIndex(deleteEventIndex: Int) { this.deleteEventIndex = deleteEventIndex }

  fun getObject(): Any? = obj

  fun isSameLabel(ev: SweepLineEvent): Boolean {
    // no label set indicates single group
    if (label == null) return false
    return label === ev.label
  }

  /**
   * Events are ordered first by their x-value, and then by their eventType.
   * Insert events are sorted before Delete events, so that
   * items whose Insert and Delete events occur at the same x-value will be
   * correctly handled.
   */
  override fun compareTo(o: Any?): Int {
    val pe = o as SweepLineEvent
    if (xValue < pe.xValue) return -1
    if (xValue > pe.xValue) return 1
    if (eventType < pe.eventType) return -1
    if (eventType > pe.eventType) return 1
    return 0
  }

  companion object {
    private const val INSERT = 1
    private const val DELETE = 2
  }
}
