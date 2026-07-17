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

package org.locationtech.jts.geom.util

import kotlin.jvm.JvmStatic
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.CoordinateSequence
import org.locationtech.jts.geom.CoordinateSequenceFilter
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.util.Assert

/**
 * Represents an affine transformation on the 2D Cartesian plane.
 * It can be used to transform a [Coordinate] or [Geometry].
 * An affine transformation is a mapping of the 2D plane into itself
 * via a series of transformations of the following basic types:
 * 
 * - reflection (through a line)
 * - rotation (around the origin)
 * - scaling (relative to the origin)
 * - shearing (in both the X and Y directions)
 * - translation
 * 
 * In general, affine transformations preserve straightness and parallel lines,
 * but do not preserve distance or shape.
 *
 * @author Martin Davis
 *
 */
open class AffineTransformation : CoordinateSequenceFilter {

  // affine matrix entries
  // (bottom row is always [ 0 0 1 ])
  private var m00 = 0.0
  private var m01 = 0.0
  private var m02 = 0.0
  private var m10 = 0.0
  private var m11 = 0.0
  private var m12 = 0.0

  /**
   * Constructs a new identity transformation
   */
  constructor() {
    setToIdentity()
  }

  /**
   * Constructs a new transformation whose
   * matrix has the specified values.
   *
   * @param matrix an array containing the 6 values { m00, m01, m02, m10, m11, m12 }
   * @throws NullPointerException if matrix is null
   * @throws IndexOutOfBoundsException if matrix is too small
   */
  constructor(matrix: DoubleArray) {
    m00 = matrix[0]
    m01 = matrix[1]
    m02 = matrix[2]
    m10 = matrix[3]
    m11 = matrix[4]
    m12 = matrix[5]
  }

  /**
   * Constructs a new transformation whose
   * matrix has the specified values.
   *
   * @param m00 the entry for the [0, 0] element in the transformation matrix
   * @param m01 the entry for the [0, 1] element in the transformation matrix
   * @param m02 the entry for the [0, 2] element in the transformation matrix
   * @param m10 the entry for the [1, 0] element in the transformation matrix
   * @param m11 the entry for the [1, 1] element in the transformation matrix
   * @param m12 the entry for the [1, 2] element in the transformation matrix
   */
  constructor(
    m00: Double,
    m01: Double,
    m02: Double,
    m10: Double,
    m11: Double,
    m12: Double
  ) {
    setTransformation(m00, m01, m02, m10, m11, m12)
  }

  /**
   * Constructs a transformation which is
   * a copy of the given one.
   *
   * @param trans the transformation to copy
   */
  constructor(trans: AffineTransformation) {
    setTransformation(trans)
  }

  /**
   * Constructs a transformation
   * which maps the given source
   * points into the given destination points.
   *
   * @param src0 source point 0
   * @param src1 source point 1
   * @param src2 source point 2
   * @param dest0 the mapped point for source point 0
   * @param dest1 the mapped point for source point 1
   * @param dest2 the mapped point for source point 2
   *
   * @deprecated use AffineTransformationFactory
   */
  @Deprecated("use AffineTransformationFactory")
  constructor(
    src0: Coordinate,
    src1: Coordinate,
    src2: Coordinate,
    dest0: Coordinate,
    dest1: Coordinate,
    dest2: Coordinate
  ) {
    throw UnsupportedOperationException("Use AffineTransformationFactory instead")
  }

  /**
   * Sets this transformation to be the identity transformation.
   *
   * @return this transformation, with an updated matrix
   */
  fun setToIdentity(): AffineTransformation {
    m00 = 1.0;    m01 = 0.0;  m02 = 0.0
    m10 = 0.0;    m11 = 1.0;  m12 = 0.0
    return this
  }

  /**
   * Sets this transformation's matrix to have the given values.
   *
   * @return this transformation, with an updated matrix
   */
  fun setTransformation(
    m00: Double,
    m01: Double,
    m02: Double,
    m10: Double,
    m11: Double,
    m12: Double
  ): AffineTransformation {
    this.m00 = m00
    this.m01 = m01
    this.m02 = m02
    this.m10 = m10
    this.m11 = m11
    this.m12 = m12
    return this
  }

  /**
   * Sets this transformation to be a copy of the given one
   *
   * @param trans a transformation to copy
   * @return this transformation, with an updated matrix
   */
  fun setTransformation(trans: AffineTransformation): AffineTransformation {
    m00 = trans.m00;    m01 = trans.m01;  m02 = trans.m02
    m10 = trans.m10;    m11 = trans.m11;  m12 = trans.m12
    return this
  }

  /**
   * Gets an array containing the entries
   * of the transformation matrix.
   * Only the 6 non-trivial entries are returned,
   * in the sequence:
   * ```
   * m00, m01, m02, m10, m11, m12
   * ```
   *
   * @return an array of length 6
   */
  fun getMatrixEntries(): DoubleArray {
    return doubleArrayOf(m00, m01, m02, m10, m11, m12)
  }

  /**
   * Computes the determinant of the transformation matrix.
   *
   * @return the determinant of the transformation
   * @see getInverse
   */
  fun getDeterminant(): Double {
    return m00 * m11 - m01 * m10
  }

  /**
   * Computes the inverse of this transformation, if one
   * exists.
   *
   * @return a new inverse transformation
   * @throws NoninvertibleTransformationException
   * @see getDeterminant
   */
  @Throws(NoninvertibleTransformationException::class)
  fun getInverse(): AffineTransformation {
    val det = getDeterminant()
    if (det == 0.0)
      throw NoninvertibleTransformationException("Transformation is non-invertible")

    val im00 = m11 / det
    val im10 = -m10 / det
    val im01 = -m01 / det
    val im11 = m00 / det
    val im02 = (m01 * m12 - m02 * m11) / det
    val im12 = (-m00 * m12 + m10 * m02) / det

    return AffineTransformation(im00, im01, im02, im10, im11, im12)
  }

  /**
   * Explicitly computes the math for a reflection.  May not work.
   *
   * @return this transformation, with an updated matrix
   */
  fun setToReflectionBasic(x0: Double, y0: Double, x1: Double, y1: Double): AffineTransformation {
    if (x0 == x1 && y0 == y1) {
      throw IllegalArgumentException("Reflection line points must be distinct")
    }
    val dx = x1 - x0
    val dy = y1 - y0
    val d = hypot(dx, dy)
    val sin = dy / d
    val cos = dx / d
    val cs2 = 2 * sin * cos
    val c2s2 = cos * cos - sin * sin
    m00 = c2s2;   m01 = cs2;    m02 = 0.0
    m10 = cs2;    m11 = -c2s2;  m12 = 0.0
    return this
  }

  /**
   * Sets this transformation to be a reflection
   * about the line defined by a line `(x0,y0) - (x1,y1)`.
   *
   * @return this transformation, with an updated matrix
   */
  fun setToReflection(x0: Double, y0: Double, x1: Double, y1: Double): AffineTransformation {
    if (x0 == x1 && y0 == y1) {
      throw IllegalArgumentException("Reflection line points must be distinct")
    }
    // translate line vector to origin
    setToTranslation(-x0, -y0)

    // rotate vector to positive x axis direction
    val dx = x1 - x0
    val dy = y1 - y0
    val d = hypot(dx, dy)
    val sin = dy / d
    val cos = dx / d
    rotate(-sin, cos)
    // reflect about the x axis
    scale(1.0, -1.0)
    // rotate back
    rotate(sin, cos)
    // translate back
    translate(x0, y0)
    return this
  }

  /**
   * Sets this transformation to be a reflection
   * about the line defined by vector (x,y).
   *
   * @param x the x-component of the reflection line vector
   * @param y the y-component of the reflection line vector
   * @return this transformation, with an updated matrix
   */
  fun setToReflection(x: Double, y: Double): AffineTransformation {
    if (x == 0.0 && y == 0.0) {
      throw IllegalArgumentException("Reflection vector must be non-zero")
    }

    /*
     * Handle special case - x = y.
     * This case is specified explicitly to avoid roundoff error.
     */
    if (x == y) {
      m00 = 0.0
      m01 = 1.0
      m02 = 0.0
      m10 = 1.0
      m11 = 0.0
      m12 = 0.0
      return this
    }

    // rotate vector to positive x axis direction
    val d = hypot(x, y)
    val sin = y / d
    val cos = x / d
    rotate(-sin, cos)
    // reflect about the x-axis
    scale(1.0, -1.0)
    // rotate back
    rotate(sin, cos)
    return this
  }

  /**
   * Sets this transformation to be a rotation around the origin.
   *
   * @param theta the rotation angle, in radians
   * @return this transformation, with an updated matrix
   */
  fun setToRotation(theta: Double): AffineTransformation {
    setToRotation(sin(theta), cos(theta))
    return this
  }

  /**
   * Sets this transformation to be a rotation around the origin
   * by specifying the sin and cos of the rotation angle directly.
   *
   * @param sinTheta the sine of the rotation angle
   * @param cosTheta the cosine of the rotation angle
   * @return this transformation, with an updated matrix
   */
  fun setToRotation(sinTheta: Double, cosTheta: Double): AffineTransformation {
    m00 = cosTheta;    m01 = -sinTheta;  m02 = 0.0
    m10 = sinTheta;    m11 = cosTheta;   m12 = 0.0
    return this
  }

  /**
   * Sets this transformation to be a rotation
   * around a given point (x,y).
   *
   * @param theta the rotation angle, in radians
   * @param x the x-ordinate of the rotation point
   * @param y the y-ordinate of the rotation point
   * @return this transformation, with an updated matrix
   */
  fun setToRotation(theta: Double, x: Double, y: Double): AffineTransformation {
    setToRotation(sin(theta), cos(theta), x, y)
    return this
  }

  /**
   * Sets this transformation to be a rotation
   * around a given point (x,y)
   * by specifying the sin and cos of the rotation angle directly.
   *
   * @param sinTheta the sine of the rotation angle
   * @param cosTheta the cosine of the rotation angle
   * @param x the x-ordinate of the rotation point
   * @param y the y-ordinate of the rotation point
   * @return this transformation, with an updated matrix
   */
  fun setToRotation(sinTheta: Double, cosTheta: Double, x: Double, y: Double): AffineTransformation {
    m00 = cosTheta;    m01 = -sinTheta;  m02 = x - x * cosTheta + y * sinTheta
    m10 = sinTheta;    m11 = cosTheta;   m12 = y - x * sinTheta - y * cosTheta
    return this
  }

  /**
   * Sets this transformation to be a scaling.
   *
   * @param xScale the amount to scale x-ordinates by
   * @param yScale the amount to scale y-ordinates by
   * @return this transformation, with an updated matrix
   */
  fun setToScale(xScale: Double, yScale: Double): AffineTransformation {
    m00 = xScale;   m01 = 0.0;      m02 = 0.0
    m10 = 0.0;      m11 = yScale;   m12 = 0.0
    return this
  }

  /**
   * Sets this transformation to be a shear.
   *
   * @param xShear the x component to shear by
   * @param yShear the y component to shear by
   * @return this transformation, with an updated matrix
   */
  fun setToShear(xShear: Double, yShear: Double): AffineTransformation {
    m00 = 1.0;      m01 = xShear;      m02 = 0.0
    m10 = yShear;   m11 = 1.0;         m12 = 0.0
    return this
  }

  /**
   * Sets this transformation to be a translation.
   *
   * @param dx the x component to translate by
   * @param dy the y component to translate by
   * @return this transformation, with an updated matrix
   */
  fun setToTranslation(dx: Double, dy: Double): AffineTransformation {
    m00 = 1.0;  m01 = 0.0; m02 = dx
    m10 = 0.0;  m11 = 1.0; m12 = dy
    return this
  }

  /**
   * Updates the value of this transformation
   * to that of a reflection transformation composed
   * with the current value.
   *
   * @return this transformation, with an updated matrix
   */
  fun reflect(x0: Double, y0: Double, x1: Double, y1: Double): AffineTransformation {
    compose(reflectionInstance(x0, y0, x1, y1))
    return this
  }

  /**
   * Updates the value of this transformation
   * to that of a reflection transformation composed
   * with the current value.
   *
   * @return this transformation, with an updated matrix
   */
  fun reflect(x: Double, y: Double): AffineTransformation {
    compose(reflectionInstance(x, y))
    return this
  }

  /**
   * Updates the value of this transformation
   * to that of a rotation transformation composed
   * with the current value.
   *
   * @param theta the angle to rotate by, in radians
   * @return this transformation, with an updated matrix
   */
  fun rotate(theta: Double): AffineTransformation {
    compose(rotationInstance(theta))
    return this
  }

  /**
   * Updates the value of this transformation
   * to that of a rotation around the origin composed
   * with the current value,
   * with the sin and cos of the rotation angle specified directly.
   *
   * @param sinTheta the sine of the angle to rotate by
   * @param cosTheta the cosine of the angle to rotate by
   * @return this transformation, with an updated matrix
   */
  fun rotate(sinTheta: Double, cosTheta: Double): AffineTransformation {
    compose(rotationInstance(sinTheta, cosTheta))
    return this
  }

  /**
   * Updates the value of this transformation
   * to that of a rotation around a given point composed
   * with the current value.
   *
   * @param theta the angle to rotate by, in radians
   * @param x the x-ordinate of the rotation point
   * @param y the y-ordinate of the rotation point
   * @return this transformation, with an updated matrix
   */
  fun rotate(theta: Double, x: Double, y: Double): AffineTransformation {
    compose(rotationInstance(theta, x, y))
    return this
  }

  /**
   * Updates the value of this transformation
   * to that of a rotation around a given point composed
   * with the current value,
   * with the sin and cos of the rotation angle specified directly.
   *
   * @param sinTheta the sine of the angle to rotate by
   * @param cosTheta the cosine of the angle to rotate by
   * @param x the x-ordinate of the rotation point
   * @param y the y-ordinate of the rotation point
   * @return this transformation, with an updated matrix
   */
  fun rotate(sinTheta: Double, cosTheta: Double, x: Double, y: Double): AffineTransformation {
    compose(rotationInstance(sinTheta, cosTheta, x, y))
    return this
  }

  /**
   * Updates the value of this transformation
   * to that of a scale transformation composed
   * with the current value.
   *
   * @param xScale the value to scale by in the x direction
   * @param yScale the value to scale by in the y direction
   * @return this transformation, with an updated matrix
   */
  fun scale(xScale: Double, yScale: Double): AffineTransformation {
    compose(scaleInstance(xScale, yScale))
    return this
  }

  /**
   * Updates the value of this transformation
   * to that of a shear transformation composed
   * with the current value.
   *
   * @param xShear the value to shear by in the x direction
   * @param yShear the value to shear by in the y direction
   * @return this transformation, with an updated matrix
   */
  fun shear(xShear: Double, yShear: Double): AffineTransformation {
    compose(shearInstance(xShear, yShear))
    return this
  }

  /**
   * Updates the value of this transformation
   * to that of a translation transformation composed
   * with the current value.
   *
   * @param x the value to translate by in the x direction
   * @param y the value to translate by in the y direction
   * @return this transformation, with an updated matrix
   */
  fun translate(x: Double, y: Double): AffineTransformation {
    compose(translationInstance(x, y))
    return this
  }

  /**
   * Updates this transformation to be
   * the composition of this transformation with the given [AffineTransformation].
   *
   * @param trans an affine transformation
   * @return this transformation, with an updated matrix
   */
  fun compose(trans: AffineTransformation): AffineTransformation {
    val mp00 = trans.m00 * m00 + trans.m01 * m10
    val mp01 = trans.m00 * m01 + trans.m01 * m11
    val mp02 = trans.m00 * m02 + trans.m01 * m12 + trans.m02
    val mp10 = trans.m10 * m00 + trans.m11 * m10
    val mp11 = trans.m10 * m01 + trans.m11 * m11
    val mp12 = trans.m10 * m02 + trans.m11 * m12 + trans.m12
    m00 = mp00
    m01 = mp01
    m02 = mp02
    m10 = mp10
    m11 = mp11
    m12 = mp12
    return this
  }

  /**
   * Updates this transformation to be the composition
   * of a given [AffineTransformation] with this transformation.
   *
   * @param trans an affine transformation
   * @return this transformation, with an updated matrix
   */
  fun composeBefore(trans: AffineTransformation): AffineTransformation {
    val mp00 = m00 * trans.m00 + m01 * trans.m10
    val mp01 = m00 * trans.m01 + m01 * trans.m11
    val mp02 = m00 * trans.m02 + m01 * trans.m12 + m02
    val mp10 = m10 * trans.m00 + m11 * trans.m10
    val mp11 = m10 * trans.m01 + m11 * trans.m11
    val mp12 = m10 * trans.m02 + m11 * trans.m12 + m12
    m00 = mp00
    m01 = mp01
    m02 = mp02
    m10 = mp10
    m11 = mp11
    m12 = mp12
    return this
  }

  /**
   * Applies this transformation to the `src` coordinate
   * and places the results in the `dest` coordinate
   * (which may be the same as the source).
   *
   * @param src the coordinate to transform
   * @param dest the coordinate to accept the results
   * @return the `dest` coordinate
   */
  fun transform(src: Coordinate, dest: Coordinate): Coordinate {
    val xp = m00 * src.x + m01 * src.y + m02
    val yp = m10 * src.x + m11 * src.y + m12
    dest.x = xp
    dest.y = yp
    return dest
  }

  /**
   * Creates a new [Geometry] which is the result
   * of this transformation applied to the input Geometry.
   *
   *@param g  a `Geometry`
   *@return a transformed Geometry
   */
  fun transform(g: Geometry): Geometry {
    val g2 = g.copy()
    g2.apply(this)
    return g2
  }

  /**
   * Applies this transformation to the i'th coordinate
   * in the given CoordinateSequence.
   *
   *@param seq  a `CoordinateSequence`
   *@param i the index of the coordinate to transform
   */
  fun transform(seq: CoordinateSequence, i: Int) {
    val xp = m00 * seq.getOrdinate(i, 0) + m01 * seq.getOrdinate(i, 1) + m02
    val yp = m10 * seq.getOrdinate(i, 0) + m11 * seq.getOrdinate(i, 1) + m12
    seq.setOrdinate(i, 0, xp)
    seq.setOrdinate(i, 1, yp)
  }

  /**
   * Transforms the i'th coordinate in the input sequence
   *
   *@param seq  a `CoordinateSequence`
   *@param i the index of the coordinate to transform
   */
  override fun filter(seq: CoordinateSequence, i: Int) {
    transform(seq, i)
  }

  override fun isGeometryChanged(): Boolean {
    return true
  }

  /**
   * Reports that this filter should continue to be executed until
   * all coordinates have been transformed.
   *
   * @return false
   */
  override fun isDone(): Boolean {
    return false
  }

  /**
   * Tests if this transformation is the identity transformation.
   *
   * @return true if this is the identity transformation
   */
  fun isIdentity(): Boolean {
    return (m00 == 1.0 && m01 == 0.0 && m02 == 0.0 &&
        m10 == 0.0 && m11 == 1.0 && m12 == 0.0)
  }

  /**
   * Tests if an object is an
   * `AffineTransformation`
   * and has the same matrix as
   * this transformation.
   *
   * @param obj an object to test
   * @return true if the given object is equal to this object
   */
  override fun equals(obj: Any?): Boolean {
    if (obj == null) return false
    if (obj !is AffineTransformation)
      return false

    val trans = obj
    return m00 == trans.m00 &&
        m01 == trans.m01 &&
        m02 == trans.m02 &&
        m10 == trans.m10 &&
        m11 == trans.m11 &&
        m12 == trans.m12
  }

  /* (non-Javadoc)
   */
  override fun hashCode(): Int {
    val prime = 31
    var result = 1
    var temp: Long
    temp = m00.toBits()
    result = prime * result + (temp xor (temp ushr 32)).toInt()
    temp = m01.toBits()
    result = prime * result + (temp xor (temp ushr 32)).toInt()
    temp = m02.toBits()
    result = prime * result + (temp xor (temp ushr 32)).toInt()
    temp = m10.toBits()
    result = prime * result + (temp xor (temp ushr 32)).toInt()
    temp = m11.toBits()
    result = prime * result + (temp xor (temp ushr 32)).toInt()
    temp = m12.toBits()
    result = prime * result + (temp xor (temp ushr 32)).toInt()
    return result
  }

  /**
   * Gets a text representation of this transformation.
   * The string is of the form:
   * ```
   * AffineTransformation[[m00, m01, m02], [m10, m11, m12]]
   * ```
   *
   * @return a string representing this transformation
   *
   */
  override fun toString(): String {
    return "AffineTransformation[[" + m00 + ", " + m01 + ", " + m02 +
        "], [" +
        m10 + ", " + m11 + ", " + m12 + "]]"
  }

  /**
   * Clones this transformation
   *
   * @return a copy of this transformation
   */
  fun clone(): Any {
    return AffineTransformation(this)
  }

  companion object {
    /**
     * Creates a transformation for a reflection about the
     * line (x0,y0) - (x1,y1).
     *
     * @return a transformation for the reflection
     */
    @JvmStatic
    fun reflectionInstance(x0: Double, y0: Double, x1: Double, y1: Double): AffineTransformation {
      val trans = AffineTransformation()
      trans.setToReflection(x0, y0, x1, y1)
      return trans
    }

    /**
     * Creates a transformation for a reflection about the
     * line (0,0) - (x,y).
     *
     * @return a transformation for the reflection
     */
    @JvmStatic
    fun reflectionInstance(x: Double, y: Double): AffineTransformation {
      val trans = AffineTransformation()
      trans.setToReflection(x, y)
      return trans
    }

    /**
     * Creates a transformation for a rotation
     * about the origin
     * by an angle *theta*.
     *
     * @param theta the rotation angle, in radians
     * @return a transformation for the rotation
     */
    @JvmStatic
    fun rotationInstance(theta: Double): AffineTransformation {
      return rotationInstance(sin(theta), cos(theta))
    }

    /**
     * Creates a transformation for a rotation
     * by an angle *theta*,
     * specified by the sine and cosine of the angle.
     *
     * @param sinTheta the sine of the rotation angle
     * @param cosTheta the cosine of the rotation angle
     * @return a transformation for the rotation
     */
    @JvmStatic
    fun rotationInstance(sinTheta: Double, cosTheta: Double): AffineTransformation {
      val trans = AffineTransformation()
      trans.setToRotation(sinTheta, cosTheta)
      return trans
    }

    /**
     * Creates a transformation for a rotation
     * about the point (x,y) by an angle *theta*.
     *
     * @param theta the rotation angle, in radians
     * @param x the x-ordinate of the rotation point
     * @param y the y-ordinate of the rotation point
     * @return a transformation for the rotation
     */
    @JvmStatic
    fun rotationInstance(theta: Double, x: Double, y: Double): AffineTransformation {
      return rotationInstance(sin(theta), cos(theta), x, y)
    }

    /**
     * Creates a transformation for a rotation
     * about the point (x,y) by an angle *theta*,
     * specified by the sine and cosine of the angle.
     *
     * @param sinTheta the sine of the rotation angle
     * @param cosTheta the cosine of the rotation angle
     * @param x the x-ordinate of the rotation point
     * @param y the y-ordinate of the rotation point
     * @return a transformation for the rotation
     */
    @JvmStatic
    fun rotationInstance(sinTheta: Double, cosTheta: Double, x: Double, y: Double): AffineTransformation {
      val trans = AffineTransformation()
      trans.setToRotation(sinTheta, cosTheta, x, y)
      return trans
    }

    /**
     * Creates a transformation for a scaling relative to the origin.
     *
     * @param xScale the value to scale by in the x direction
     * @param yScale the value to scale by in the y direction
     * @return a transformation for the scaling
     */
    @JvmStatic
    fun scaleInstance(xScale: Double, yScale: Double): AffineTransformation {
      val trans = AffineTransformation()
      trans.setToScale(xScale, yScale)
      return trans
    }

    /**
     * Creates a transformation for a scaling relative to the point (x,y).
     *
     * @param xScale the value to scale by in the x direction
     * @param yScale the value to scale by in the y direction
     * @param x the x-ordinate of the point to scale around
     * @param y the y-ordinate of the point to scale around
     * @return a transformation for the scaling
     */
    @JvmStatic
    fun scaleInstance(xScale: Double, yScale: Double, x: Double, y: Double): AffineTransformation {
      val trans = AffineTransformation()
      trans.translate(-x, -y)
      trans.scale(xScale, yScale)
      trans.translate(x, y)
      return trans
    }

    /**
     * Creates a transformation for a shear.
     *
     * @param xShear the value to shear by in the x direction
     * @param yShear the value to shear by in the y direction
     * @return a transformation for the shear
     */
    @JvmStatic
    fun shearInstance(xShear: Double, yShear: Double): AffineTransformation {
      val trans = AffineTransformation()
      trans.setToShear(xShear, yShear)
      return trans
    }

    /**
     * Creates a transformation for a translation.
     *
     * @param x the value to translate by in the x direction
     * @param y the value to translate by in the y direction
     * @return a transformation for the translation
     */
    @JvmStatic
    fun translationInstance(x: Double, y: Double): AffineTransformation {
      val trans = AffineTransformation()
      trans.setToTranslation(x, y)
      return trans
    }
  }
}
