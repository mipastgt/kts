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
package org.locationtech.jts.geom.prep

import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.Lineal
import org.locationtech.jts.noding.FastSegmentSetIntersectionFinder
import org.locationtech.jts.noding.SegmentStringUtil

/**
 * A prepared version for [Lineal] geometries.
 *
 * Instances of this class are thread-safe.
 *
 * @author mbdavis
 *
 */
open class PreparedLineString(line: Lineal) : BasicPreparedGeometry(line as Geometry) {

  private var segIntFinder: FastSegmentSetIntersectionFinder? = null

  fun getIntersectionFinder(): FastSegmentSetIntersectionFinder {
    /*
     * MD - Another option would be to use a simple scan for
     * segment testing for small geometries.
     * However, testing indicates that there is no particular advantage
     * to this approach.
     */
    if (segIntFinder == null)
      segIntFinder = FastSegmentSetIntersectionFinder(SegmentStringUtil.extractSegmentStrings(getGeometry()))
    return segIntFinder!!
  }

  override fun intersects(g: Geometry): Boolean {
    if (!envelopesIntersect(g)) return false
    return PreparedLineStringIntersects.intersects(this, g)
  }

  /*
   * There's not much point in trying to optimize contains, since
   * contains for linear targets requires the entire test geometry
   * to exactly match the target linework.
   */
}
