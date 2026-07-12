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
package org.locationtech.jts.util

import kotlin.jvm.JvmStatic

import kotlin.time.TimeSource

/**
 * Implements a timer function which can compute
 * elapsed time as well as split times.
 *
 * @version 1.7
 */
class Stopwatch {

  private var startMark: TimeSource.Monotonic.ValueTimeMark = TimeSource.Monotonic.markNow()
  private var totalTime: Long = 0
  private var isRunning = false

  init {
    start()
  }

  fun start() {
    if (isRunning) return
    startMark = TimeSource.Monotonic.markNow()
    isRunning = true
  }

  fun stop(): Long {
    if (isRunning) {
      updateTotalTime()
      isRunning = false
    }
    return totalTime
  }

  fun reset() {
    totalTime = 0
    startMark = TimeSource.Monotonic.markNow()
  }

  fun split(): Long {
    if (isRunning)
      updateTotalTime()
    return totalTime
  }

  private fun updateTotalTime() {
    val endMark = TimeSource.Monotonic.markNow()
    val elapsedTime = (endMark - startMark).inWholeMilliseconds
    startMark = endMark
    totalTime += elapsedTime
  }

  fun getTime(): Long {
    updateTotalTime()
    return totalTime
  }

  fun getTimeString(): String {
    val totalTime = getTime()
    return getTimeString(totalTime)
  }

  companion object {
    @JvmStatic
    fun getTimeString(timeMillis: Long): String {
      val totalTimeStr = if (timeMillis < 10000)
        "$timeMillis ms"
      else
        "${timeMillis.toDouble() / 1000.0} s"
      return totalTimeStr
    }
  }
}
