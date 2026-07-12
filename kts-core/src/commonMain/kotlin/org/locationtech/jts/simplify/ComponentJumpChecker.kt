/*
 * Copyright (c) 2023 Martin Davis.
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

import org.locationtech.jts.algorithm.RayCrossingCounter
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Envelope
import org.locationtech.jts.geom.LineSegment

/**
 * Checks if simplifying (flattening) line sections or segments
 * would cause them to "jump" over other components in the geometry.
 *
 * @author mdavis
 */
internal class ComponentJumpChecker(private val components: Collection<TaggedLineString>) {

  /**
   * Checks if a line section jumps a component if flattened.
   *
   * Assumes start <= end.
   */
  fun hasJump(line: TaggedLineString, start: Int, end: Int, seg: LineSegment): Boolean {
    val sectionEnv = computeEnvelope(line, start, end)
    for (comp in components) {
      //-- don't test component against itself
      if (comp === line)
        continue

      val compPt = comp.getComponentPoint()
      if (sectionEnv.intersects(compPt)) {
        if (hasJumpAtComponent(compPt, line, start, end, seg)) {
          return true
        }
      }
    }
    return false
  }

  /**
   * Checks if two consecutive segments jumps a component if flattened.
   */
  fun hasJump(line: TaggedLineString, seg1: LineSegment, seg2: LineSegment, seg: LineSegment): Boolean {
    val sectionEnv = computeEnvelope(seg1, seg2)
    for (comp in components) {
      //-- don't test component against itself
      if (comp === line)
        continue

      val compPt = comp.getComponentPoint()
      if (sectionEnv.intersects(compPt)) {
        if (hasJumpAtComponent(compPt, seg1, seg2, seg)) {
          return true
        }
      }
    }
    return false
  }

  companion object {
    private fun hasJumpAtComponent(compPt: Coordinate, line: TaggedLineString, start: Int, end: Int, seg: LineSegment): Boolean {
      val sectionCount = crossingCount(compPt, line, start, end)
      val segCount = crossingCount(compPt, seg)
      val hasJump = sectionCount % 2 != segCount % 2
      return hasJump
    }

    private fun hasJumpAtComponent(compPt: Coordinate, seg1: LineSegment, seg2: LineSegment, seg: LineSegment): Boolean {
      val sectionCount = crossingCount(compPt, seg1, seg2)
      val segCount = crossingCount(compPt, seg)
      val hasJump = sectionCount % 2 != segCount % 2
      return hasJump
    }

    private fun crossingCount(compPt: Coordinate, seg: LineSegment): Int {
      val rcc = RayCrossingCounter(compPt)
      rcc.countSegment(seg.p0, seg.p1)
      return rcc.getCount()
    }

    private fun crossingCount(compPt: Coordinate, seg1: LineSegment, seg2: LineSegment): Int {
      val rcc = RayCrossingCounter(compPt)
      rcc.countSegment(seg1.p0, seg1.p1)
      rcc.countSegment(seg2.p0, seg2.p1)
      return rcc.getCount()
    }

    private fun crossingCount(compPt: Coordinate, line: TaggedLineString, start: Int, end: Int): Int {
      val rcc = RayCrossingCounter(compPt)
      for (i in start until end) {
        rcc.countSegment(line.getCoordinate(i), line.getCoordinate(i + 1))
      }
      return rcc.getCount()
    }

    private fun computeEnvelope(seg1: LineSegment, seg2: LineSegment): Envelope {
      val env = Envelope()
      env.expandToInclude(seg1.p0)
      env.expandToInclude(seg1.p1)
      env.expandToInclude(seg2.p0)
      env.expandToInclude(seg2.p1)
      return env
    }

    private fun computeEnvelope(line: TaggedLineString, start: Int, end: Int): Envelope {
      val env = Envelope()
      for (i in start..end) {
        env.expandToInclude(line.getCoordinate(i))
      }
      return env
    }
  }
}
