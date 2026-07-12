/*
 * Copyright (c) 2019 Martin Davis.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.locationtech.jts.operation.overlayng

import kotlin.jvm.JvmStatic

import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.TopologyException
import org.locationtech.jts.noding.BoundaryChainNoder
import org.locationtech.jts.noding.Noder
import org.locationtech.jts.noding.SegmentExtractingNoder

/**
 * Unions a valid coverage of polygons or lines
 * in an efficient way.
 *
 * @author Martin Davis
 *
 * @see BoundaryChainNoder
 *
 * @see SegmentExtractingNoder
 */
class CoverageUnion private constructor() {

  companion object {
    /**
     * Unions a valid polygonal coverage or linear network.
     *
     * @param coverage a coverage of polygons or lines
     * @return the union of the coverage
     *
     * @throws TopologyException in some cases if the coverage is invalid
     */
    @JvmStatic
    fun union(coverage: Geometry): Geometry {
      var noder: Noder = BoundaryChainNoder()
      //-- these are less performant
      //Noder noder = new SegmentExtractingNoder();
      //Noder noder = new BoundarySegmentNoder();

      //-- linear networks require a segment-extracting noder
      if (coverage.getDimension() < 2) {
        noder = SegmentExtractingNoder()
      }

      // a precision model is not needed since no noding is done
      return OverlayNG.union(coverage, null, noder)
    }
  }
}
