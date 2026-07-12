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

import org.locationtech.jts.geom.Envelope
import org.locationtech.jts.geom.LineSegment
import org.locationtech.jts.index.ItemVisitor
import org.locationtech.jts.index.quadtree.Quadtree

/**
 * An spatial index on a set of [LineSegment]s.
 * Supports adding and removing items.
 *
 * @author Martin Davis
 */
internal class LineSegmentIndex {
  private val index = Quadtree()

  fun add(line: TaggedLineString) {
    val segs = line.getSegments()
    for (i in segs.indices) {
      val seg = segs[i]
      add(seg)
    }
  }

  fun add(seg: LineSegment) {
    index.insert(Envelope(seg.p0, seg.p1), seg)
  }

  fun remove(seg: LineSegment) {
    index.remove(Envelope(seg.p0, seg.p1), seg)
  }

  fun query(querySeg: LineSegment): MutableList<Any?> {
    val env = Envelope(querySeg.p0, querySeg.p1)

    val visitor = LineSegmentVisitor(querySeg)
    index.query(env, visitor)
    val itemsFound = visitor.getItems()

    return itemsFound
  }
}

/**
 * ItemVisitor subclass to reduce volume of query results.
 */
internal class LineSegmentVisitor(private val querySeg: LineSegment) : ItemVisitor {

  private val items = ArrayList<Any?>()

  override fun visitItem(item: Any?) {
    val seg = item as LineSegment
    if (Envelope.intersects(seg.p0, seg.p1, querySeg.p0, querySeg.p1))
      items.add(item)
  }

  fun getItems(): ArrayList<Any?> = items
}
