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

package org.locationtech.jts.linearref

import kotlin.jvm.JvmStatic

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Geometry

/**
 * Computes the [LinearLocation] for a given length
 * along a linear [Geometry].
 * Negative lengths are measured in reverse from end of the linear geometry.
 * Out-of-range values are clamped.
 *
 * **Note:**<br></br>
 * This class is intended for internal use only, and it
 * might be made package-private in a future version of this library
 */
class LengthLocationMap(private val linearGeom: Geometry) {
  // TODO: cache computed cumulative length for each vertex
  // TODO: support user-defined measures
  // TODO: support measure index for fast mapping to a location

  /**
   * Compute the [LinearLocation] corresponding to a length.
   * Negative lengths are measured in reverse from end of the linear geometry.
   * Out-of-range values are clamped.
   * Ambiguous indexes are resolved to the lowest possible location value.
   *
   * @param length the length index
   * @return the corresponding LinearLocation
   */
  fun getLocation(length: Double): LinearLocation {
    return getLocation(length, true)
  }

  /**
   * Compute the [LinearLocation] corresponding to a length.
   * Negative lengths are measured in reverse from end of the linear geometry.
   * Out-of-range values are clamped.
   * Ambiguous indexes are resolved to the lowest or highest possible location value,
   * depending on the value of `resolveLower`
   *
   * @param length the length index
   * @return the corresponding LinearLocation
   */
  fun getLocation(length: Double, resolveLower: Boolean): LinearLocation {
    var forwardLength = length

    // negative values are measured from end of geometry
    if (length < 0.0) {
      val lineLen = linearGeom.getLength()
      forwardLength = lineLen + length
    }
    val loc = getLocationForward(forwardLength)
    if (resolveLower) {
      return loc
    }
    return resolveHigher(loc)
  }

  private fun getLocationForward(length: Double): LinearLocation {
    if (length <= 0.0)
      return LinearLocation()

    var totalLength = 0.0

    val it = LinearIterator(linearGeom)
    while (it.hasNext()) {

      /*
       * Special handling is required for the situation when the
       * length references exactly to a component endpoint.
       * In this case, the endpoint location of the current component
       * is returned,
       * rather than the startpoint location of the next component.
       * This produces consistent behaviour with the project method.
       */
      if (it.isEndOfLine()) {
        if (totalLength == length) {
          val compIndex = it.getComponentIndex()
          val segIndex = it.getVertexIndex()
          return LinearLocation(compIndex, segIndex, 0.0)
        }
      } else {
        val p0 = it.getSegmentStart()
        val p1 = it.getSegmentEnd()
        val segLen = p1!!.distance(p0)
        // length falls in this segment
        if (totalLength + segLen > length) {
          val frac = (length - totalLength) / segLen
          val compIndex = it.getComponentIndex()
          val segIndex = it.getVertexIndex()
          return LinearLocation(compIndex, segIndex, frac)
        }
        totalLength += segLen
      }

      it.next()
    }
    // length is longer than line - return end location
    return LinearLocation.getEndLocation(linearGeom)
  }

  private fun resolveHigher(loc: LinearLocation): LinearLocation {
    if (!loc.isEndpoint(linearGeom))
      return loc
    var compIndex = loc.getComponentIndex()
    // if last component can't resolve any higher
    if (compIndex >= linearGeom.getNumGeometries() - 1) return loc

    do {
      compIndex++
    } while (compIndex < linearGeom.getNumGeometries() - 1
        && linearGeom.getGeometryN(compIndex).getLength() == 0.0)
    // resolve to next higher location
    return LinearLocation(compIndex, 0, 0.0)
  }

  fun getLength(loc: LinearLocation): Double {
    var totalLength = 0.0

    val it = LinearIterator(linearGeom)
    while (it.hasNext()) {
      if (!it.isEndOfLine()) {
        val p0 = it.getSegmentStart()
        val p1 = it.getSegmentEnd()
        val segLen = p1!!.distance(p0)
        // length falls in this segment
        if (loc.getComponentIndex() == it.getComponentIndex()
            && loc.getSegmentIndex() == it.getVertexIndex()) {
          return totalLength + segLen * loc.getSegmentFraction()
        }
        totalLength += segLen
      } else {
        // At the end of the component
        if (loc.getComponentIndex() == it.getComponentIndex()) {
          return totalLength
        }
      }
      it.next()
    }
    return totalLength
  }

  companion object {
    /**
     * Computes the [LinearLocation] for a
     * given length along a linear [Geometry].
     *
     * @param linearGeom the linear geometry to use
     * @param length the length index of the location
     * @return the [LinearLocation] for the length
     */
    @JvmStatic
    fun getLocation(linearGeom: Geometry, length: Double): LinearLocation {
      val locater = LengthLocationMap(linearGeom)
      return locater.getLocation(length)
    }

    /**
     * Computes the [LinearLocation] for a
     * given length along a linear [Geometry],
     * with control over how the location
     * is resolved at component endpoints.
     *
     * @param linearGeom the linear geometry to use
     * @param length the length index of the location
     * @param resolveLower if true lengths are resolved to the lowest possible index
     * @return the [LinearLocation] for the length
     */
    @JvmStatic
    fun getLocation(linearGeom: Geometry, length: Double, resolveLower: Boolean): LinearLocation {
      val locater = LengthLocationMap(linearGeom)
      return locater.getLocation(length, resolveLower)
    }

    /**
     * Computes the length for a given [LinearLocation]
     * on a linear [Geometry].
     *
     * @param linearGeom the linear geometry to use
     * @param loc the [LinearLocation] index of the location
     * @return the length for the [LinearLocation]
     */
    @JvmStatic
    fun getLength(linearGeom: Geometry, loc: LinearLocation): Double {
      val locater = LengthLocationMap(linearGeom)
      return locater.getLength(loc)
    }
  }
}
