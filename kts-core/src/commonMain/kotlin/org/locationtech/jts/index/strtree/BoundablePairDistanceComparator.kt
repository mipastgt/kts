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
package org.locationtech.jts.index.strtree

/**
 * The Class BoundablePairDistanceComparator. It implements Java comparator and is used
 * as a parameter to sort the BoundablePair list.
 */
internal class BoundablePairDistanceComparator(
  private val normalOrder: Boolean
) : Comparator<BoundablePair> {

  override fun compare(p1: BoundablePair, p2: BoundablePair): Int {
    val distance1 = p1.getDistance()
    val distance2 = p2.getDistance()
    return if (normalOrder) {
      if (distance1 > distance2) {
        1
      } else if (distance1 == distance2) {
        0
      } else {
        -1
      }
    } else {
      if (distance1 > distance2) {
        -1
      } else if (distance1 == distance2) {
        0
      } else {
        1
      }
    }
  }
}
