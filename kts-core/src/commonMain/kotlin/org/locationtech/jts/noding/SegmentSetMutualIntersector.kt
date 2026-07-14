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
 * An intersector for the red-blue intersection problem.
 * In this class of line arrangement problem,
 * two disjoint sets of linestrings are intersected.
 *
 *
 * Implementing classes must provide a way
 * of supplying the base set of segment strings to
 * test against (e.g. in the constructor,
 * for straightforward thread-safety).
 *
 *
 * In order to allow optimizing processing,
 * the following condition is assumed to hold for each set:
 *
 *  * the only intersection between any two linestrings occurs at their endpoints.
 *
 * Implementations can take advantage of this fact to optimize processing
 * (i.e. by avoiding testing for intersections between linestrings
 * belonging to the same set).
 *
 * @author Martin Davis
 */
interface SegmentSetMutualIntersector {
  /**
   * Computes the intersections with a given set of [SegmentString]s,
   * using the supplied [SegmentIntersector].
   *
   * @param segStrings a collection of [SegmentString]s to node
   * @param segInt the intersection detector to either record intersection occurrences
   * or add intersection nodes to the input segment strings.
   */
  fun process(segStrings: Collection<*>?, segInt: SegmentIntersector)
}
