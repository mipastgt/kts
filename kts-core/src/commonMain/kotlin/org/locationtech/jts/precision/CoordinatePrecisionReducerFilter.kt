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

package org.locationtech.jts.precision

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.CoordinateSequence
import org.locationtech.jts.geom.CoordinateSequenceFilter
import org.locationtech.jts.geom.PrecisionModel

/**
 * Reduces the precision of the [Coordinate]s in a
 * [CoordinateSequence] to match the supplied [PrecisionModel].
 * Uses [PrecisionModel.makePrecise].
 * The input is modified in-place, so
 * it should be cloned beforehand if the
 * original should not be modified.
 *
 * @author mbdavis
 */
class CoordinatePrecisionReducerFilter(private val precModel: PrecisionModel) : CoordinateSequenceFilter {

  /**
   * Rounds the Coordinates in the sequence to match the PrecisionModel
   */
  override fun filter(seq: CoordinateSequence, i: Int) {
    seq.setOrdinate(i, 0, precModel.makePrecise(seq.getOrdinate(i, 0)))
    seq.setOrdinate(i, 1, precModel.makePrecise(seq.getOrdinate(i, 1)))
  }

  /**
   * Always runs over all geometry components.
   *
   * @return false
   */
  override fun isDone(): Boolean = false

  /**
   * Always reports that the geometry has changed
   *
   * @return true
   */
  override fun isGeometryChanged(): Boolean = true
}
