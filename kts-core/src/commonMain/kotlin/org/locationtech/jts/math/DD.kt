/*
 * Copyright (c) 2016 Martin Davis.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.locationtech.jts.math

import kotlin.jvm.JvmStatic
import kotlin.jvm.JvmField
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow


/**
 * Implements extended-precision floating-point numbers
 * which maintain 106 bits (approximately 30 decimal digits) of precision.
 * 
 * A DoubleDouble uses a representation containing two double-precision values.
 * A number x is represented as a pair of doubles, x.hi and x.lo,
 * such that the number represented by x is x.hi + x.lo, where
 * ```
 *    |x.lo| <= 0.5*ulp(x.hi)
 * ```
 * and ulp(y) means "unit in the last place of y".
 * The basic arithmetic operations are implemented using
 * convenient properties of IEEE-754 floating-point arithmetic.
 * 
 * The range of values which can be represented is the same as in IEEE-754.
 * The precision of the representable numbers
 * is twice as great as IEEE-754 double precision.
 * 
 * The correctness of the arithmetic algorithms relies on operations
 * being performed with standard IEEE-754 double precision and rounding.
 * This is the Java standard arithmetic model, but for performance reasons
 * Java implementations are not
 * constrained to using this standard by default.
 * Some processors (notably the Intel Pentium architecture) perform
 * floating point operations in (non-IEEE-754-standard) extended-precision.
 * A JVM implementation may choose to use the non-standard extended-precision
 * as its default arithmetic mode.
 * To prevent this from happening, this code uses the
 * Java `strictfp` modifier,
 * which forces all operations to take place in the standard IEEE-754 rounding model.
 * 
 * The API provides both a set of value-oriented operations
 * and a set of mutating operations.
 * Value-oriented operations treat DoubleDouble values as
 * immutable; operations on them return new objects carrying the result
 * of the operation.  This provides a simple and safe semantics for
 * writing DoubleDouble expressions.  However, there is a performance
 * penalty for the object allocations required.
 * The mutable interface updates object values in-place.
 * It provides optimum memory performance, but requires
 * care to ensure that aliasing errors are not created
 * and constant values are not changed.
 * 
 * For example, the following code example constructs three DD instances:
 * two to hold the input values and one to hold the result of the addition.
 * ```
 *     val a = DD(2.0)
 *     val b = DD(3.0)
 *     val c = a.add(b)
 * ```
 * In contrast, the following approach uses only one object:
 * ```
 *     val a = DD(2.0)
 *     a.selfAdd(3.0)
 * ```
 * 
 * This implementation uses algorithms originally designed variously by
 * Knuth, Kahan, Dekker, and Linnainmaa.
 * Douglas Priest developed the first C implementation of these techniques.
 * Other more recent C++ implementation are due to Keith M. Briggs and David Bailey et al.
 *
 * <h3>References</h3>
 * 
 * - Priest, D., *Algorithms for Arbitrary Precision Floating Point Arithmetic*,
 * in P. Kornerup and D. Matula, Eds., Proc. 10th Symposium on Computer Arithmetic,
 * IEEE Computer Society Press, Los Alamitos, Calif., 1991.
 * - Yozo Hida, Xiaoye S. Li and David H. Bailey,
 * *Quad-Double Arithmetic: Algorithms, Implementation, and Application*,
 * manuscript, Oct 2000; Lawrence Berkeley National Laboratory Report BNL-46996.
 * - David Bailey, *High Precision Software Directory*;
 * `http://crd.lbl.gov/~dhbailey/mpdist/index.html`
 * 
 *
 *
 * @author Martin Davis
 *
 */
class DD : Comparable<Any?> {

  /**
   * The high-order component of the double-double precision value.
   */
  private var hi = 0.0

  /**
   * The low-order component of the double-double precision value.
   */
  private var lo = 0.0

  /**
   * Creates a new DoubleDouble with value 0.0.
   */
  constructor() {
    init(0.0)
  }

  /**
   * Creates a new DoubleDouble with value x.
   *
   * @param x the value to initialize
   */
  constructor(x: Double) {
    init(x)
  }

  /**
   * Creates a new DoubleDouble with value (hi, lo).
   *
   * @param hi the high-order component
   * @param lo the high-order component
   */
  constructor(hi: Double, lo: Double) {
    init(hi, lo)
  }

  /**
   * Creates a new DoubleDouble with value equal to the argument.
   *
   * @param dd the value to initialize
   */
  constructor(dd: DD) {
    init(dd)
  }

  /**
   * Creates a new DoubleDouble with value equal to the argument.
   *
   * @param str the value to initialize by
   * @throws NumberFormatException if `str` is not a valid representation of a number
   */
  constructor(str: String) : this(parse(str))

  /**
   * Creates and returns a copy of this value.
   *
   * @return a copy of this value
   */
  fun clone(): Any {
    return DD(this)
  }

  private fun init(x: Double) {
    this.hi = x
    this.lo = 0.0
  }

  private fun init(hi: Double, lo: Double) {
    this.hi = hi
    this.lo = lo
  }

  private fun init(dd: DD) {
    hi = dd.hi
    lo = dd.lo
  }

  /*
  double getHighComponent() { return hi; }

  double getLowComponent() { return lo; }
  */

  // Testing only - should not be public
  /*
  public void RENORM()
  {
    double s = hi + lo;
    double err = lo - (s - hi);
    hi = s;
    lo = err;
  }
  */

  /**
   * Set the value for the DD object. This method supports the mutating
   * operations concept described in the class documentation (see above).
   * @param value a DD instance supplying an extended-precision value.
   * @return a self-reference to the DD instance.
   */
  fun setValue(value: DD): DD {
    init(value)
    return this
  }

  /**
   * Set the value for the DD object. This method supports the mutating
   * operations concept described in the class documentation (see above).
   * @param value a floating point value to be stored in the instance.
   * @return a self-reference to the DD instance.
   */
  fun setValue(value: Double): DD {
    init(value)
    return this
  }

  /**
   * Returns a new DoubleDouble whose value is `(this + y)`.
   *
   * @param y the addend
   * @return `(this + y)`
   */
  fun add(y: DD): DD {
    return copy(this).selfAdd(y)
  }

  /**
   * Returns a new DoubleDouble whose value is `(this + y)`.
   *
   * @param y the addend
   * @return `(this + y)`
   */
  fun add(y: Double): DD {
    return copy(this).selfAdd(y)
  }

  /**
   * Adds the argument to the value of `this`.
   * To prevent altering constants,
   * this method **must only** be used on values known to
   * be newly created.
   *
   * @param y the addend
   * @return this object, increased by y
   */
  fun selfAdd(y: DD): DD {
    return selfAdd(y.hi, y.lo)
  }

  /**
   * Adds the argument to the value of `this`.
   * To prevent altering constants,
   * this method **must only** be used on values known to
   * be newly created.
   *
   * @param y the addend
   * @return this object, increased by y
   */
  fun selfAdd(y: Double): DD {
    var H: Double
    var h: Double
    var S: Double
    var s: Double
    var e: Double
    var f: Double
    S = hi + y
    e = S - hi
    s = S - e
    s = (y - e) + (hi - s)
    f = s + lo
    H = S + f
    h = f + (S - H)
    hi = H + h
    lo = h + (H - hi)
    return this
    // return selfAdd(y, 0.0);
  }

  private fun selfAdd(yhi: Double, ylo: Double): DD {
    var H: Double
    var h: Double
    var T: Double
    var t: Double
    var S: Double
    var s: Double
    var e: Double
    var f: Double
    S = hi + yhi
    T = lo + ylo
    e = S - hi
    f = T - lo
    s = S - e
    t = T - f
    s = (yhi - e) + (hi - s)
    t = (ylo - f) + (lo - t)
    e = s + T; H = S + e; h = e + (S - H); e = t + h

    val zhi = H + e
    val zlo = e + (H - zhi)
    hi = zhi
    lo = zlo
    return this
  }

  /**
   * Computes a new DoubleDouble object whose value is `(this - y)`.
   *
   * @param y the subtrahend
   * @return `(this - y)`
   */
  fun subtract(y: DD): DD {
    return add(y.negate())
  }

  /**
   * Computes a new DoubleDouble object whose value is `(this - y)`.
   *
   * @param y the subtrahend
   * @return `(this - y)`
   */
  fun subtract(y: Double): DD {
    return add(-y)
  }

  /**
   * Subtracts the argument from the value of `this`.
   * To prevent altering constants,
   * this method **must only** be used on values known to
   * be newly created.
   *
   * @param y the addend
   * @return this object, decreased by y
   */
  fun selfSubtract(y: DD): DD {
    if (isNaN()) return this
    return selfAdd(-y.hi, -y.lo)
  }

  /**
   * Subtracts the argument from the value of `this`.
   * To prevent altering constants,
   * this method **must only** be used on values known to
   * be newly created.
   *
   * @param y the addend
   * @return this object, decreased by y
   */
  fun selfSubtract(y: Double): DD {
    if (isNaN()) return this
    return selfAdd(-y, 0.0)
  }

  /**
   * Returns a new DoubleDouble whose value is `-this`.
   *
   * @return `-this`
   */
  fun negate(): DD {
    if (isNaN()) return this
    return DD(-hi, -lo)
  }

  /**
   * Returns a new DoubleDouble whose value is `(this * y)`.
   *
   * @param y the multiplicand
   * @return `(this * y)`
   */
  fun multiply(y: DD): DD {
    if (y.isNaN()) return createNaN()
    return copy(this).selfMultiply(y)
  }

  /**
   * Returns a new DoubleDouble whose value is `(this * y)`.
   *
   * @param y the multiplicand
   * @return `(this * y)`
   */
  fun multiply(y: Double): DD {
    if (y.isNaN()) return createNaN()
    return copy(this).selfMultiply(y, 0.0)
  }

  /**
   * Multiplies this object by the argument, returning `this`.
   * To prevent altering constants,
   * this method **must only** be used on values known to
   * be newly created.
   *
   * @param y the value to multiply by
   * @return this object, multiplied by y
   */
  fun selfMultiply(y: DD): DD {
    return selfMultiply(y.hi, y.lo)
  }

  /**
   * Multiplies this object by the argument, returning `this`.
   * To prevent altering constants,
   * this method **must only** be used on values known to
   * be newly created.
   *
   * @param y the value to multiply by
   * @return this object, multiplied by y
   */
  fun selfMultiply(y: Double): DD {
    return selfMultiply(y, 0.0)
  }

  private fun selfMultiply(yhi: Double, ylo: Double): DD {
    var hx: Double
    var tx: Double
    var hy: Double
    var ty: Double
    var C: Double
    var c: Double
    C = SPLIT * hi; hx = C - hi; c = SPLIT * yhi
    hx = C - hx; tx = hi - hx; hy = c - yhi
    C = hi * yhi; hy = c - hy; ty = yhi - hy
    c = ((((hx * hy - C) + hx * ty) + tx * hy) + tx * ty) + (hi * ylo + lo * yhi)
    val zhi = C + c; hx = C - zhi
    val zlo = c + hx
    hi = zhi
    lo = zlo
    return this
  }

  /**
   * Computes a new DoubleDouble whose value is `(this / y)`.
   *
   * @param y the divisor
   * @return a new object with the value `(this / y)`
   */
  fun divide(y: DD): DD {
    var hc: Double
    var tc: Double
    var hy: Double
    var ty: Double
    var C: Double
    var c: Double
    var U: Double
    var u: Double
    C = hi / y.hi; c = SPLIT * C; hc = c - C; u = SPLIT * y.hi; hc = c - hc
    tc = C - hc; hy = u - y.hi; U = C * y.hi; hy = u - hy; ty = y.hi - hy
    u = (((hc * hy - U) + hc * ty) + tc * hy) + tc * ty
    c = ((((hi - U) - u) + lo) - C * y.lo) / y.hi
    u = C + c

    val zhi = u
    val zlo = (C - u) + c
    return DD(zhi, zlo)
  }

  /**
   * Computes a new DoubleDouble whose value is `(this / y)`.
   *
   * @param y the divisor
   * @return a new object with the value `(this / y)`
   */
  fun divide(y: Double): DD {
    if (y.isNaN()) return createNaN()
    return copy(this).selfDivide(y, 0.0)
  }

  /**
   * Divides this object by the argument, returning `this`.
   * To prevent altering constants,
   * this method **must only** be used on values known to
   * be newly created.
   *
   * @param y the value to divide by
   * @return this object, divided by y
   */
  fun selfDivide(y: DD): DD {
    return selfDivide(y.hi, y.lo)
  }

  /**
   * Divides this object by the argument, returning `this`.
   * To prevent altering constants,
   * this method **must only** be used on values known to
   * be newly created.
   *
   * @param y the value to divide by
   * @return this object, divided by y
   */
  fun selfDivide(y: Double): DD {
    return selfDivide(y, 0.0)
  }

  private fun selfDivide(yhi: Double, ylo: Double): DD {
    var hc: Double
    var tc: Double
    var hy: Double
    var ty: Double
    var C: Double
    var c: Double
    var U: Double
    var u: Double
    C = hi / yhi; c = SPLIT * C; hc = c - C; u = SPLIT * yhi; hc = c - hc
    tc = C - hc; hy = u - yhi; U = C * yhi; hy = u - hy; ty = yhi - hy
    u = (((hc * hy - U) + hc * ty) + tc * hy) + tc * ty
    c = ((((hi - U) - u) + lo) - C * ylo) / yhi
    u = C + c

    hi = u
    lo = (C - u) + c
    return this
  }

  /**
   * Returns a DoubleDouble whose value is  `1 / this`.
   *
   * @return the reciprocal of this value
   */
  fun reciprocal(): DD {
    var hc: Double
    var tc: Double
    var hy: Double
    var ty: Double
    var C: Double
    var c: Double
    var U: Double
    var u: Double
    C = 1.0 / hi
    c = SPLIT * C
    hc = c - C
    u = SPLIT * hi
    hc = c - hc; tc = C - hc; hy = u - hi; U = C * hi; hy = u - hy; ty = hi - hy
    u = (((hc * hy - U) + hc * ty) + tc * hy) + tc * ty
    c = ((((1.0 - U) - u)) - C * lo) / hi

    val zhi = C + c
    val zlo = (C - zhi) + c
    return DD(zhi, zlo)
  }

  /**
   * Returns the largest (closest to positive infinity)
   * value that is not greater than the argument
   * and is equal to a mathematical integer.
   * Special cases:
   * 
   * - If this value is NaN, returns NaN.
   * 
   *
   * @return the largest (closest to positive infinity)
   * value that is not greater than the argument
   * and is equal to a mathematical integer.
   */
  fun floor(): DD {
    if (isNaN()) return NaN
    val fhi = floor(hi)
    var flo = 0.0
    // Hi is already integral.  Floor the low word
    if (fhi == hi) {
      flo = floor(lo)
    }
    // do we need to renormalize here?
    return DD(fhi, flo)
  }

  /**
   * Returns the smallest (closest to negative infinity) value
   * that is not less than the argument and is equal to a mathematical integer.
   * Special cases:
   * 
   * - If this value is NaN, returns NaN.
   * 
   *
   * @return the smallest (closest to negative infinity) value
   * that is not less than the argument and is equal to a mathematical integer.
   */
  fun ceil(): DD {
    if (isNaN()) return NaN
    val fhi = ceil(hi)
    var flo = 0.0
    // Hi is already integral.  Ceil the low word
    if (fhi == hi) {
      flo = ceil(lo)
      // do we need to renormalize here?
    }
    return DD(fhi, flo)
  }

  /**
   * Returns an integer indicating the sign of this value.
   * 
   * - if this value is > 0, returns 1
   * - if this value is < 0, returns -1
   * - if this value is = 0, returns 0
   * - if this value is NaN, returns 0
   * 
   *
   * @return an integer indicating the sign of this value
   */
  fun signum(): Int {
    if (hi > 0) return 1
    if (hi < 0) return -1
    if (lo > 0) return 1
    if (lo < 0) return -1
    return 0
  }

  /**
   * Rounds this value to the nearest integer.
   * The value is rounded to an integer by adding 1/2 and taking the floor of the result.
   * Special cases:
   * 
   * - If this value is NaN, returns NaN.
   * 
   *
   * @return this value rounded to the nearest integer
   */
  fun rint(): DD {
    if (isNaN()) return this
    // may not be 100% correct
    val plus5 = this.add(0.5)
    return plus5.floor()
  }

  /**
   * Returns the integer which is largest in absolute value and not further
   * from zero than this value.
   * Special cases:
   * 
   * - If this value is NaN, returns NaN.
   * 
   *
   * @return the integer which is largest in absolute value and not further from zero than this value
   */
  fun trunc(): DD {
    if (isNaN()) return NaN
    if (isPositive())
      return floor()
    else
      return ceil()
  }

  /**
   * Returns the absolute value of this value.
   * Special cases:
   * 
   * - If this value is NaN, it is returned.
   * 
   *
   * @return the absolute value of this value
   */
  fun abs(): DD {
    if (isNaN()) return NaN
    if (isNegative())
      return negate()
    return DD(this)
  }

  /**
   * Computes the square of this value.
   *
   * @return the square of this value.
   */
  fun sqr(): DD {
    return this.multiply(this)
  }

  /**
   * Squares this object.
   * To prevent altering constants,
   * this method **must only** be used on values known to
   * be newly created.
   *
   * @return the square of this value.
   */
  fun selfSqr(): DD {
    return this.selfMultiply(this)
  }

  /**
   * Computes the positive square root of this value.
   * If the number is NaN or negative, NaN is returned.
   *
   * @return the positive square root of this number.
   * If the argument is NaN or less than zero, the result is NaN.
   */
  fun sqrt(): DD {
    /* Strategy:  Use Karp's trick:  if x is an approximation
    to sqrt(a), then

       sqrt(a) = a*x + [a - (a*x)^2] * x / 2   (approx)

    The approximation is accurate to twice the accuracy of x.
    Also, the multiplication (a*x) and [-]*x can be done with
    only half the precision.
 */

    if (isZero())
      return valueOf(0.0)

    if (isNegative()) {
      return NaN
    }

    val x = 1.0 / kotlin.math.sqrt(hi)
    val ax = hi * x

    val axdd = valueOf(ax)
    val diffSq = this.subtract(axdd.sqr())
    val d2 = diffSq.hi * (x * 0.5)

    return axdd.add(d2)
  }

  /**
   * Computes the value of this number raised to an integral power.
   * Follows semantics of Java pow as closely as possible.
   *
   * @param exp the integer exponent
   * @return x raised to the integral power exp
   */
  fun pow(exp: Int): DD {
    if (exp.toDouble() == 0.0)
      return valueOf(1.0)

    var r = DD(this)
    var s = valueOf(1.0)
    var n = abs(exp)

    if (n > 1) {
      /* Use binary exponentiation */
      while (n > 0) {
        if (n % 2 == 1) {
          s.selfMultiply(r)
        }
        n /= 2
        if (n > 0)
          r = r.sqr()
      }
    } else {
      s = r
    }

    /* Compute the reciprocal if n is negative. */
    if (exp < 0)
      return s.reciprocal()
    return s
  }

  /*------------------------------------------------------------
   *   Ordering Functions
   *------------------------------------------------------------
   */

  /**
   * Computes the minimum of this and another DD number.
   *
   * @param x a DD number
   * @return the minimum of the two numbers
   */
  fun min(x: DD): DD {
    if (this.le(x)) {
      return this
    } else {
      return x
    }
  }

  /**
   * Computes the maximum of this and another DD number.
   *
   * @param x a DD number
   * @return the maximum of the two numbers
   */
  fun max(x: DD): DD {
    if (this.ge(x)) {
      return this
    } else {
      return x
    }
  }

  /*------------------------------------------------------------
   *   Conversion Functions
   *------------------------------------------------------------
   */

  /**
   * Converts this value to the nearest double-precision number.
   *
   * @return the nearest double-precision number to this value
   */
  fun doubleValue(): Double {
    return hi + lo
  }

  /**
   * Converts this value to the nearest integer.
   *
   * @return the nearest integer to this value
   */
  fun intValue(): Int {
    return hi.toInt()
  }

  /*------------------------------------------------------------
   *   Predicates
   *------------------------------------------------------------
   */

  /**
   * Tests whether this value is equal to 0.
   *
   * @return true if this value is equal to 0
   */
  fun isZero(): Boolean {
    return hi == 0.0 && lo == 0.0
  }

  /**
   * Tests whether this value is less than 0.
   *
   * @return true if this value is less than 0
   */
  fun isNegative(): Boolean {
    return hi < 0.0 || (hi == 0.0 && lo < 0.0)
  }

  /**
   * Tests whether this value is greater than 0.
   *
   * @return true if this value is greater than 0
   */
  fun isPositive(): Boolean {
    return hi > 0.0 || (hi == 0.0 && lo > 0.0)
  }

  /**
   * Tests whether this value is NaN.
   *
   * @return true if this value is NaN
   */
  fun isNaN(): Boolean {
    return hi.isNaN()
  }

  /**
   * Tests whether this value is equal to another `DoubleDouble` value.
   *
   * @param y a DoubleDouble value
   * @return true if this value = y
   */
  fun equals(y: DD): Boolean {
    return hi == y.hi && lo == y.lo
  }

  /**
   * Tests whether this value is greater than another `DoubleDouble` value.
   * @param y a DoubleDouble value
   * @return true if this value > y
   */
  fun gt(y: DD): Boolean {
    return (hi > y.hi) || (hi == y.hi && lo > y.lo)
  }

  /**
   * Tests whether this value is greater than or equals to another `DoubleDouble` value.
   * @param y a DoubleDouble value
   * @return true if this value >= y
   */
  fun ge(y: DD): Boolean {
    return (hi > y.hi) || (hi == y.hi && lo >= y.lo)
  }

  /**
   * Tests whether this value is less than another `DoubleDouble` value.
   * @param y a DoubleDouble value
   * @return true if this value < y
   */
  fun lt(y: DD): Boolean {
    return (hi < y.hi) || (hi == y.hi && lo < y.lo)
  }

  /**
   * Tests whether this value is less than or equal to another `DoubleDouble` value.
   * @param y a DoubleDouble value
   * @return true if this value <= y
   */
  fun le(y: DD): Boolean {
    return (hi < y.hi) || (hi == y.hi && lo <= y.lo)
  }

  /**
   * Compares two DoubleDouble objects numerically.
   *
   * @return -1,0 or 1 depending on whether this value is less than, equal to
   * or greater than the value of `o`
   */
  override fun compareTo(o: Any?): Int {
    val other = o as DD

    if (hi < other.hi) return -1
    if (hi > other.hi) return 1
    if (lo < other.lo) return -1
    if (lo > other.lo) return 1
    return 0
  }

  /*------------------------------------------------------------
   *   Output
   *------------------------------------------------------------
   */

  /**
   * Dumps the components of this number to a string.
   *
   * @return a string showing the components of the number
   */
  fun dump(): String {
    return "DD<" + hi + ", " + lo + ">"
  }

  /**
   * Returns a string representation of this number, in either standard or scientific notation.
   * If the magnitude of the number is in the range [ 10^-3, 10^8 ]
   * standard notation will be used.  Otherwise, scientific notation will be used.
   *
   * @return a string representation of this number
   */
  override fun toString(): String {
    val mag = magnitude(hi)
    if (mag >= -3 && mag <= 20)
      return toStandardNotation()
    return toSciNotation()
  }

  /**
   * Returns the string representation of this value in standard notation.
   *
   * @return the string representation in standard notation
   */
  fun toStandardNotation(): String {
    val specialStr = getSpecialNumberString()
    if (specialStr != null)
      return specialStr

    val magnitude = IntArray(1)
    val sigDigits = extractSignificantDigits(true, magnitude)
    val decimalPointPos = magnitude[0] + 1

    var num = sigDigits
    // add a leading 0 if the decimal point is the first char
    if (sigDigits[0] == '.') {
      num = "0" + sigDigits
    } else if (decimalPointPos < 0) {
      num = "0." + stringOfChar('0', -decimalPointPos) + sigDigits
    } else if (sigDigits.indexOf('.') == -1) {
      // no point inserted - sig digits must be smaller than magnitude of number
      // add zeroes to end to make number the correct size
      val numZeroes = decimalPointPos - sigDigits.length
      val zeroes = stringOfChar('0', numZeroes)
      num = sigDigits + zeroes + ".0"
    }

    if (this.isNegative())
      return "-" + num
    return num
  }

  /**
   * Returns the string representation of this value in scientific notation.
   *
   * @return the string representation in scientific notation
   */
  fun toSciNotation(): String {
    // special case zero, to allow as
    if (isZero())
      return SCI_NOT_ZERO

    val specialStr = getSpecialNumberString()
    if (specialStr != null)
      return specialStr

    val magnitude = IntArray(1)
    val digits = extractSignificantDigits(false, magnitude)
    val expStr = SCI_NOT_EXPONENT_CHAR + magnitude[0]

    // should never have leading zeroes
    // MD - is this correct?  Or should we simply strip them if they are present?
    if (digits[0] == '0') {
      throw IllegalStateException("Found leading zero: " + digits)
    }

    // add decimal point
    var trailingDigits = ""
    if (digits.length > 1)
      trailingDigits = digits.substring(1)
    val digitsWithDecimal = digits[0] + "." + trailingDigits

    if (this.isNegative())
      return "-" + digitsWithDecimal + expStr
    return digitsWithDecimal + expStr
  }

  /**
   * Extracts the significant digits in the decimal representation of the argument.
   * A decimal point may be optionally inserted in the string of digits
   * (as long as its position lies within the extracted digits
   * - if not, the caller must prepend or append the appropriate zeroes and decimal point).
   *
   * @param insertDecimalPoint whether to insert a decimal point into the digit string
   * @param magnitude an array of length 1 in which the decimal magnitude of the value is returned
   * @return the string containing the significant digits and possibly a decimal point
   */
  private fun extractSignificantDigits(insertDecimalPoint: Boolean, magnitude: IntArray): String {
    var y = this.abs()
    // compute *correct* magnitude of y
    var mag = magnitude(y.hi)
    val scale = TEN.pow(mag)
    y = y.divide(scale)

    // fix magnitude if off by one
    if (y.gt(TEN)) {
      y = y.divide(TEN)
      mag += 1
    } else if (y.lt(ONE)) {
      y = y.multiply(TEN)
      mag -= 1
    }

    val decimalPointPos = mag + 1
    val buf = StringBuilder()
    val numDigits = MAX_PRINT_DIGITS - 1
    for (i in 0..numDigits) {
      if (insertDecimalPoint && i == decimalPointPos) {
        buf.append('.')
      }
      val digit = y.hi.toInt()
//      System.out.println("printDump: [" + i + "] digit: " + digit + "  y: " + y.dump() + "  buf: " + buf);

      /*
       * This should never happen, due to heuristic checks on remainder below
       */
      if (digit < 0 || digit > 9) {
//        System.out.println("digit > 10 : " + digit);
//        throw new IllegalStateException("Internal errror: found digit = " + digit);
      }
      /*
       * If a negative remainder is encountered, simply terminate the extraction.
       * This is robust, but maybe slightly inaccurate.
       * My current hypothesis is that negative remainders only occur for very small lo components,
       * so the inaccuracy is tolerable
       */
      if (digit < 0) {
        break
        // throw new IllegalStateException("Internal errror: found digit = " + digit);
      }
      var rebiasBy10 = false
      var digitChar: Char = 0.toChar()
      if (digit > 9) {
        // set flag to re-bias after next 10-shift
        rebiasBy10 = true
        // output digit will end up being '9'
        digitChar = '9'
      } else {
        digitChar = ('0' + digit)
      }
      buf.append(digitChar)
      y = (y.subtract(valueOf(digit.toDouble()))
        .multiply(TEN))
      if (rebiasBy10)
        y.selfAdd(TEN)

      var continueExtractingDigits = true
      /*
       * Heuristic check: if the remaining portion of
       * y is non-positive, assume that output is complete
       */
//      if (y.hi <= 0.0)
//        if (y.hi < 0.0)
//        continueExtractingDigits = false;
      /**
       * Check if remaining digits will be 0, and if so don't output them.
       * Do this by comparing the magnitude of the remainder with the expected precision.
       */
      val remMag = magnitude(y.hi)
      if (remMag < 0 && abs(remMag) >= (numDigits - i))
        continueExtractingDigits = false
      if (!continueExtractingDigits)
        break
    }
    magnitude[0] = mag
    return buf.toString()
  }

  /**
   * Returns the string for this value if it has a known representation.
   * (E.g. NaN or 0.0)
   *
   * @return the string for this special number
   * or null if the number is not a special number
   */
  private fun getSpecialNumberString(): String? {
    if (isZero()) return "0.0"
    if (isNaN()) return "NaN "
    return null
  }

  companion object {
    /**
     * The value nearest to the constant Pi.
     */
    @JvmField
    val PI = DD(
      3.141592653589793116e+00,
      1.224646799147353207e-16
    )

    /**
     * The value nearest to the constant 2 * Pi.
     */
    @JvmField
    val TWO_PI = DD(
      6.283185307179586232e+00,
      2.449293598294706414e-16
    )

    /**
     * The value nearest to the constant Pi / 2.
     */
    @JvmField
    val PI_2 = DD(
      1.570796326794896558e+00,
      6.123233995736766036e-17
    )

    /**
     * The value nearest to the constant e (the natural logarithm base).
     */
    @JvmField
    val E = DD(
      2.718281828459045091e+00,
      1.445646891729250158e-16
    )

    /**
     * A value representing the result of an operation which does not return a valid number.
     */
    @JvmField
    val NaN = DD(Double.NaN, Double.NaN)

    /**
     * The smallest representable relative difference between two {link @ DoubleDouble} values
     */
    const val EPS = 1.23259516440783e-32 /* = 2^-106 */

    private fun createNaN(): DD {
      return DD(Double.NaN, Double.NaN)
    }

    /**
     * Converts the string argument to a DoubleDouble number.
     *
     * @param str a string containing a representation of a numeric value
     * @return the extended precision version of the value
     * @throws NumberFormatException if `s` is not a valid representation of a number
     */
    @JvmStatic
    @Throws(NumberFormatException::class)
    fun valueOf(str: String): DD {
      return parse(str)
    }

    /**
     * Converts the `double` argument to a DoubleDouble number.
     *
     * @param x a numeric value
     * @return the extended precision version of the value
     */
    @JvmStatic
    fun valueOf(x: Double): DD {
      return DD(x)
    }

    /**
     * The value to split a double-precision value on during multiplication
     */
    private const val SPLIT = 134217729.0 // 2^27+1, for IEEE double

    /**
     * Creates a new DoubleDouble with the value of the argument.
     *
     * @param dd the DoubleDouble value to copy
     * @return a copy of the input value
     */
    @JvmStatic
    fun copy(dd: DD): DD {
      return DD(dd)
    }

    /**
     * Computes the square of this value.
     *
     * @return the square of this value.
     */
    @JvmStatic
    fun sqr(x: Double): DD {
      return valueOf(x).selfMultiply(x)
    }

    @JvmStatic
    fun sqrt(x: Double): DD {
      return valueOf(x).sqrt()
    }

    /**
     * Computes the determinant of the 2x2 matrix with the given entries.
     *
     * @param x1 a double value
     * @param y1 a double value
     * @param x2 a double value
     * @param y2 a double value
     * @return the determinant of the values
     */
    @JvmStatic
    fun determinant(x1: Double, y1: Double, x2: Double, y2: Double): DD {
      return determinant(valueOf(x1), valueOf(y1), valueOf(x2), valueOf(y2))
    }

    /**
     * Computes the determinant of the 2x2 matrix with the given entries.
     *
     * @param x1 a matrix entry
     * @param y1 a matrix entry
     * @param x2 a matrix entry
     * @param y2 a matrix entry
     * @return the determinant of the matrix of values
     */
    @JvmStatic
    fun determinant(x1: DD, y1: DD, x2: DD, y2: DD): DD {
      val det = x1.multiply(y2).selfSubtract(y1.multiply(x2))
      return det
    }

    private const val MAX_PRINT_DIGITS = 32
    private val TEN = valueOf(10.0)
    private val ONE = valueOf(1.0)
    private const val SCI_NOT_EXPONENT_CHAR = "E"
    private const val SCI_NOT_ZERO = "0.0E0"

    /**
     * Creates a string of a given length containing the given character
     *
     * @param ch the character to be repeated
     * @param len the len of the desired string
     * @return the string
     */
    private fun stringOfChar(ch: Char, len: Int): String {
      val buf = StringBuilder()
      for (i in 0 until len) {
        buf.append(ch)
      }
      return buf.toString()
    }

    /**
     * Determines the decimal magnitude of a number.
     * The magnitude is the exponent of the greatest power of 10 which is less than
     * or equal to the number.
     *
     * @param x the number to find the magnitude of
     * @return the decimal magnitude of x
     */
    private fun magnitude(x: Double): Int {
      val xAbs = abs(x)
      val xLog10 = ln(xAbs) / ln(10.0)
      var xMag = floor(xLog10).toInt()
      /**
       * Since log computation is inexact, there may be an off-by-one error
       * in the computed magnitude.
       * Following tests that magnitude is correct, and adjusts it if not
       */
      val xApprox = (10.0).pow(xMag.toDouble())
      if (xApprox * 10 <= xAbs)
        xMag += 1

      return xMag
    }

    /*------------------------------------------------------------
     *   Input
     *------------------------------------------------------------
     */

    /**
     * Converts a string representation of a real number into a DoubleDouble value.
     * The format accepted is similar to the standard Java real number syntax.
     * It is defined by the following regular expression:
     * ```
     * [`+`|`-`] {*digit*} [ `.` {*digit*} ] [ ( `e` | `E` ) [`+`|`-`] {*digit*}+
     * ```
     *
     * @param str the string to parse
     * @return the value of the parsed number
     * @throws NumberFormatException if `str` is not a valid representation of a number
     */
    @JvmStatic
    @Throws(NumberFormatException::class)
    fun parse(str: String): DD {
      var i = 0
      val strlen = str.length

      // skip leading whitespace
      while (str[i].isWhitespace())
        i++

      // check for sign
      var isNegative = false
      if (i < strlen) {
        val signCh = str[i]
        if (signCh == '-' || signCh == '+') {
          i++
          if (signCh == '-') isNegative = true
        }
      }

      // scan all digits and accumulate into an integral value
      // Keep track of the location of the decimal point (if any) to allow scaling later
      val `val` = DD()

      var numDigits = 0
      var numBeforeDec = 0
      var exp = 0
      var hasDecimalChar = false
      while (true) {
        if (i >= strlen)
          break
        val ch = str[i]
        i++
        if (ch.isDigit()) {
          val d = (ch - '0').toDouble()
          `val`.selfMultiply(TEN)
          // MD: need to optimize this
          `val`.selfAdd(d)
          numDigits++
          continue
        }
        if (ch == '.') {
          numBeforeDec = numDigits
          hasDecimalChar = true
          continue
        }
        if (ch == 'e' || ch == 'E') {
          val expStr = str.substring(i)
          // this should catch any format problems with the exponent
          try {
            exp = expStr.toInt()
          } catch (ex: NumberFormatException) {
            throw NumberFormatException("Invalid exponent " + expStr + " in string " + str)
          }
          break
        }
        throw NumberFormatException(
          "Unexpected character '" + ch
            + "' at position " + i
            + " in string " + str
        )
      }
      var val2 = `val`

      // correct number of digits before decimal sign if we don't have a decimal sign in the string
      if (!hasDecimalChar) numBeforeDec = numDigits

      // scale the number correctly
      val numDecPlaces = numDigits - numBeforeDec - exp
      if (numDecPlaces == 0) {
        val2 = `val`
      } else if (numDecPlaces > 0) {
        val scale = TEN.pow(numDecPlaces)
        val2 = `val`.divide(scale)
      } else if (numDecPlaces < 0) {
        val scale = TEN.pow(-numDecPlaces)
        val2 = `val`.multiply(scale)
      }
      // apply leading sign, if any
      if (isNegative) {
        return val2.negate()
      }
      return val2
    }
  }
}
