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
package org.locationtech.jts.index.hprtree
import kotlin.math.pow

import org.locationtech.jts.geom.Envelope
import org.locationtech.jts.shape.fractal.HilbertCode

class HilbertEncoder(private val level: Int, extent: Envelope) {
  private val minx: Double
  private val miny: Double
  private val strideX: Double
  private val strideY: Double

  init {
    val hside = (2.0).pow(level.toDouble()).toInt() - 1

    minx = extent.getMinX()
    strideX = extent.getWidth() / hside

    miny = extent.getMinY()
    strideY = extent.getHeight() / hside
  }

  fun encode(env: Envelope): Int {
    val midx = env.getWidth() / 2 + env.getMinX()
    val x = ((midx - minx) / strideX).toInt()

    val midy = env.getHeight() / 2 + env.getMinY()
    val y = ((midy - miny) / strideY).toInt()

    return HilbertCode.encode(level, x, y)
  }
}
