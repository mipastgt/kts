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
class SweepLineInterval {

  private val min: Double
  private val max: Double
  private val item: Any?

  constructor(min: Double, max: Double) : this(min, max, null)

  constructor(min: Double, max: Double, item: Any?) {
    this.min = if (min < max) min else max
    this.max = if (max > min) max else min
    this.item = item
  }

  fun getMin(): Double = min
  fun getMax(): Double = max
  fun getItem(): Any? = item
}
