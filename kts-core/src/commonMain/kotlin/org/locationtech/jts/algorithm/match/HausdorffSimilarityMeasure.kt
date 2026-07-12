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

package org.locationtech.jts.algorithm.match

import kotlin.jvm.JvmStatic
import kotlin.math.sqrt

import org.locationtech.jts.algorithm.distance.DiscreteHausdorffDistance
import org.locationtech.jts.geom.Envelope
import org.locationtech.jts.geom.Geometry

/**
 * Measures the degree of similarity between two [Geometry]s
 * using the Hausdorff distance metric.
 * The measure is normalized to lie in the range [0, 1].
 * Higher measures indicate a great degree of similarity.
 *
 * The measure is computed by computing the Hausdorff distance
 * between the input geometries, and then normalizing
 * this by dividing it by the diagonal distance across
 * the envelope of the combined geometries.
 *
 * @author mbdavis
 */
class HausdorffSimilarityMeasure : SimilarityMeasure {

  override fun measure(g1: Geometry, g2: Geometry): Double {
    val distance = DiscreteHausdorffDistance.distance(g1, g2, DENSIFY_FRACTION)
    if (distance == 0.0) return 1.0

    val env = Envelope(g1.getEnvelopeInternal())
    env.expandToInclude(g2.getEnvelopeInternal())
    val envSize = diagonalSize(env)

    // normalize so that more similarity produces a measure closer to 1
    val measure = 1 - distance / envSize

    return measure
  }

  companion object {
    /*
     * Densify a small amount to increase accuracy of Hausdorff distance
     */
    private const val DENSIFY_FRACTION = 0.25

    @JvmStatic
    fun diagonalSize(env: Envelope): Double {
      if (env.isNull()) return 0.0

      val width = env.getWidth()
      val hgt = env.getHeight()

      return sqrt(width * width + hgt * hgt)
    }
  }
}
