/*
 * Copyright (c) 2022 Martin Davis.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.locationtech.jts.coverage

import kotlin.jvm.JvmStatic

import org.locationtech.jts.geom.Geometry

/**
 * Unions a polygonal coverage in an efficient way.
 *
 * @author Martin Davis
 */
class CoverageUnion {
  companion object {
    /**
     * Unions a polygonal coverage.
     *
     * @param coverage the polygons in the coverage
     * @return the union of the coverage polygons
     */
    @JvmStatic
    fun union(coverage: Array<Geometry>): Geometry? {
      // union of an empty coverage is null, since no factory is available
      if (coverage.isEmpty())
        return null

      val geomFact = coverage[0].getFactory()
      val geoms = geomFact.createGeometryCollection(coverage)
      return org.locationtech.jts.operation.overlayng.CoverageUnion.union(geoms)
    }
  }
}
