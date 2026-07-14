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
package org.locationtech.jts.geom

import kotlin.jvm.JvmStatic
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min


/**
 *  Defines a rectangular region of the 2D coordinate plane.
 *  It is often used to represent the bounding box of a [Geometry],
 *  e.g. the minimum and maximum x and y values of the [Coordinate]s.
 *  
 *  Envelopes support infinite or half-infinite regions, by using the values of
 *  `Double.POSITIVE_INFINITY` and `Double.NEGATIVE_INFINITY`.
 *  Envelope objects may have a null value.
 *  
 *  When Envelope objects are created or initialized,
 *  the supplies extent values are automatically sorted into the correct order.
 *
 */
open class Envelope : Comparable<Any?> {

  /**
   *  the minimum x-coordinate
   */
  private var minx = 0.0

  /**
   *  the maximum x-coordinate
   */
  private var maxx = 0.0

  /**
   *  the minimum y-coordinate
   */
  private var miny = 0.0

  /**
   *  the maximum y-coordinate
   */
  private var maxy = 0.0

  /**
   *  Creates a null `Envelope`.
   */
  constructor() {
    init()
  }

  /**
   *  Creates an `Envelope` for a region defined by maximum and minimum values.
   *
   * @param  x1  the first x-value
   * @param  x2  the second x-value
   * @param  y1  the first y-value
   * @param  y2  the second y-value
   */
  constructor(x1: Double, x2: Double, y1: Double, y2: Double) {
    init(x1, x2, y1, y2)
  }

  /**
   *  Creates an `Envelope` for a region defined by two Coordinates.
   *
   * @param  p1  the first Coordinate
   * @param  p2  the second Coordinate
   */
  constructor(p1: Coordinate, p2: Coordinate) {
    init(p1.x, p2.x, p1.y, p2.y)
  }

  /**
   *  Creates an `Envelope` for a region defined by a single Coordinate.
   *
   * @param  p  the Coordinate
   */
  constructor(p: Coordinate) {
    init(p.x, p.x, p.y, p.y)
  }

  /**
   *  Create an `Envelope` from an existing Envelope.
   *
   * @param  env  the Envelope to initialize from
   */
  constructor(env: Envelope) {
    init(env)
  }

  override fun hashCode(): Int {
    //Algorithm from Effective Java by Joshua Bloch [Jon Aquino]
    var result = 17
    result = 37 * result + Coordinate.hashCode(minx)
    result = 37 * result + Coordinate.hashCode(maxx)
    result = 37 * result + Coordinate.hashCode(miny)
    result = 37 * result + Coordinate.hashCode(maxy)
    return result
  }

  /**
   *  Initialize to a null `Envelope`.
   */
  fun init() {
    setToNull()
  }

  /**
   *  Initialize an `Envelope` for a region defined by maximum and minimum values.
   *
   * @param  x1  the first x-value
   * @param  x2  the second x-value
   * @param  y1  the first y-value
   * @param  y2  the second y-value
   */
  fun init(x1: Double, x2: Double, y1: Double, y2: Double) {
    if (x1 < x2) {
      minx = x1
      maxx = x2
    } else {
      minx = x2
      maxx = x1
    }
    if (y1 < y2) {
      miny = y1
      maxy = y2
    } else {
      miny = y2
      maxy = y1
    }
  }

  /**
   * Creates a copy of this envelope object.
   *
   * @return a copy of this envelope
   */
  fun copy(): Envelope {
    return Envelope(this)
  }

  /**
   *  Initialize an `Envelope` to a region defined by two Coordinates.
   *
   * @param  p1  the first Coordinate
   * @param  p2  the second Coordinate
   */
  fun init(p1: Coordinate, p2: Coordinate) {
    init(p1.x, p2.x, p1.y, p2.y)
  }

  /**
   *  Initialize an `Envelope` to a region defined by a single Coordinate.
   *
   * @param  p  the coordinate
   */
  fun init(p: Coordinate) {
    init(p.x, p.x, p.y, p.y)
  }

  /**
   *  Initialize an `Envelope` from an existing Envelope.
   *
   * @param  env  the Envelope to initialize from
   */
  fun init(env: Envelope) {
    this.minx = env.minx
    this.maxx = env.maxx
    this.miny = env.miny
    this.maxy = env.maxy
  }

  /**
   *  Makes this `Envelope` a "null" envelope, that is, the envelope
   *  of the empty geometry.
   */
  fun setToNull() {
    minx = 0.0
    maxx = -1.0
    miny = 0.0
    maxy = -1.0
  }

  /**
   *  Returns `true` if this `Envelope` is a "null"
   *  envelope.
   *
   * @return    `true` if this `Envelope` is uninitialized
   *      or is the envelope of the empty geometry.
   */
  fun isNull(): Boolean {
    return maxx < minx
  }

  /**
   *  Returns the difference between the maximum and minimum x values.
   *
   * @return    max x - min x, or 0 if this is a null `Envelope`
   */
  fun getWidth(): Double {
    if (isNull()) {
      return 0.0
    }
    return maxx - minx
  }

  /**
   *  Returns the difference between the maximum and minimum y values.
   *
   * @return    max y - min y, or 0 if this is a null `Envelope`
   */
  fun getHeight(): Double {
    if (isNull()) {
      return 0.0
    }
    return maxy - miny
  }

  /**
   * Gets the length of the diameter (diagonal) of the envelope.
   *
   * @return the diameter length
   */
  fun getDiameter(): Double {
    if (isNull()) {
      return 0.0
    }
    val w = getWidth()
    val h = getHeight()
    return hypot(w, h)
  }

  /**
   *  Returns the `Envelope`s minimum x-value. min x > max x
   *  indicates that this is a null `Envelope`.
   *
   * @return    the minimum x-coordinate
   */
  fun getMinX(): Double {
    return minx
  }

  /**
   *  Returns the `Envelope`s maximum x-value. min x > max x
   *  indicates that this is a null `Envelope`.
   *
   * @return    the maximum x-coordinate
   */
  fun getMaxX(): Double {
    return maxx
  }

  /**
   *  Returns the `Envelope`s minimum y-value. min y > max y
   *  indicates that this is a null `Envelope`.
   *
   * @return    the minimum y-coordinate
   */
  fun getMinY(): Double {
    return miny
  }

  /**
   *  Returns the `Envelope`s maximum y-value. min y > max y
   *  indicates that this is a null `Envelope`.
   *
   * @return    the maximum y-coordinate
   */
  fun getMaxY(): Double {
    return maxy
  }

  /**
   * Gets the area of this envelope.
   *
   * @return the area of the envelope
   * @return 0.0 if the envelope is null
   */
  fun getArea(): Double {
    return getWidth() * getHeight()
  }

  /**
   * Gets the minimum extent of this envelope across both dimensions.
   *
   * @return the minimum extent of this envelope
   */
  fun minExtent(): Double {
    if (isNull()) return 0.0
    val w = getWidth()
    val h = getHeight()
    if (w < h) return w
    return h
  }

  /**
   * Gets the maximum extent of this envelope across both dimensions.
   *
   * @return the maximum extent of this envelope
   */
  fun maxExtent(): Double {
    if (isNull()) return 0.0
    val w = getWidth()
    val h = getHeight()
    if (w > h) return w
    return h
  }

  /**
   *  Enlarges this `Envelope` so that it contains
   *  the given [Coordinate].
   *  Has no effect if the point is already on or within the envelope.
   *
   * @param  p  the Coordinate to expand to include
   */
  fun expandToInclude(p: Coordinate) {
    expandToInclude(p.x, p.y)
  }

  /**
   * Expands this envelope by a given distance in all directions.
   * Both positive and negative distances are supported.
   *
   * @param distance the distance to expand the envelope
   */
  fun expandBy(distance: Double) {
    expandBy(distance, distance)
  }

  /**
   * Expands this envelope by a given distance in all directions.
   * Both positive and negative distances are supported.
   *
   * @param deltaX the distance to expand the envelope along the the X axis
   * @param deltaY the distance to expand the envelope along the the Y axis
   */
  fun expandBy(deltaX: Double, deltaY: Double) {
    if (isNull()) return

    minx -= deltaX
    maxx += deltaX
    miny -= deltaY
    maxy += deltaY

    // check for envelope disappearing
    if (minx > maxx || miny > maxy)
      setToNull()
  }

  /**
   *  Enlarges this `Envelope` so that it contains
   *  the given point.
   *  Has no effect if the point is already on or within the envelope.
   *
   * @param  x  the value to lower the minimum x to or to raise the maximum x to
   * @param  y  the value to lower the minimum y to or to raise the maximum y to
   */
  fun expandToInclude(x: Double, y: Double) {
    if (isNull()) {
      minx = x
      maxx = x
      miny = y
      maxy = y
    } else {
      if (x < minx) {
        minx = x
      }
      if (x > maxx) {
        maxx = x
      }
      if (y < miny) {
        miny = y
      }
      if (y > maxy) {
        maxy = y
      }
    }
  }

  /**
   *  Enlarges this `Envelope` so that it contains
   *  the `other` Envelope.
   *  Has no effect if `other` is wholly on or
   *  within the envelope.
   *
   * @param  other  the `Envelope` to expand to include
   */
  fun expandToInclude(other: Envelope) {
    if (other.isNull()) {
      return
    }
    if (isNull()) {
      minx = other.getMinX()
      maxx = other.getMaxX()
      miny = other.getMinY()
      maxy = other.getMaxY()
    } else {
      if (other.minx < minx) {
        minx = other.minx
      }
      if (other.maxx > maxx) {
        maxx = other.maxx
      }
      if (other.miny < miny) {
        miny = other.miny
      }
      if (other.maxy > maxy) {
        maxy = other.maxy
      }
    }
  }

  /**
   * Translates this envelope by given amounts in the X and Y direction.
   *
   * @param transX the amount to translate along the X axis
   * @param transY the amount to translate along the Y axis
   */
  fun translate(transX: Double, transY: Double) {
    if (isNull()) {
      return
    }
    init(
      getMinX() + transX, getMaxX() + transX,
      getMinY() + transY, getMaxY() + transY
    )
  }

  /**
   * Computes the coordinate of the centre of this envelope (as long as it is non-null
   *
   * @return the centre coordinate of this envelope
   * `null` if the envelope is null
   */
  fun centre(): Coordinate? {
    if (isNull()) return null
    return Coordinate(
      (getMinX() + getMaxX()) / 2.0,
      (getMinY() + getMaxY()) / 2.0
    )
  }

  /**
   * Computes the intersection of two [Envelope]s.
   *
   * @param env the envelope to intersect with
   * @return a new Envelope representing the intersection of the envelopes (this will be
   * the null envelope if either argument is null, or they do not intersect
   */
  fun intersection(env: Envelope): Envelope {
    if (isNull() || env.isNull() || !intersects(env)) return Envelope()

    val intMinX = if (minx > env.minx) minx else env.minx
    val intMinY = if (miny > env.miny) miny else env.miny
    val intMaxX = if (maxx < env.maxx) maxx else env.maxx
    val intMaxY = if (maxy < env.maxy) maxy else env.maxy
    return Envelope(intMinX, intMaxX, intMinY, intMaxY)
  }

  /**
   * Tests if the region defined by `other`
   * intersects the region of this `Envelope`.
   * 
   * A null envelope never intersects.
   *
   * @param  other  the `Envelope` which this `Envelope` is
   *          being checked for intersecting
   * @return        `true` if the `Envelope`s intersect
   */
  fun intersects(other: Envelope): Boolean {
    if (isNull() || other.isNull()) {
      return false
    }
    return !(other.minx > maxx ||
      other.maxx < minx ||
      other.miny > maxy ||
      other.maxy < miny)
  }

  /**
   * Tests if the extent defined by two extremal points
   * intersects the extent of this `Envelope`.
   *
   * @param a a point
   * @param b another point
   * @return   `true` if the extents intersect
   */
  fun intersects(a: Coordinate, b: Coordinate): Boolean {
    if (isNull()) {
      return false
    }

    val envminx = if (a.x < b.x) a.x else b.x
    if (envminx > maxx) return false

    val envmaxx = if (a.x > b.x) a.x else b.x
    if (envmaxx < minx) return false

    val envminy = if (a.y < b.y) a.y else b.y
    if (envminy > maxy) return false

    val envmaxy = if (a.y > b.y) a.y else b.y
    if (envmaxy < miny) return false

    return true
  }

  /**
   * Tests if the region defined by `other`
   * is disjoint from the region of this `Envelope`.
   * 
   * A null envelope is always disjoint.
   *
   * @param  other  the `Envelope` being checked for disjointness
   * @return        `true` if the `Envelope`s are disjoint
   *
   * @see #intersects(Envelope)
   */
  fun disjoint(other: Envelope): Boolean {
    return !intersects(other)
  }

  /**
   * @deprecated Use #intersects instead. In the future, #overlaps may be
   * changed to be a true overlap check; that is, whether the intersection is
   * two-dimensional.
   */
  fun overlaps(other: Envelope): Boolean {
    return intersects(other)
  }

  /**
   * Tests if the point `p`
   * intersects (lies inside) the region of this `Envelope`.
   *
   * @param  p  the `Coordinate` to be tested
   * @return `true` if the point intersects this `Envelope`
   */
  fun intersects(p: Coordinate): Boolean {
    return intersects(p.x, p.y)
  }

  /**
   * @deprecated Use #intersects instead.
   */
  fun overlaps(p: Coordinate): Boolean {
    return intersects(p)
  }

  /**
   *  Check if the point `(x, y)`
   *  intersects (lies inside) the region of this `Envelope`.
   *
   * @param  x  the x-ordinate of the point
   * @param  y  the y-ordinate of the point
   * @return        `true` if the point overlaps this `Envelope`
   */
  fun intersects(x: Double, y: Double): Boolean {
    if (isNull()) return false
    return !(x > maxx ||
      x < minx ||
      y > maxy ||
      y < miny)
  }

  /**
   * @deprecated Use #intersects instead.
   */
  fun overlaps(x: Double, y: Double): Boolean {
    return intersects(x, y)
  }

  /**
   * Tests if the `Envelope other`
   * lies wholely inside this `Envelope` (inclusive of the boundary).
   * 
   * Note that this is <b>not</b> the same definition as the SFS `contains`,
   * which would exclude the envelope boundary.
   *
   * @param  other the `Envelope` to check
   * @return true if `other` is contained in this `Envelope`
   *
   * @see #covers(Envelope)
   */
  fun contains(other: Envelope): Boolean {
    return covers(other)
  }

  /**
   * Tests if the given point lies in or on the envelope.
   * 
   * Note that this is <b>not</b> the same definition as the SFS `contains`,
   * which would exclude the envelope boundary.
   *
   * @param  p  the point which this `Envelope` is
   *      being checked for containing
   * @return    `true` if the point lies in the interior or
   *      on the boundary of this `Envelope`.
   *
   * @see #covers(Coordinate)
   */
  fun contains(p: Coordinate): Boolean {
    return covers(p)
  }

  /**
   * Tests if the given point lies in or on the envelope.
   * 
   * Note that this is <b>not</b> the same definition as the SFS `contains`,
   * which would exclude the envelope boundary.
   *
   * @param  x  the x-coordinate of the point which this `Envelope` is
   *      being checked for containing
   * @param  y  the y-coordinate of the point which this `Envelope` is
   *      being checked for containing
   * @return    `true` if `(x, y)` lies in the interior or
   *      on the boundary of this `Envelope`.
   *
   * @see #covers(double, double)
   */
  fun contains(x: Double, y: Double): Boolean {
    return covers(x, y)
  }

  /**
   * Tests if an envelope is properly contained in this one.
   * The envelope is properly contained if it is contained
   * by this one but not equal to it.
   *
   * @param other the envelope to test
   * @return true if the envelope is properly contained
   */
  fun containsProperly(other: Envelope): Boolean {
    if (equals(other))
      return false
    return covers(other)
  }

  /**
   * Tests if the given point lies in or on the envelope.
   *
   * @param  x  the x-coordinate of the point which this `Envelope` is
   *      being checked for containing
   * @param  y  the y-coordinate of the point which this `Envelope` is
   *      being checked for containing
   * @return    `true` if `(x, y)` lies in the interior or
   *      on the boundary of this `Envelope`.
   */
  fun covers(x: Double, y: Double): Boolean {
    if (isNull()) return false
    return x >= minx &&
      x <= maxx &&
      y >= miny &&
      y <= maxy
  }

  /**
   * Tests if the given point lies in or on the envelope.
   *
   * @param  p  the point which this `Envelope` is
   *      being checked for containing
   * @return    `true` if the point lies in the interior or
   *      on the boundary of this `Envelope`.
   */
  fun covers(p: Coordinate): Boolean {
    return covers(p.x, p.y)
  }

  /**
   * Tests if the `Envelope other`
   * lies wholely inside this `Envelope` (inclusive of the boundary).
   *
   * @param  other the `Envelope` to check
   * @return true if this `Envelope` covers the `other`
   */
  fun covers(other: Envelope): Boolean {
    if (isNull() || other.isNull()) {
      return false
    }
    return other.getMinX() >= minx &&
      other.getMaxX() <= maxx &&
      other.getMinY() >= miny &&
      other.getMaxY() <= maxy
  }

  /**
   * Computes the distance between this and another
   * `Envelope`.
   * The distance between overlapping Envelopes is 0.  Otherwise, the
   * distance is the Euclidean distance between the closest points.
   */
  fun distance(env: Envelope): Double {
    if (intersects(env)) return 0.0

    var dx = 0.0
    if (maxx < env.minx)
      dx = env.minx - maxx
    else if (minx > env.maxx)
      dx = minx - env.maxx

    var dy = 0.0
    if (maxy < env.miny)
      dy = env.miny - maxy
    else if (miny > env.maxy) dy = miny - env.maxy

    // if either is zero, the envelopes overlap either vertically or horizontally
    if (dx == 0.0) return dy
    if (dy == 0.0) return dx
    return hypot(dx, dy)
  }

  override fun equals(other: Any?): Boolean {
    if (other !is Envelope) {
      return false
    }
    val otherEnvelope = other
    if (isNull()) {
      return otherEnvelope.isNull()
    }
    return maxx == otherEnvelope.getMaxX() &&
      maxy == otherEnvelope.getMaxY() &&
      minx == otherEnvelope.getMinX() &&
      miny == otherEnvelope.getMinY()
  }

  override fun toString(): String {
    return "Env[" + minx + " : " + maxx + ", " + miny + " : " + maxy + "]"
  }

  /**
   * Compares two envelopes using lexicographic ordering.
   * The ordering comparison is based on the usual numerical
   * comparison between the sequence of ordinates.
   * Null envelopes are less than all non-null envelopes.
   *
   * @param o an Envelope object
   */
  override fun compareTo(o: Any?): Int {
    val env = o as Envelope
    // compare nulls if present
    if (isNull()) {
      if (env.isNull()) return 0
      return -1
    } else {
      if (env.isNull()) return 1
    }
    // compare based on numerical ordering of ordinates
    if (minx < env.minx) return -1
    if (minx > env.minx) return 1
    if (miny < env.miny) return -1
    if (miny > env.miny) return 1
    if (maxx < env.maxx) return -1
    if (maxx > env.maxx) return 1
    if (maxy < env.maxy) return -1
    if (maxy > env.maxy) return 1
    return 0
  }

  companion object {

    /**
     * Test the point q to see whether it intersects the Envelope defined by p1-p2
     * @param p1 one extremal point of the envelope
     * @param p2 another extremal point of the envelope
     * @param q the point to test for intersection
     * @return `true` if q intersects the envelope p1-p2
     */
    @JvmStatic
    fun intersects(p1: Coordinate, p2: Coordinate, q: Coordinate): Boolean {
      //OptimizeIt shows that Math#min and Math#max here are a bottleneck.
      //Replace with direct comparisons. [Jon Aquino]
      if (((q.x >= (if (p1.x < p2.x) p1.x else p2.x)) && (q.x <= (if (p1.x > p2.x) p1.x else p2.x))) &&
        ((q.y >= (if (p1.y < p2.y) p1.y else p2.y)) && (q.y <= (if (p1.y > p2.y) p1.y else p2.y)))) {
        return true
      }
      return false
    }

    /**
     * Tests whether the envelope defined by p1-p2
     * and the envelope defined by q1-q2
     * intersect.
     *
     * @param p1 one extremal point of the envelope P
     * @param p2 another extremal point of the envelope P
     * @param q1 one extremal point of the envelope Q
     * @param q2 another extremal point of the envelope Q
     * @return `true` if Q intersects P
     */
    @JvmStatic
    fun intersects(p1: Coordinate, p2: Coordinate, q1: Coordinate, q2: Coordinate): Boolean {
      var minq = min(q1.x, q2.x)
      var maxq = max(q1.x, q2.x)
      var minp = min(p1.x, p2.x)
      var maxp = max(p1.x, p2.x)

      if (minp > maxq)
        return false
      if (maxp < minq)
        return false

      minq = min(q1.y, q2.y)
      maxq = max(q1.y, q2.y)
      minp = min(p1.y, p2.y)
      maxp = max(p1.y, p2.y)

      if (minp > maxq)
        return false
      if (maxp < minq)
        return false
      return true
    }
  }
}
