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

package org.locationtech.jts.simplify

/**
 * Simplifies a collection of TaggedLineStrings, preserving topology
 * (in the sense that no new intersections are introduced).
 * This class is essentially just a container for the common
 * indexes used by [TaggedLineStringSimplifier].
 */
internal class TaggedLinesSimplifier {
  private val inputIndex = LineSegmentIndex()
  private val outputIndex = LineSegmentIndex()

  private var distanceTolerance = 0.0

  /**
   * Sets the distance tolerance for the simplification.
   *
   * @param distanceTolerance the approximation tolerance to use
   */
  fun setDistanceTolerance(distanceTolerance: Double) {
    this.distanceTolerance = distanceTolerance
  }

  /**
   * Simplify a collection of TaggedLineStrings
   *
   * @param taggedLines the collection of lines to simplify
   */
  fun simplify(taggedLines: Collection<TaggedLineString>) {
    val jumpChecker = ComponentJumpChecker(taggedLines)

    for (line in taggedLines) {
      inputIndex.add(line)
    }
    for (line in taggedLines) {
      val tlss = TaggedLineStringSimplifier(inputIndex, outputIndex, jumpChecker)
      tlss.simplify(line, distanceTolerance)
    }
  }
}
