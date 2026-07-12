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
package org.locationtech.jts.algorithm.locate
import kotlin.math.max
import kotlin.math.min

import org.locationtech.jts.algorithm.RayCrossingCounter
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.LineSegment
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geom.util.LinearComponentExtracter
import org.locationtech.jts.index.ArrayListVisitor
import org.locationtech.jts.index.ItemVisitor
import org.locationtech.jts.index.intervalrtree.SortedPackedIntervalRTree

/**
 * Determines the [org.locationtech.jts.geom.Location] of [Coordinate]s relative to
 * an areal geometry, using indexing for efficiency.
 * This algorithm is suitable for use in cases where
 * many points will be tested against a given area.
 *
 * The index is lazy-loaded, which allows
 * creating instances even if they are not used.
 *
 * Thread-safe and immutable.
 *
 * @author Martin Davis
 */
class IndexedPointInAreaLocator(g: Geometry) : PointOnGeometryLocator {

  private var geom: Geometry? = g

  private var index: IntervalIndexedGeometry? = null

  /**
   * Determines the [org.locationtech.jts.geom.Location] of a point in an areal [Geometry].
   *
   * @param p the point to test
   * @return the location of the point in the geometry
   */
  override fun locate(p: Coordinate): Int {
    // avoid calling synchronized method improves performance
    if (index == null) createIndex()

    val rcc = RayCrossingCounter(p)

    val visitor = SegmentVisitor(rcc)
    index!!.query(p.y, p.y, visitor)

    return rcc.getLocation()
  }

  /**
   * Creates the indexed geometry, creating it if necessary.
   */
  private fun createIndex() {
    if (index == null) {
      index = IntervalIndexedGeometry(geom!!)
      // no need to hold onto geom
      geom = null
    }
  }

  private class SegmentVisitor(private val counter: RayCrossingCounter) : ItemVisitor {
    override fun visitItem(item: Any?) {
      val seg = item as LineSegment
      counter.countSegment(seg.getCoordinate(0), seg.getCoordinate(1))
    }
  }

  private class IntervalIndexedGeometry(geom: Geometry) {
    private val isEmpty: Boolean
    private val index = SortedPackedIntervalRTree()

    init {
      if (geom.isEmpty()) {
        isEmpty = true
      } else {
        isEmpty = false
        init(geom)
      }
    }

    private fun init(geom: Geometry) {
      val lines = LinearComponentExtracter.getLines(geom)
      val i = lines.iterator()
      while (i.hasNext()) {
        val line = i.next() as LineString
        //-- only include rings of Polygons or LinearRings
        if (!line.isClosed())
          continue

        val pts = line.getCoordinates()
        addLine(pts)
      }
    }

    private fun addLine(pts: Array<Coordinate>) {
      for (i in 1 until pts.size) {
        val seg = LineSegment(pts[i - 1], pts[i])
        val min = min(seg.p0.y, seg.p1.y)
        val max = max(seg.p0.y, seg.p1.y)
        index.insert(min, max, seg)
      }
    }

    fun query(min: Double, max: Double): List<*> {
      if (isEmpty)
        return ArrayList<Any?>()

      val visitor = ArrayListVisitor()
      index.query(min, max, visitor)
      return visitor.getItems()
    }

    fun query(min: Double, max: Double, visitor: ItemVisitor) {
      if (isEmpty)
        return
      index.query(min, max, visitor)
    }
  }
}
