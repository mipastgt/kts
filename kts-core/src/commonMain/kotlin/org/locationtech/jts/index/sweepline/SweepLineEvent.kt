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
 */
class SweepLineEvent(x: Double, insertEvent: SweepLineEvent?, sweepInt: SweepLineInterval) :
    Comparable<Any?> {

  private val xValue: Double = x
  private val eventType: Int
  private val insertEvent: SweepLineEvent? = insertEvent // null if this is an INSERT event
  private var deleteEventIndex = 0

  private val sweepInt: SweepLineInterval = sweepInt

  init {
    eventType = if (insertEvent != null) DELETE else INSERT
  }

  fun isInsert(): Boolean = insertEvent == null
  fun isDelete(): Boolean = insertEvent != null
  fun getInsertEvent(): SweepLineEvent? = insertEvent
  fun getDeleteEventIndex(): Int = deleteEventIndex
  fun setDeleteEventIndex(deleteEventIndex: Int) {
    this.deleteEventIndex = deleteEventIndex
  }

  fun getInterval(): SweepLineInterval = sweepInt

  /**
   * ProjectionEvents are ordered first by their x-value, and then by their eventType.
   * It is important that Insert events are sorted before Delete events, so that
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
    const val INSERT = 1
    const val DELETE = 2
  }
}
