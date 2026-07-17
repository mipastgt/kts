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
package org.locationtech.jts.geom

import kotlin.jvm.JvmStatic
import kotlin.jvm.JvmField
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.ln
import kotlin.math.roundToLong

/**
 * Specifies the precision model of the [Coordinate]s in a [Geometry].
 * In other words, specifies the grid of allowable points for a `Geometry`.
 * A precision model may be **floating** ([FLOATING] or [FLOATING_SINGLE]),
 * in which case normal floating-point value semantics apply.
 * 
 * For a [FIXED] precision model the [makePrecise] method allows rounding a coordinate to
 * a "precise" value; that is, one whose
 *  precision is known exactly.
 *
 * Coordinates are assumed to be precise in geometries.
 * That is, the coordinates are assumed to be rounded to the
 * precision model given for the geometry.
 * All internal operations
 * assume that coordinates are rounded to the precision model.
 * Constructive methods (such as boolean operations) always round computed
 * coordinates to the appropriate precision model.
 * 
 * Three types of precision model are supported:
 * 
 * - FLOATING - represents full double precision floating point.
 * This is the default precision model used in JTS
 * - FLOATING_SINGLE - represents single precision floating point.
 * - FIXED - represents a model with a fixed number of decimal places.
 *  A Fixed Precision Model is specified by a **scale factor**.
 *  The scale factor specifies the size of the grid which numbers are rounded to.
 *  Input coordinates are mapped to fixed coordinates according to the following
 *  equations:
 *    <UL>
 *      <LI> jtsPt.x = round( (inputPt.x * scale ) / scale
 *      <LI> jtsPt.y = round( (inputPt.y * scale ) / scale
 *    </UL>
 * 
 * For example, to specify 3 decimal places of precision, use a scale factor
 * of 1000. To specify -3 decimal places of precision (i.e. rounding to
 * the nearest 1000), use a scale factor of 0.001.
 * 
 * It is also supported to specify a precise **grid size**
 * by providing it as a negative scale factor.
 * This allows setting a precise grid size rather than using a fractional scale,
 * which provides more accurate and robust rounding.
 * For example, to specify rounding to the nearest 1000 use a scale factor of -1000.
 * 
 * Coordinates are represented internally as Java double-precision values.
 * Java uses the IEEE-394 floating point standard, which
 * provides 53 bits of precision. (Thus the maximum precisely representable
 * *integer* is 9,007,199,254,740,992 - or almost 16 decimal digits of precision).
 *
 */
open class PrecisionModel : Comparable<Any?> {

  /**
   * The type of PrecisionModel this represents.
   */
  private var modelType: Type = FLOATING

  /**
   * The scale factor which determines the number of decimal places in fixed precision.
   */
  private var scale = 0.0

  /**
   * If non-zero, the precise grid size specified.
   * In this case, the scale is also valid and is computed from the grid size.
   * If zero, the scale is used to compute the grid size where needed.
   */
  private var gridSize = 0.0

  /**
   * Creates a `PrecisionModel` with a default precision
   * of FLOATING.
   */
  constructor() {
    // default is floating precision
    modelType = FLOATING
  }

  /**
   * Creates a `PrecisionModel` that specifies
   * an explicit precision model type.
   * If the model type is FIXED the scale factor will default to 1.
   *
   * @param modelType the type of the precision model
   */
  constructor(modelType: Type) {
    this.modelType = modelType
    if (modelType === FIXED) {
      setScale(1.0)
    }
  }

  /**
   *  Creates a `PrecisionModel` that specifies Fixed precision.
   *  Fixed-precision coordinates are represented as precise internal coordinates,
   *  which are rounded to the grid defined by the scale factor.
   *
   * @param  scale    amount by which to multiply a coordinate after subtracting
   *      the offset, to obtain a precise coordinate
   * @param  offsetX  not used.
   * @param  offsetY  not used.
   *
   * @deprecated offsets are no longer supported, since internal representation is rounded floating point
   */
  constructor(scale: Double, offsetX: Double, offsetY: Double) {
    modelType = FIXED
    setScale(scale)
  }

  /**
   *  Creates a `PrecisionModel` that specifies Fixed precision.
   *  Fixed-precision coordinates are represented as precise internal coordinates,
   *  which are rounded to the grid defined by the scale factor.
   *  The provided scale may be negative, to specify an exact grid size.
   *  The scale is then computed as the reciprocal.
   *
   * @param  scale amount by which to multiply a coordinate after subtracting
   *      the offset, to obtain a precise coordinate.  Must be non-zero.
   */
  constructor(scale: Double) {
    modelType = FIXED
    setScale(scale)
  }

  /**
   *  Copy constructor to create a new `PrecisionModel`
   *  from an existing one.
   */
  constructor(pm: PrecisionModel) {
    modelType = pm.modelType
    scale = pm.scale
    gridSize = pm.gridSize
  }

  /**
   * Tests whether the precision model supports floating point
   * @return `true` if the precision model supports floating point
   */
  fun isFloating(): Boolean {
    return modelType === FLOATING || modelType === FLOATING_SINGLE
  }

  /**
   * Returns the maximum number of significant digits provided by this
   * precision model.
   * Intended for use by routines which need to print out
   * decimal representations of precise values (such as [WKTWriter][org.locationtech.jts.io.WKTWriter]).
   * 
   * This method would be more correctly called
   * `getMinimumDecimalPlaces`,
   * since it actually computes the number of decimal places
   * that is required to correctly display the full
   * precision of an ordinate value.
   * 
   * Since it is difficult to compute the required number of
   * decimal places for scale factors which are not powers of 10,
   * the algorithm uses a very rough approximation in this case.
   * This has the side effect that for scale factors which are
   * powers of 10 the value returned is 1 greater than the true value.
   *
   *
   * @return the maximum number of decimal places provided by this precision model
   */
  fun getMaximumSignificantDigits(): Int {
    var maxSigDigits = 16
    if (modelType === FLOATING) {
      maxSigDigits = 16
    } else if (modelType === FLOATING_SINGLE) {
      maxSigDigits = 6
    } else if (modelType === FIXED) {
      maxSigDigits = 1 + ceil(ln(getScale()) / ln(10.0)).toInt()
    }
    return maxSigDigits
  }

  /**
   * Returns the scale factor used to specify a fixed precision model.
   * The number of decimal places of precision is
   * equal to the base-10 logarithm of the scale factor.
   * Non-integral and negative scale factors are supported.
   * Negative scale factors indicate that the places
   * of precision is to the left of the decimal point.
   *
   * @return the scale factor for the fixed precision model
   */
  fun getScale(): Double {
    return scale
  }

  /**
   * Computes the grid size for a fixed precision model.
   * This is equal to the reciprocal of the scale factor.
   * If the grid size has been set explicity (via a negative scale factor)
   * it will be returned.
   *
   * @return the grid size at a fixed precision scale.
   */
  fun gridSize(): Double {
    if (isFloating())
      return Double.NaN

    if (gridSize != 0.0)
      return gridSize
    return 1.0 / scale
  }

  /**
   * Gets the type of this precision model
   * @return the type of this precision model
   * @see Type
   */
  fun getType(): Type {
    return modelType
  }

  /**
   *  Sets the multiplying factor used to obtain a precise coordinate.
   * This method is private because PrecisionModel is an immutable (value) type.
   */
  private fun setScale(scale: Double) {
    /*
     * A negative scale indicates the grid size is being set.
     * The scale is set as well, as the reciprocal.
     */
    if (scale < 0) {
      gridSize = abs(scale)
      this.scale = 1.0 / gridSize
    } else {
      this.scale = abs(scale)
      /*
       * Leave gridSize as 0, to ensure it is computed using scale
       */
      gridSize = 0.0
    }
  }

  /**
   * Returns the x-offset used to obtain a precise coordinate.
   *
   * @return the amount by which to subtract the x-coordinate before
   *         multiplying by the scale
   * @deprecated Offsets are no longer used
   */
  fun getOffsetX(): Double {
    //We actually don't use offsetX and offsetY anymore ... [Jon Aquino]
    return 0.0
  }

  /**
   * Returns the y-offset used to obtain a precise coordinate.
   *
   * @return the amount by which to subtract the y-coordinate before
   *         multiplying by the scale
   * @deprecated Offsets are no longer used
   */
  fun getOffsetY(): Double {
    return 0.0
  }

  /**
   *  Sets `internal` to the precise representation of `external`.
   *
   * @param external the original coordinate
   * @param internal the coordinate whose values will be changed to the
   *                 precise representation of `external`
   * @deprecated use makePrecise instead
   */
  fun toInternal(external: Coordinate, internal: Coordinate) {
    if (isFloating()) {
      internal.x = external.x
      internal.y = external.y
    } else {
      internal.x = makePrecise(external.x)
      internal.y = makePrecise(external.y)
    }
    internal.setZ(external.getZ())
  }

  /**
   *  Returns the precise representation of `external`.
   *
   * @param  external  the original coordinate
   * @return           the coordinate whose values will be changed to the precise
   *      representation of `external`
   * @deprecated use makePrecise instead
   */
  fun toInternal(external: Coordinate): Coordinate {
    val internal = Coordinate(external)
    makePrecise(internal)
    return internal
  }

  /**
   *  Returns the external representation of `internal`.
   *
   * @param  internal  the original coordinate
   * @return           the coordinate whose values will be changed to the
   *      external representation of `internal`
   * @deprecated no longer needed, since internal representation is same as external representation
   */
  fun toExternal(internal: Coordinate): Coordinate {
    val external = Coordinate(internal)
    return external
  }

  /**
   *  Sets `external` to the external representation of `internal`.
   *
   * @param  internal  the original coordinate
   * @param  external  the coordinate whose values will be changed to the
   *      external representation of `internal`
   * @deprecated no longer needed, since internal representation is same as external representation
   */
  fun toExternal(internal: Coordinate, external: Coordinate) {
    external.x = internal.x
    external.y = internal.y
  }

  /**
   * Rounds a numeric value to the PrecisionModel grid.
   * Asymmetric Arithmetic Rounding is used, to provide
   * uniform rounding behaviour no matter where the number is
   * on the number line.
   * 
   * This method has no effect on NaN values.
   * 
   * **Note:** Java's `Math#rint` uses the "Banker's Rounding" algorithm,
   * which is not suitable for precision operations elsewhere in JTS.
   */
  fun makePrecise(`val`: Double): Double {
    // don't change NaN values
    if (`val`.isNaN()) return `val`

    if (modelType === FLOATING_SINGLE) {
      val floatSingleVal = `val`.toFloat()
      return floatSingleVal.toDouble()
    }
    if (modelType === FIXED) {
      if (gridSize > 0) {
        return (`val` / gridSize).roundToLong() * gridSize
      } else {
        return (`val` * scale).roundToLong() / scale
      }
    }
    // modelType == FLOATING - no rounding necessary
    return `val`
  }

  /**
   * Rounds a Coordinate to the PrecisionModel grid.
   */
  fun makePrecise(coord: Coordinate) {
    // optimization for full precision
    if (modelType === FLOATING) return

    coord.x = makePrecise(coord.x)
    coord.y = makePrecise(coord.y)
    //MD says it's OK that we're not makePrecise'ing the z [Jon Aquino]
  }

  override fun toString(): String {
    var description = "UNKNOWN"
    if (modelType === FLOATING) {
      description = "Floating"
    } else if (modelType === FLOATING_SINGLE) {
      description = "Floating-Single"
    } else if (modelType === FIXED) {
      description = "Fixed (Scale=" + getScale() + ")"
    }
    return description
  }

  override fun equals(other: Any?): Boolean {
    if (other !is PrecisionModel) {
      return false
    }
    val otherPrecisionModel = other
    return modelType === otherPrecisionModel.modelType &&
      scale == otherPrecisionModel.scale
  }

  /* (non-Javadoc)
   */
  override fun hashCode(): Int {
    val prime = 31
    var result = 1
    result = prime * result + modelType.hashCode()
    var temp: Long
    temp = scale.toBits()
    result = prime * result + (temp xor (temp ushr 32)).toInt()
    return result
  }

  /**
   *  Compares this [PrecisionModel] object with the specified object for order.
   * A PrecisionModel is greater than another if it provides greater precision.
   * The comparison is based on the value returned by the
   * [getMaximumSignificantDigits] method.
   * This comparison is not strictly accurate when comparing floating precision models
   * to fixed models; however, it is correct when both models are either floating or fixed.
   *
   * @param  o  the `PrecisionModel` with which this `PrecisionModel`
   *      is being compared
   * @return    a negative integer, zero, or a positive integer as this `PrecisionModel`
   *      is less than, equal to, or greater than the specified `PrecisionModel`
   */
  override fun compareTo(o: Any?): Int {
    val other = o as PrecisionModel

    val sigDigits = getMaximumSignificantDigits()
    val otherSigDigits = other.getMaximumSignificantDigits()
    return sigDigits.compareTo(otherSigDigits)
//    if (sigDigits > otherSigDigits)
//      return 1;
//    else if
//    if (modelType == FLOATING && other.modelType == FLOATING) return 0;
//    if (modelType == FLOATING && other.modelType != FLOATING) return 1;
//    if (modelType != FLOATING && other.modelType == FLOATING) return -1;
//    if (modelType == FIXED && other.modelType == FIXED) {
//      if (scale > other.scale)
//        return 1;
//      else if (scale < other.scale)
//        return -1;
//      else
//        return 0;
//    }
//    Assert.shouldNeverReachHere("Unknown Precision Model type encountered");
//    return 0;
  }

  /**
   * The types of Precision Model which JTS supports.
   */
  class Type(private val name: String) {
    init {
      nameToTypeMap.put(name, this)
    }

    override fun toString(): String {
      return name
    }

    /*
     * Ssee http://www.javaworld.com/javaworld/javatips/jw-javatip122.html
     */
    private fun readResolve(): Any? {
      return nameToTypeMap.get(name)
    }

    companion object {
      private val nameToTypeMap: MutableMap<Any?, Any?> = HashMap()
    }
  }

  companion object {
    /**
     * Determines which of two [PrecisionModel]s is the most precise
     * (allows the greatest number of significant digits).
     *
     * @param pm1 a PrecisionModel
     * @param pm2 a PrecisionModel
     * @return the PrecisionModel which is most precise
     */
    @JvmStatic
    fun mostPrecise(pm1: PrecisionModel, pm2: PrecisionModel): PrecisionModel {
      if (pm1.compareTo(pm2) >= 0)
        return pm1
      return pm2
    }


    /**
     * Fixed Precision indicates that coordinates have a fixed number of decimal places.
     * The number of decimal places is determined by the log10 of the scale factor.
     */
    @JvmField
    val FIXED = Type("FIXED")

    /**
     * Floating precision corresponds to the standard Java
     * double-precision floating-point representation, which is
     * based on the IEEE-754 standard
     */
    @JvmField
    val FLOATING = Type("FLOATING")

    /**
     * Floating single precision corresponds to the standard Java
     * single-precision floating-point representation, which is
     * based on the IEEE-754 standard
     */
    @JvmField
    val FLOATING_SINGLE = Type("FLOATING SINGLE")

    /**
     *  The maximum precise value representable in a double. Since IEE754
     *  double-precision numbers allow 53 bits of mantissa, the value is equal to
     *  2^53 - 1.  This provides *almost* 16 decimal digits of precision.
     */
    const val maximumPreciseValue = 9007199254740992.0
  }
}
