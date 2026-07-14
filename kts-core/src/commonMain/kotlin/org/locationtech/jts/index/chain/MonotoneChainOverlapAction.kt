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
package org.locationtech.jts.index.chain

import kotlin.jvm.JvmField

import org.locationtech.jts.geom.LineSegment

/**
 * The action for the internal iterator for performing
 * overlap queries on a MonotoneChain
 *
 */
open class MonotoneChainOverlapAction {
  @JvmField
  protected var overlapSeg1 = LineSegment()
  @JvmField
  protected var overlapSeg2 = LineSegment()

  /**
   * This function can be overridden if the original chains are needed
   *
   * @param start1 the index of the start of the overlapping segment from mc1
   * @param start2 the index of the start of the overlapping segment from mc2
   */
  open fun overlap(mc1: MonotoneChain, start1: Int, mc2: MonotoneChain, start2: Int) {
    mc1.getLineSegment(start1, overlapSeg1)
    mc2.getLineSegment(start2, overlapSeg2)
    overlap(overlapSeg1, overlapSeg2)
  }

  /**
   * This is a convenience function which can be overridden to obtain the actual
   * line segments which overlap
   * @param seg1
   * @param seg2
   */
  open fun overlap(seg1: LineSegment, seg2: LineSegment) {
  }
}
