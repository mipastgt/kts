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

import org.locationtech.jts.util.TreeMap

import org.locationtech.jts.geom.CoordinateArrays

/**
 * Dissolves a noded collection of [SegmentString]s to produce
 * a set of merged linework with unique segments.
 * A custom [SegmentStringMerger] merging strategy
 * can be supplied.
 * This strategy will be called when two identical (up to orientation)
 * strings are dissolved together.
 * The default merging strategy is simply to discard one of the merged strings.
 *
 *
 * A common use for this class is to merge noded edges
 * while preserving topological labelling.
 * This requires a custom merging strategy to be supplied
 * to merge the topology labels appropriately.
 *
 * @version 1.7
 * @see SegmentStringMerger
 */
class SegmentStringDissolver
/**
 * Creates a dissolver with a user-defined merge strategy.
 *
 * @param merger the merging strategy to use
 */
  (private val merger: SegmentStringMerger?) {

  private val ocaMap: MutableMap<OrientedCoordinateArray, SegmentString> = TreeMap()

  // testing only
  //private List testAddedSS = new ArrayList();

  /**
   * Creates a dissolver with the default merging strategy.
   */
  constructor() : this(null)

  /**
   * Dissolve all [SegmentString]s in the input [Collection]
   * @param segStrings
   */
  fun dissolve(segStrings: Collection<*>) {
    for (obj in segStrings) {
      dissolve(obj as SegmentString)
    }
  }

  private fun add(oca: OrientedCoordinateArray, segString: SegmentString) {
    ocaMap.put(oca, segString)
    //testAddedSS.add(oca);
  }

  /**
   * Dissolve the given [SegmentString].
   *
   * @param segString the string to dissolve
   */
  fun dissolve(segString: SegmentString) {
    val oca = OrientedCoordinateArray(segString.getCoordinates())
    val existing = findMatching(oca, segString)
    if (existing == null) {
      add(oca, segString)
    } else {
      if (merger != null) {
        val isSameOrientation = CoordinateArrays.equals(existing.getCoordinates(), segString.getCoordinates())
        merger.merge(existing, segString, isSameOrientation)
      }
    }
  }

  private fun findMatching(oca: OrientedCoordinateArray, segString: SegmentString): SegmentString? {
    val matchSS = ocaMap[oca]
    return matchSS
  }

  /**
   * Gets the collection of dissolved (i.e. unique) [SegmentString]s
   *
   * @return the unique [SegmentString]s
   */
  fun getDissolved(): MutableCollection<SegmentString> {
    return ocaMap.values
  }

  /**
   * A merging strategy which can be used to update the context data of [SegmentString]s
   * which are merged during the dissolve process.
   *
   * @author mbdavis
   */
  interface SegmentStringMerger {
    /**
     * Updates the context data of a SegmentString
     * when an identical (up to orientation) one is found during dissolving.
     *
     * @param mergeTarget the segment string to update
     * @param ssToMerge the segment string being dissolved
     * @param isSameOrientation `true` if the strings are in the same direction,
     * `false` if they are opposite
     */
    fun merge(mergeTarget: SegmentString, ssToMerge: SegmentString, isSameOrientation: Boolean)
  }
}
