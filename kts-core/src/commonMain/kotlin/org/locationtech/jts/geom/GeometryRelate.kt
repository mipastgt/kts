/*
 * Copyright (c) 2020 Martin Davis.
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

import kotlin.jvm.JvmField

import org.locationtech.jts.operation.relate.RelateOp
import org.locationtech.jts.operation.relateng.RelateNG
import org.locationtech.jts.operation.relateng.RelatePredicate

/**
 * Internal class which encapsulates the runtime switch to use RelateNG.
 * (Was package-private in Java; kept module-internal here since only the Kotlin
 * {@link Geometry} calls it.)
 *
 * @author mdavis
 */
internal class GeometryRelate {
  companion object {
    @JvmField
    var RELATE_PROPERTY_NAME: String = "jts.relate"

    @JvmField
    var RELATE_PROPERTY_VALUE_NG: String = "ng"
    @JvmField
    var RELATE_PROPERTY_VALUE_OLD: String = "old"

    /**
     * Currently the old relate implementation is the default
     */
    @JvmField
    var RELATE_NG_DEFAULT: Boolean = false

    private var isRelateNG = RELATE_NG_DEFAULT

    /**
     * This function is provided primarily for unit testing.
     *
     * @param relateImplCode the code for the overlay method (may be null)
     */
    fun setRelateImpl(relateImplCode: String?) {
      if (relateImplCode == null)
        return
      // set flag explicitly since current value may not be default
      isRelateNG = RELATE_NG_DEFAULT

      if (RELATE_PROPERTY_VALUE_NG.equals(relateImplCode, ignoreCase = true))
        isRelateNG = true
    }

    fun intersects(a: Geometry, b: Geometry): Boolean {
      if (isRelateNG) {
        return RelateNG.relate(a, b, RelatePredicate.intersects())
      }
      if (a.isGeometryCollectionInternal() || b.isGeometryCollectionInternal()) {
        for (i in 0 until a.getNumGeometries()) {
          for (j in 0 until b.getNumGeometries()) {
            if (a.getGeometryN(i).intersects(b.getGeometryN(j))) {
              return true
            }
          }
        }
        return false
      }
      return RelateOp.relate(a, b).isIntersects()
    }

    fun contains(a: Geometry, b: Geometry): Boolean {
      if (isRelateNG) {
        return RelateNG.relate(a, b, RelatePredicate.contains())
      }
      // optimization - lower dimension cannot contain areas
      if (b.getDimension() == 2 && a.getDimension() < 2) {
        return false
      }
      // optimization - P cannot contain a non-zero-length L
      if (b.getDimension() == 1 && a.getDimension() < 1 && b.getLength() > 0.0) {
        return false
      }
      // optimization - envelope test
      if (!a.getEnvelopeInternal().contains(b.getEnvelopeInternal()))
        return false
      return RelateOp.relate(a, b).isContains()
    }

    fun covers(a: Geometry, b: Geometry): Boolean {
      if (isRelateNG) {
        return RelateNG.relate(a, b, RelatePredicate.covers())
      }
      // optimization - lower dimension cannot cover areas
      if (b.getDimension() == 2 && a.getDimension() < 2) {
        return false
      }
      // optimization - P cannot cover a non-zero-length L
      if (b.getDimension() == 1 && a.getDimension() < 1 && b.getLength() > 0.0) {
        return false
      }
      // optimization - envelope test
      if (!a.getEnvelopeInternal().covers(b.getEnvelopeInternal()))
        return false
      // optimization for rectangle arguments
      if (a.isRectangle()) {
        // since we have already tested that the test envelope is covered
        return true
      }
      return RelateOp.relate(a, b).isCovers()
    }

    fun coveredBy(a: Geometry, b: Geometry): Boolean {
      if (isRelateNG) {
        return RelateNG.relate(a, b, RelatePredicate.coveredBy())
      }
      return covers(b, a)
    }

    fun crosses(a: Geometry, b: Geometry): Boolean {
      if (isRelateNG) {
        return RelateNG.relate(a, b, RelatePredicate.crosses())
      }
      // short-circuit test
      if (!a.getEnvelopeInternal().intersects(b.getEnvelopeInternal()))
        return false
      return RelateOp.relate(a, b).isCrosses(a.getDimension(), b.getDimension())
    }

    fun disjoint(a: Geometry, b: Geometry): Boolean {
      if (isRelateNG) {
        return RelateNG.relate(a, b, RelatePredicate.disjoint())
      }
      return !intersects(a, b)
    }

    fun equalsTopo(a: Geometry, b: Geometry): Boolean {
      if (isRelateNG) {
        return RelateNG.relate(a, b, RelatePredicate.equalsTopo())
      }
      if (!a.getEnvelopeInternal().equals(b.getEnvelopeInternal()))
        return false
      return RelateOp.relate(a, b).isEquals(a.getDimension(), b.getDimension())
    }

    fun overlaps(a: Geometry, b: Geometry): Boolean {
      if (isRelateNG) {
        return RelateNG.relate(a, b, RelatePredicate.overlaps())
      }
      if (!a.getEnvelopeInternal().intersects(b.getEnvelopeInternal()))
        return false
      return RelateOp.relate(a, b).isOverlaps(a.getDimension(), b.getDimension())
    }

    fun touches(a: Geometry, b: Geometry): Boolean {
      if (isRelateNG) {
        return RelateNG.relate(a, b, RelatePredicate.touches())
      }
      if (!a.getEnvelopeInternal().intersects(b.getEnvelopeInternal()))
        return false
      return RelateOp.relate(a, b).isTouches(a.getDimension(), b.getDimension())
    }

    fun within(a: Geometry, b: Geometry): Boolean {
      if (isRelateNG) {
        return RelateNG.relate(a, b, RelatePredicate.within())
      }
      return contains(b, a)
    }

    fun relate(a: Geometry, b: Geometry): IntersectionMatrix {
      if (isRelateNG) {
        return RelateNG.relate(a, b)
      }
      Geometry.checkNotGeometryCollection(a)
      Geometry.checkNotGeometryCollection(b)
      return RelateOp.relate(a, b)
    }

    fun relate(a: Geometry, b: Geometry, intersectionPattern: String): Boolean {
      if (isRelateNG) {
        return RelateNG.relate(a, b, intersectionPattern)
      }
      Geometry.checkNotGeometryCollection(a)
      Geometry.checkNotGeometryCollection(b)
      return RelateOp.relate(a, b).matches(intersectionPattern)
    }
  }
}
