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
package org.locationtech.jts.index.strtree

import kotlin.jvm.JvmStatic
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

import org.locationtech.jts.geom.Envelope

/**
 * Functions for computing distances between [Envelope]s.
 *
 * @author mdavis
 */
class EnvelopeDistance {
  companion object {
    /**
     * Computes the maximum distance between the points defining two envelopes.
     *
     * @param env1 an envelope
     * @param env2 an envelope
     * @return the maximum distance between the points defining the envelopes
     */
    @JvmStatic
    fun maximumDistance(env1: Envelope, env2: Envelope): Double {
      val minx = min(env1.getMinX(), env2.getMinX())
      val miny = min(env1.getMinY(), env2.getMinY())
      val maxx = max(env1.getMaxX(), env2.getMaxX())
      val maxy = max(env1.getMaxY(), env2.getMaxY())
      return distance(minx, miny, maxx, maxy)
    }

    private fun distance(x1: Double, y1: Double, x2: Double, y2: Double): Double {
      val dx = x2 - x1
      val dy = y2 - y1
      return hypot(dx, dy)
    }

    /**
     * Computes the Min-Max Distance between two [Envelope]s.
     *
     * @param a an envelope
     * @param b an envelope
     * @return the min-max-distance between the envelopes
     */
    @JvmStatic
    fun minMaxDistance(a: Envelope, b: Envelope): Double {
      val aminx = a.getMinX()
      val aminy = a.getMinY()
      val amaxx = a.getMaxX()
      val amaxy = a.getMaxY()
      val bminx = b.getMinX()
      val bminy = b.getMinY()
      val bmaxx = b.getMaxX()
      val bmaxy = b.getMaxY()

      var dist = maxDistance(aminx, aminy, aminx, amaxy, bminx, bminy, bminx, bmaxy)
      dist = min(dist, maxDistance(aminx, aminy, aminx, amaxy, bminx, bminy, bmaxx, bminy))
      dist = min(dist, maxDistance(aminx, aminy, aminx, amaxy, bmaxx, bmaxy, bminx, bmaxy))
      dist = min(dist, maxDistance(aminx, aminy, aminx, amaxy, bmaxx, bmaxy, bmaxx, bminy))

      dist = min(dist, maxDistance(aminx, aminy, amaxx, aminy, bminx, bminy, bminx, bmaxy))
      dist = min(dist, maxDistance(aminx, aminy, amaxx, aminy, bminx, bminy, bmaxx, bminy))
      dist = min(dist, maxDistance(aminx, aminy, amaxx, aminy, bmaxx, bmaxy, bminx, bmaxy))
      dist = min(dist, maxDistance(aminx, aminy, amaxx, aminy, bmaxx, bmaxy, bmaxx, bminy))

      dist = min(dist, maxDistance(amaxx, amaxy, aminx, amaxy, bminx, bminy, bminx, bmaxy))
      dist = min(dist, maxDistance(amaxx, amaxy, aminx, amaxy, bminx, bminy, bmaxx, bminy))
      dist = min(dist, maxDistance(amaxx, amaxy, aminx, amaxy, bmaxx, bmaxy, bminx, bmaxy))
      dist = min(dist, maxDistance(amaxx, amaxy, aminx, amaxy, bmaxx, bmaxy, bmaxx, bminy))

      dist = min(dist, maxDistance(amaxx, amaxy, amaxx, aminy, bminx, bminy, bminx, bmaxy))
      dist = min(dist, maxDistance(amaxx, amaxy, amaxx, aminy, bminx, bminy, bmaxx, bminy))
      dist = min(dist, maxDistance(amaxx, amaxy, amaxx, aminy, bmaxx, bmaxy, bminx, bmaxy))
      dist = min(dist, maxDistance(amaxx, amaxy, amaxx, aminy, bmaxx, bmaxy, bmaxx, bminy))

      return dist
    }

    /**
     * Computes the maximum distance between two line segments.
     */
    private fun maxDistance(
      ax1: Double, ay1: Double, ax2: Double, ay2: Double,
      bx1: Double, by1: Double, bx2: Double, by2: Double
    ): Double {
      var dist = distance(ax1, ay1, bx1, by1)
      dist = max(dist, distance(ax1, ay1, bx2, by2))
      dist = max(dist, distance(ax2, ay2, bx1, by1))
      dist = max(dist, distance(ax2, ay2, bx2, by2))
      return dist
    }
  }
}
