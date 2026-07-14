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
package org.locationtech.jts.operation.buffer

import kotlin.jvm.JvmStatic
import kotlin.math.cos

import org.locationtech.jts.algorithm.Angle

/**
 * A value class containing the parameters which
 * specify how a buffer should be constructed.
 * 
 * The parameters allow control over:
 * 
 * - Quadrant segments (accuracy of approximation for circular arcs)
 * - End Cap style
 * - Join style
 * - Mitre limit
 * - whether the buffer is single-sided
 * 
 *
 * @author Martin Davis
 *
 */
class BufferParameters {

  private var quadrantSegments = DEFAULT_QUADRANT_SEGMENTS
  private var endCapStyle = CAP_ROUND
  private var joinStyle = JOIN_ROUND
  private var mitreLimit = DEFAULT_MITRE_LIMIT
  private var singleSided = false
  private var simplifyFactor = DEFAULT_SIMPLIFY_FACTOR

  /**
   * Creates a default set of parameters
   *
   */
  constructor()

  /**
   * Creates a set of parameters with the
   * given quadrantSegments value.
   *
   * @param quadrantSegments the number of quadrant segments to use
   */
  constructor(quadrantSegments: Int) {
    setQuadrantSegments(quadrantSegments)
  }

  /**
   * Creates a set of parameters with the
   * given quadrantSegments and endCapStyle values.
   *
   * @param quadrantSegments the number of quadrant segments to use
   * @param endCapStyle the end cap style to use
   */
  constructor(quadrantSegments: Int, endCapStyle: Int) {
    setQuadrantSegments(quadrantSegments)
    setEndCapStyle(endCapStyle)
  }

  /**
   * Creates a set of parameters with the
   * given parameter values.
   *
   * @param quadrantSegments the number of quadrant segments to use
   * @param endCapStyle the end cap style to use
   * @param joinStyle the join style to use
   * @param mitreLimit the mitre limit to use
   */
  constructor(quadrantSegments: Int, endCapStyle: Int, joinStyle: Int, mitreLimit: Double) {
    setQuadrantSegments(quadrantSegments)
    setEndCapStyle(endCapStyle)
    setJoinStyle(joinStyle)
    setMitreLimit(mitreLimit)
  }

  /**
   * Gets the number of quadrant segments which will be used
   * to approximate angle fillets in round endcaps and joins.
   *
   * @return the number of quadrant segments
   */
  fun getQuadrantSegments(): Int {
    return quadrantSegments
  }

  /**
   * Sets the number of line segments in a quarter-circle
   * used to approximate angle fillets in round endcaps and joins.
   * The value should be at least 1.
   *
   * @param quadSegs the number of segments in a fillet for a circle quadrant
   */
  fun setQuadrantSegments(quadSegs: Int) {
    quadrantSegments = quadSegs
  }

  /**
   * Gets the end cap style.
   *
   * @return the end cap style code
   */
  fun getEndCapStyle(): Int {
    return endCapStyle
  }

  /**
   * Specifies the end cap style of the generated buffer.
   * The styles supported are [CAP_ROUND], [CAP_FLAT], and [CAP_SQUARE].
   * The default is [CAP_ROUND].
   *
   * @param endCapStyle the code for the end cap style
   */
  fun setEndCapStyle(endCapStyle: Int) {
    this.endCapStyle = endCapStyle
  }

  /**
   * Gets the join style.
   *
   * @return the join style code
   */
  fun getJoinStyle(): Int {
    return joinStyle
  }

  /**
   * Sets the join style for outside (reflex) corners between line segments.
   * The styles supported are [JOIN_ROUND], [JOIN_MITRE] and JOIN_BEVEL.
   * The default is [JOIN_ROUND].
   *
   * @param joinStyle the code for the join style
   */
  fun setJoinStyle(joinStyle: Int) {
    this.joinStyle = joinStyle
  }

  /**
   * Gets the mitre ratio limit.
   *
   * @return the limit value
   */
  fun getMitreLimit(): Double {
    return mitreLimit
  }

  /**
   * Sets the limit on the mitre ratio used for very sharp corners.
   *
   * @param mitreLimit the mitre ratio limit
   */
  fun setMitreLimit(mitreLimit: Double) {
    this.mitreLimit = mitreLimit
  }

  /**
   * Sets whether the computed buffer should be single-sided.
   *
   * @param isSingleSided true if a single-sided buffer should be constructed
   */
  fun setSingleSided(isSingleSided: Boolean) {
    this.singleSided = isSingleSided
  }

  /**
   * Tests whether the buffer is to be generated on a single side only.
   *
   * @return true if the generated buffer is to be single-sided
   */
  fun isSingleSided(): Boolean {
    return singleSided
  }

  /**
   * Gets the simplify factor.
   *
   * @return the simplify factor
   */
  fun getSimplifyFactor(): Double {
    return simplifyFactor
  }

  /**
   * Sets the factor used to determine the simplify distance tolerance
   * for input simplification.
   *
   * @param simplifyFactor a value greater than or equal to zero.
   */
  fun setSimplifyFactor(simplifyFactor: Double) {
    this.simplifyFactor = if (simplifyFactor < 0) 0.0 else simplifyFactor
  }

  fun copy(): BufferParameters {
    val bp = BufferParameters()
    bp.quadrantSegments = quadrantSegments
    bp.endCapStyle = endCapStyle
    bp.joinStyle = joinStyle
    bp.mitreLimit = mitreLimit
    return bp
  }

  companion object {
    /**
     * Specifies a round line buffer end cap style.
     */
    const val CAP_ROUND = 1

    /**
     * Specifies a flat line buffer end cap style.
     */
    const val CAP_FLAT = 2

    /**
     * Specifies a square line buffer end cap style.
     */
    const val CAP_SQUARE = 3

    /**
     * Specifies a round join style.
     */
    const val JOIN_ROUND = 1

    /**
     * Specifies a mitre join style.
     */
    const val JOIN_MITRE = 2

    /**
     * Specifies a bevel join style.
     */
    const val JOIN_BEVEL = 3

    /**
     * The default number of facets into which to divide a fillet of 90 degrees.
     * A value of 8 gives less than 2% max error in the buffer distance.
     * For a max error of < 1%, use QS = 12.
     * For a max error of < 0.1%, use QS = 18.
     */
    const val DEFAULT_QUADRANT_SEGMENTS = 8

    /**
     * The default mitre limit
     * Allows fairly pointy mitres.
     */
    const val DEFAULT_MITRE_LIMIT = 5.0

    /**
     * The default simplify factor
     * Provides an accuracy of about 1%, which matches the accuracy of the default Quadrant Segments parameter.
     */
    const val DEFAULT_SIMPLIFY_FACTOR = 0.01

    /**
     * Computes the maximum distance error due to a given level
     * of approximation to a true arc.
     *
     * @param quadSegs the number of segments used to approximate a quarter-circle
     * @return the error of approximation
     */
    @JvmStatic
    fun bufferDistanceError(quadSegs: Int): Double {
      val alpha = Angle.PI_OVER_2 / quadSegs
      return 1 - cos(alpha / 2.0)
    }
  }
}
