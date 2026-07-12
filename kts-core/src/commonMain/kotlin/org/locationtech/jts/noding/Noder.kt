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
package org.locationtech.jts.noding

/**
 * Computes all intersections between segments in a set of [SegmentString]s.
 * Intersections found are represented as [SegmentNode]s and added to the
 * [SegmentString]s in which they occur.
 * As a final step in the noding a new set of segment strings split
 * at the nodes may be returned.
 *
 * @version 1.7
 */
interface Noder {

  /**
   * Computes the noding for a collection of [SegmentString]s.
   * Some Noders may add all these nodes to the input SegmentStrings;
   * others may only add some or none at all.
   *
   * @param segStrings a collection of [SegmentString]s to node
   */
  fun computeNodes(segStrings: Collection<*>?)

  /**
   * Returns a [Collection] of fully noded [SegmentString]s.
   * The SegmentStrings have the same context as their parent.
   *
   * @return a Collection of SegmentStrings
   */
  fun getNodedSubstrings(): MutableCollection<*>?

}
