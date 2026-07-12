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
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.pow

import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.Polygon
import org.locationtech.jts.geom.PrecisionModel
import org.locationtech.jts.geom.TopologyException
import org.locationtech.jts.math.MathUtil
import org.locationtech.jts.noding.Noder
import org.locationtech.jts.noding.ScaledNoder
import org.locationtech.jts.noding.snapround.SnapRoundingNoder

/**
 * Computes the buffer of a geometry, for both positive and negative buffer distances.
 * <p>
 * The buffer operation always returns a polygonal result.
 * The negative or zero-distance buffer of lines and points is always an empty [Polygon].
 *
 * @version 1.7
 */
class BufferOp {

  private var argGeom: Geometry
  private var distance = 0.0

  private var bufParams = BufferParameters()

  private var resultGeometry: Geometry? = null
  private var saveException: RuntimeException? = null // debugging only
  private var isInvertOrientation = false

  /**
   * Initializes a buffer computation for the given geometry
   *
   * @param g the geometry to buffer
   */
  constructor(g: Geometry) {
    argGeom = g
  }

  /**
   * Initializes a buffer computation for the given geometry
   * with the given set of parameters
   *
   * @param g the geometry to buffer
   * @param bufParams the buffer parameters to use
   */
  constructor(g: Geometry, bufParams: BufferParameters) {
    argGeom = g
    this.bufParams = bufParams
  }

  /**
   * Specifies the end cap style of the generated buffer.
   *
   * @param endCapStyle the end cap style to specify
   */
  fun setEndCapStyle(endCapStyle: Int) {
    bufParams.setEndCapStyle(endCapStyle)
  }

  /**
   * Sets the number of line segments in a quarter-circle
   * used to approximate angle fillets for round end caps and joins.
   *
   * @param quadrantSegments the number of segments in a fillet for a quadrant
   */
  fun setQuadrantSegments(quadrantSegments: Int) {
    bufParams.setQuadrantSegments(quadrantSegments)
  }

  /**
   * Returns the buffer computed for a geometry for a given buffer distance.
   *
   * @param distance the buffer distance
   * @return the buffer of the input geometry
   */
  fun getResultGeometry(distance: Double): Geometry {
    this.distance = distance
    computeGeometry()
    return resultGeometry!!
  }

  private fun computeGeometry() {
    bufferOriginalPrecision()
    if (resultGeometry != null) return

    val argPM = argGeom.getFactory().getPrecisionModel()
    if (argPM.getType() == PrecisionModel.FIXED) bufferFixedPrecision(argPM)
    else bufferReducedPrecision()
  }

  private fun bufferReducedPrecision() {
    // try and compute with decreasing precision
    var precDigits = MAX_PRECISION_DIGITS
    while (precDigits >= 0) {
      try {
        bufferReducedPrecision(precDigits)
      } catch (ex: TopologyException) {
        // update the saved exception to reflect the new input geometry
        saveException = ex
        // don't propagate the exception - it will be detected by fact that resultGeometry is null
      }
      if (resultGeometry != null) return
      precDigits--
    }

    // tried everything - have to bail
    throw saveException!!
  }

  private fun bufferReducedPrecision(precisionDigits: Int) {
    val sizeBasedScaleFactor = precisionScaleFactor(argGeom, distance, precisionDigits)

    val fixedPM = PrecisionModel(sizeBasedScaleFactor)
    bufferFixedPrecision(fixedPM)
  }

  private fun bufferOriginalPrecision() {
    try {
      // use fast noding by default
      val bufBuilder = createBufferBullder()
      resultGeometry = bufBuilder.buffer(argGeom, distance)
    } catch (ex: RuntimeException) {
      saveException = ex
      // don't propagate the exception - it will be detected by fact that resultGeometry is null
    }
  }

  private fun createBufferBullder(): BufferBuilder {
    val bufBuilder = BufferBuilder(bufParams)
    bufBuilder.setInvertOrientation(isInvertOrientation)
    return bufBuilder
  }

  private fun bufferFixedPrecision(fixedPM: PrecisionModel) {
    /*
     * Snap-Rounding provides both robustness
     * and a fixed output precision.
     */
    val snapNoder: Noder = SnapRoundingNoder(PrecisionModel(1.0))
    val noder: Noder = ScaledNoder(snapNoder, fixedPM.getScale())

    val bufBuilder = createBufferBullder()
    bufBuilder.setWorkingPrecisionModel(fixedPM)
    bufBuilder.setNoder(noder)
    // this may throw an exception, if robustness errors are encountered
    resultGeometry = bufBuilder.buffer(argGeom, distance)
  }

  companion object {
    /**
     * Specifies a round line buffer end cap style.
     */
    @Deprecated("use BufferParameters")
    const val CAP_ROUND = BufferParameters.CAP_ROUND

    /**
     * Specifies a butt (or flat) line buffer end cap style.
     */
    @Deprecated("use BufferParameters")
    const val CAP_BUTT = BufferParameters.CAP_FLAT

    /**
     * Specifies a butt (or flat) line buffer end cap style.
     */
    @Deprecated("use BufferParameters")
    const val CAP_FLAT = BufferParameters.CAP_FLAT

    /**
     * Specifies a square line buffer end cap style.
     */
    @Deprecated("use BufferParameters")
    const val CAP_SQUARE = BufferParameters.CAP_SQUARE

    /**
     * A number of digits of precision which leaves some computational "headroom"
     * for floating point operations.
     */
    private const val MAX_PRECISION_DIGITS = 12

    /**
     * Compute a scale factor to limit the precision of
     * a given combination of Geometry and buffer distance.
     *
     * @return a scale factor for the buffer computation
     */
    private fun precisionScaleFactor(g: Geometry, distance: Double, maxPrecisionDigits: Int): Double {
      val env = g.getEnvelopeInternal()
      val envMax = MathUtil.max(
        abs(env.getMaxX()),
        abs(env.getMaxY()),
        abs(env.getMinX()),
        abs(env.getMinY())
      )

      val expandByDistance = if (distance > 0.0) distance else 0.0
      val bufEnvMax = envMax + 2 * expandByDistance

      // the smallest power of 10 greater than the buffer envelope
      val bufEnvPrecisionDigits = (ln(bufEnvMax) / ln(10.0) + 1.0).toInt()
      val minUnitLog10 = maxPrecisionDigits - bufEnvPrecisionDigits

      val scaleFactor = (10.0).pow(minUnitLog10.toDouble())
      return scaleFactor
    }

    /**
     * Computes the buffer of a geometry for a given buffer distance.
     *
     * @param g the geometry to buffer
     * @param distance the buffer distance
     * @return the buffer of the input geometry
     */
    @JvmStatic
    fun bufferOp(g: Geometry, distance: Double): Geometry {
      val gBuf = BufferOp(g)
      val geomBuf = gBuf.getResultGeometry(distance)
      return geomBuf
    }

    /**
     * Computes the buffer for a geometry for a given buffer distance
     * and accuracy of approximation.
     *
     * @param g the geometry to buffer
     * @param distance the buffer distance
     * @param params the buffer parameters to use
     * @return the buffer of the input geometry
     */
    @JvmStatic
    fun bufferOp(g: Geometry, distance: Double, params: BufferParameters): Geometry {
      val bufOp = BufferOp(g, params)
      val geomBuf = bufOp.getResultGeometry(distance)
      return geomBuf
    }

    /**
     * Computes the buffer for a geometry for a given buffer distance
     * and accuracy of approximation.
     *
     * @param g the geometry to buffer
     * @param distance the buffer distance
     * @param quadrantSegments the number of segments used to approximate a quarter circle
     * @return the buffer of the input geometry
     */
    @JvmStatic
    fun bufferOp(g: Geometry, distance: Double, quadrantSegments: Int): Geometry {
      val bufOp = BufferOp(g)
      bufOp.setQuadrantSegments(quadrantSegments)
      val geomBuf = bufOp.getResultGeometry(distance)
      return geomBuf
    }

    /**
     * Computes the buffer for a geometry for a given buffer distance
     * and accuracy of approximation.
     *
     * @param g the geometry to buffer
     * @param distance the buffer distance
     * @param quadrantSegments the number of segments used to approximate a quarter circle
     * @param endCapStyle the end cap style to use
     * @return the buffer of the input geometry
     */
    @JvmStatic
    fun bufferOp(g: Geometry, distance: Double, quadrantSegments: Int, endCapStyle: Int): Geometry {
      val bufOp = BufferOp(g)
      bufOp.setQuadrantSegments(quadrantSegments)
      bufOp.setEndCapStyle(endCapStyle)
      val geomBuf = bufOp.getResultGeometry(distance)
      return geomBuf
    }

    /**
     * Buffers a geometry with distance zero.
     * The result can be computed using the maximum-signed-area orientation,
     * or by combining both orientations.
     * <p>
     * This function is for INTERNAL use only.
     *
     * @param geom the polygonal geometry to buffer by zero
     * @param isBothOrientations true if both orientations of input rings should be used
     * @return the buffered polygonal geometry
     */
    @JvmStatic
    fun bufferByZero(geom: Geometry, isBothOrientations: Boolean): Geometry {
      //--- compute buffer using maximum signed-area orientation
      val buf0 = geom.buffer(0.0)
      if (!isBothOrientations) return buf0

      //-- compute buffer using minimum signed-area orientation
      val op = BufferOp(geom)
      op.isInvertOrientation = true
      val buf0Inv = op.getResultGeometry(0.0)

      //-- the buffer results should be non-adjacent, so combining is safe
      return combine(buf0, buf0Inv)
    }

    /**
     * Combines the elements of two polygonal geometries together.
     * The input geometries must be non-adjacent, to avoid
     * creating an invalid result.
     *
     * @param poly0 a polygonal geometry (which may be empty)
     * @param poly1 a polygonal geometry (which may be empty)
     * @return a combined polygonal geometry
     */
    private fun combine(poly0: Geometry, poly1: Geometry): Geometry {
      // short-circuit - handles case where geometry is valid
      if (poly1.isEmpty()) return poly0
      if (poly0.isEmpty()) return poly1

      val polys = ArrayList<Polygon>()
      extractPolygons(poly0, polys)
      extractPolygons(poly1, polys)
      if (polys.size == 1) return polys[0]
      return poly0.getFactory().createMultiPolygon(GeometryFactory.toPolygonArray(polys))
    }

    private fun extractPolygons(poly0: Geometry, polys: MutableList<Polygon>) {
      for (i in 0 until poly0.getNumGeometries()) {
        polys.add(poly0.getGeometryN(i) as Polygon)
      }
    }
  }
}
