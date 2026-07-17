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
package org.locationtech.jts.geomgraph

import kotlin.jvm.JvmStatic

import org.locationtech.jts.geom.TopologyException
import org.locationtech.jts.noding.BasicSegmentString
import org.locationtech.jts.noding.FastNodingValidator
import org.locationtech.jts.noding.SegmentString

/**
 * Validates that a collection of [Edge]s is correctly noded.
 * Throws an appropriate exception if an noding error is found.
 * Uses [FastNodingValidator] to perform the validation.
 *
 *
 * @see FastNodingValidator
 */
class EdgeNodingValidator
/**
 * Creates a new validator for the given collection of [Edge]s.
 *
 * @param edges a collection of Edges.
 */
  (edges: Collection<*>) {

  private val nv: FastNodingValidator = FastNodingValidator(toSegmentStrings(edges))

  /**
   * Checks whether the supplied edges
   * are correctly noded.  Throws an exception if they are not.
   *
   * @throws TopologyException if the SegmentStrings are not correctly noded
   */
  fun checkValid() {
    nv.checkValid()
  }

  companion object {
    /**
     * Checks whether the supplied [Edge]s
     * are correctly noded.
     * Throws a  [TopologyException] if they are not.
     *
     * @param edges a collection of Edges.
     * @throws TopologyException if the SegmentStrings are not correctly noded
     */
    @JvmStatic
    fun checkValid(edges: Collection<*>) {
      val validator = EdgeNodingValidator(edges)
      validator.checkValid()
    }

    @JvmStatic
    fun toSegmentStrings(edges: Collection<*>): MutableList<SegmentString> {
      // convert Edges to SegmentStrings
      val segStrings: MutableList<SegmentString> = ArrayList()
      val i = edges.iterator()
      while (i.hasNext()) {
        val e = i.next() as Edge
        segStrings.add(BasicSegmentString(e.getCoordinates(), e))
      }
      return segStrings
    }
  }
}
