/*
 * Copyright (c) 2021 Felix Obermaier.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.locationtech.jts.algorithm.match

import org.locationtech.jts.algorithm.distance.DiscreteFrechetDistance
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Envelope
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.MultiPoint

/**
 * Measures the degree of similarity between two
 * [Geometry]s using the Fréchet distance metric.
 * The measure is normalized to lie in the range [0, 1].
 * Higher measures indicate a great degree of similarity.
 *
 * The measure is computed by computing the Fréchet distance
 * between the input geometries, and then normalizing
 * this by dividing it by the diagonal distance across
 * the envelope of the combined geometries.
 *
 * Note: the input should be normalized, especially when
 * measuring [MultiPoint] geometries because for the
 * Fréchet distance the order of [Coordinate]s is
 * important.
 *
 * @author Felix Obermaier
 */
class FrechetSimilarityMeasure : SimilarityMeasure {

  override fun measure(g1: Geometry, g2: Geometry): Double {

    // Check if input is of same type
    if (g1.getGeometryType() != g2.getGeometryType())
      throw IllegalArgumentException("g1 and g2 are of different type")

    // Compute the distance
    val frechetDistance = DiscreteFrechetDistance.distance(g1, g2)
    if (frechetDistance == 0.0) return 1.0

    // Compute envelope diagonal size
    val env = Envelope(g1.getEnvelopeInternal())
    env.expandToInclude(g2.getEnvelopeInternal())
    val envDiagSize = HausdorffSimilarityMeasure.diagonalSize(env)

    // normalize so that more similarity produces a measure closer to 1
    return 1 - frechetDistance / envDiagSize
  }
}
