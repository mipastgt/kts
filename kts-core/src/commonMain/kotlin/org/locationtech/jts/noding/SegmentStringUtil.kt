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

import kotlin.jvm.JvmStatic

import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geom.util.LinearComponentExtracter

/**
 * Utility methods for processing [SegmentString]s.
 *
 * @author Martin Davis
 */
class SegmentStringUtil {
  companion object {
    /**
     * Extracts all linear components from a given [Geometry]
     * to [SegmentString]s.
     * The SegmentString data item is set to be the source Geometry.
     *
     * @param geom the geometry to extract from
     * @return a List of SegmentStrings
     */
    @JvmStatic
    fun extractSegmentStrings(geom: Geometry): MutableList<SegmentString> {
      return extractNodedSegmentStrings(geom)
    }

    /**
     * Extracts all linear components from a given [Geometry]
     * to [NodedSegmentString]s.
     * The SegmentString data item is set to be the source Geometry.
     *
     * @param geom the geometry to extract from
     * @return a List of NodedSegmentStrings
     */
    @JvmStatic
    fun extractNodedSegmentStrings(geom: Geometry): MutableList<SegmentString> {
      val segStr = ArrayList<SegmentString>()
      val lines = LinearComponentExtracter.getLines(geom)
      for (obj in lines) {
        val line = obj as LineString
        val pts = line.getCoordinates()
        segStr.add(NodedSegmentString(pts, geom))
      }
      return segStr
    }

    /**
     * Extracts all linear components from a given [Geometry]
     * to [BasicSegmentString]s.
     * The SegmentString data item is set to be the source Geometry.
     *
     * @param geom the geometry to extract from
     * @return a List of BasicSegmentStrings
     */
    @JvmStatic
    fun extractBasicSegmentStrings(geom: Geometry): MutableList<SegmentString> {
      val segStr = ArrayList<SegmentString>()
      val lines = LinearComponentExtracter.getLines(geom)
      for (obj in lines) {
        val line = obj as LineString
        val pts = line.getCoordinates()
        segStr.add(BasicSegmentString(pts, geom))
      }
      return segStr
    }

    /**
     * Converts a collection of [SegmentString]s into a [Geometry].
     * The geometry will be either a [LineString] or a [org.locationtech.jts.geom.MultiLineString] (possibly empty).
     *
     * @param segStrings a collection of SegmentStrings
     * @return a LineString or MultiLineString
     */
    @JvmStatic
    fun toGeometry(segStrings: Collection<*>, geomFact: GeometryFactory): Geometry {
      val lines = arrayOfNulls<LineString>(segStrings.size)
      var index = 0
      for (obj in segStrings) {
        val ss = obj as SegmentString
        val line = geomFact.createLineString(ss.getCoordinates())
        lines[index++] = line
      }
      if (lines.size == 1) return lines[0]!!
      @Suppress("UNCHECKED_CAST")
      return geomFact.createMultiLineString(lines as Array<LineString>)
    }

    @JvmStatic
    fun toString(segStrings: MutableList<*>): String {
      val buf = StringBuilder()
      for (obj in segStrings) {
        val segStr = obj as SegmentString
        buf.append(segStr.toString())
        buf.append("\n")
      }
      return buf.toString()
    }
  }
}
