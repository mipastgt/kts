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

import kotlin.jvm.JvmStatic
import kotlin.jvm.JvmField

import org.locationtech.jts.geom.util.GeometryCollectionMapper
import org.locationtech.jts.geom.util.GeometryMapper
import org.locationtech.jts.operation.overlay.OverlayOp
import org.locationtech.jts.operation.overlay.snap.SnapIfNeededOverlayOp
import org.locationtech.jts.operation.overlayng.OverlayNGRobust
import org.locationtech.jts.operation.union.UnaryUnionOp

/**
 * Internal class which encapsulates the runtime switch to use OverlayNG,
 * and some additional extensions for optimization and GeometryCollection handling.
 * 
 * (Was package-private in Java; widened to public because a same-package Java test
 * accesses setOverlayImpl and Kotlin has no package-private visibility.)
 *
 * @author mdavis
 */
class GeometryOverlay {
  companion object {
    @JvmField
    var OVERLAY_PROPERTY_NAME: String = "jts.overlay"

    @JvmField
    var OVERLAY_PROPERTY_VALUE_NG: String = "ng"
    @JvmField
    var OVERLAY_PROPERTY_VALUE_OLD: String = "old"

    /**
     * Currently the original JTS overlay implementation is the default
     */
    @JvmField
    var OVERLAY_NG_DEFAULT: Boolean = false

    private var isOverlayNG = OVERLAY_NG_DEFAULT

    /**
     * This function is provided primarily for unit testing.
     *
     * @param overlayImplCode the code for the overlay method (may be null)
     */
    @JvmStatic
    fun setOverlayImpl(overlayImplCode: String?) {
      if (overlayImplCode == null)
        return
      // set flag explicitly since current value may not be default
      isOverlayNG = OVERLAY_NG_DEFAULT

      if (OVERLAY_PROPERTY_VALUE_NG.equals(overlayImplCode, ignoreCase = true))
        isOverlayNG = true
    }

    private fun overlay(a: Geometry, b: Geometry, opCode: Int): Geometry {
      if (isOverlayNG) {
        return OverlayNGRobust.overlay(a, b, opCode)
      } else {
        return SnapIfNeededOverlayOp.overlayOp(a, b, opCode)
      }
    }

    @JvmStatic
    fun difference(a: Geometry, b: Geometry): Geometry {
      // special case: if A.isEmpty ==> empty; if B.isEmpty ==> A
      if (a.isEmpty()) return OverlayOp.createEmptyResult(OverlayOp.DIFFERENCE, a, b, a.getFactory())
      if (b.isEmpty()) return a.copy()

      Geometry.checkNotGeometryCollection(a)
      Geometry.checkNotGeometryCollection(b)

      return overlay(a, b, OverlayOp.DIFFERENCE)
    }

    @JvmStatic
    fun intersection(a: Geometry, b: Geometry): Geometry {
      // special case: if one input is empty ==> empty
      if (a.isEmpty() || b.isEmpty())
        return OverlayOp.createEmptyResult(OverlayOp.INTERSECTION, a, b, a.getFactory())

      // compute for GCs
      // (An inefficient algorithm, but will work)
      if (a.isGeometryCollectionInternal()) {
        val g2 = b
        return GeometryCollectionMapper.map(
          a as GeometryCollection,
          object : GeometryMapper.MapOp {
            override fun map(g: Geometry): Geometry {
              return g.intersection(g2)
            }
          })
      }

      return overlay(a, b, OverlayOp.INTERSECTION)
    }

    @JvmStatic
    fun symDifference(a: Geometry, b: Geometry): Geometry {
      // handle empty geometry cases
      if (a.isEmpty() || b.isEmpty()) {
        // both empty - check dimensions
        if (a.isEmpty() && b.isEmpty())
          return OverlayOp.createEmptyResult(OverlayOp.SYMDIFFERENCE, a, b, a.getFactory())

        // special case: if either input is empty ==> result = other arg
        if (a.isEmpty()) return b.copy()
        if (b.isEmpty()) return a.copy()
      }

      Geometry.checkNotGeometryCollection(a)
      Geometry.checkNotGeometryCollection(b)
      return overlay(a, b, OverlayOp.SYMDIFFERENCE)
    }

    @JvmStatic
    fun union(a: Geometry, b: Geometry): Geometry {
      // handle empty geometry cases
      if (a.isEmpty() || b.isEmpty()) {
        if (a.isEmpty() && b.isEmpty())
          return OverlayOp.createEmptyResult(OverlayOp.UNION, a, b, a.getFactory())

        // special case: if either input is empty ==> other input
        if (a.isEmpty()) return b.copy()
        if (b.isEmpty()) return a.copy()
      }

      Geometry.checkNotGeometryCollection(a)
      Geometry.checkNotGeometryCollection(b)

      return overlay(a, b, OverlayOp.UNION)
    }

    @JvmStatic
    fun union(a: Geometry): Geometry {
      if (isOverlayNG) {
        return OverlayNGRobust.union(a)
      } else {
        return UnaryUnionOp.union(a)
      }
    }
  }
}
