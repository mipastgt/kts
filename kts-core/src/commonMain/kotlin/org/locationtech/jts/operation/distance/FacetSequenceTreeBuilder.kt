/*
 * Copyright (c) 2016 Martin Davis.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.locationtech.jts.operation.distance

import kotlin.jvm.JvmStatic

import org.locationtech.jts.geom.CoordinateSequence
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryComponentFilter
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geom.Point
import org.locationtech.jts.index.strtree.STRtree

class FacetSequenceTreeBuilder {
  companion object {
    // 6 seems to be a good facet sequence size
    private const val FACET_SEQUENCE_SIZE = 6

    // Seems to be better to use a minimum node capacity
    private const val STR_TREE_NODE_CAPACITY = 4

    @JvmStatic
    fun build(g: Geometry): STRtree {
      val tree = STRtree(STR_TREE_NODE_CAPACITY)
      val sections = computeFacetSequences(g)
      for (section in sections) {
        tree.insert(section.getEnvelope(), section)
      }
      tree.build()
      return tree
    }

    /**
     * Creates facet sequences
     *
     * @param g
     * @return List of FacetSequence
     */
    private fun computeFacetSequences(g: Geometry): MutableList<FacetSequence> {
      val sections = ArrayList<FacetSequence>()

      g.apply(object : GeometryComponentFilter {

        override fun filter(geom: Geometry) {
          if (geom is LineString) {
            val seq = geom.getCoordinateSequence()
            addFacetSequences(geom, seq, sections)
          } else if (geom is Point) {
            val seq = geom.getCoordinateSequence()
            addFacetSequences(geom, seq, sections)
          }
        }
      })
      return sections
    }

    private fun addFacetSequences(geom: Geometry, pts: CoordinateSequence, sections: MutableList<FacetSequence>) {
      var i = 0
      val size = pts.size()
      while (i <= size - 1) {
        var end = i + FACET_SEQUENCE_SIZE + 1
        // if only one point remains after this section, include it in this
        // section
        if (end >= size - 1)
          end = size
        val sect = FacetSequence(geom, pts, i, end)
        sections.add(sect)
        i = i + FACET_SEQUENCE_SIZE
      }
    }
  }
}
