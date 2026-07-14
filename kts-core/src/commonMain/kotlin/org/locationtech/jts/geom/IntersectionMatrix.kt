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

/**
 * Models a <b>Dimensionally Extended Nine-Intersection Model (DE-9IM)</b> matrix.
 * DE-9IM matrix values (such as "212FF1FF2")
 * specify the topological relationship between two [Geometry]s.
 * This class can also represent matrix patterns (such as "T*T******")
 * which are used for matching instances of DE-9IM matrices.
 * 
 * DE-9IM matrices are 3x3 matrices with integer entries.
 * The matrix indices {0,1,2} represent the topological locations
 * that occur in a geometry (Interior, Boundary, Exterior).
 * These are provided by the constants
 * [Location.INTERIOR], [Location.BOUNDARY], and [Location.EXTERIOR].
 * 
 * When used to specify the topological relationship between two geometries,
 * the matrix entries represent the possible dimensions of each intersection:
 * [Dimension.A] = 2, [Dimension.L] = 1, [Dimension.P] = 0 and [Dimension.FALSE] = -1.
 * When used to represent a matrix pattern entries can have the additional values
 * [Dimension.TRUE] {"T") and [Dimension.DONTCARE] ("*").
 * 
 * For a description of the DE-9IM and the spatial predicates derived from it,
 * see the following references:
 * 
 * - <i><a href="http://www.opengis.org/techno/specs.htm">
 * OGC 99-049 OpenGIS Simple Features Specification for SQL</a></i>
 * , Section 2.1.13
 * - <i><a href="http://portal.opengeospatial.org/files/?artifact_id=25355">
 * OGC 06-103r4 OpenGIS Implementation Standard for Geographic information - Simple feature access - Part 1: Common architecture</a></i>
 * , Section 6.1.15 (which provides some further details on certain predicate specifications).
 * 
 * - Wikipedia article on <a href="https://en.wikipedia.org/wiki/DE-9IM">DE-9IM</a>
 * 
 * 
 * Methods are provided to:
 *  <UL>
 *    <LI>set and query the elements of the matrix in a convenient fashion
 *    <LI>convert to and from the standard string representation (specified in
 *    SFS Section 2.1.13.2).
 *    <LI>test if a matrix matches a given pattern string.
 *    - test if a matrix (possibly with geometry dimensions) matches a standard named spatial predicate
 *  </UL>
 *
 */
open class IntersectionMatrix {
  /**
   *  Internal representation of this `IntersectionMatrix`.
   */
  private var matrix: Array<IntArray> = Array(3) { IntArray(3) }

  /**
   *  Creates an `IntersectionMatrix` with `FALSE`
   *  dimension values.
   */
  constructor() {
    setAll(Dimension.FALSE)
  }

  /**
   *  Creates an `IntersectionMatrix` with the given dimension
   *  symbols.
   *
   * @param  elements  a String of nine dimension symbols in row major order
   */
  constructor(elements: String) : this() {
    set(elements)
  }

  /**
   *  Creates an `IntersectionMatrix` with the same elements as
   *  `other`.
   *
   * @param  other  an `IntersectionMatrix` to copy
   */
  constructor(other: IntersectionMatrix) : this() {
    matrix[Location.INTERIOR][Location.INTERIOR] = other.matrix[Location.INTERIOR][Location.INTERIOR]
    matrix[Location.INTERIOR][Location.BOUNDARY] = other.matrix[Location.INTERIOR][Location.BOUNDARY]
    matrix[Location.INTERIOR][Location.EXTERIOR] = other.matrix[Location.INTERIOR][Location.EXTERIOR]
    matrix[Location.BOUNDARY][Location.INTERIOR] = other.matrix[Location.BOUNDARY][Location.INTERIOR]
    matrix[Location.BOUNDARY][Location.BOUNDARY] = other.matrix[Location.BOUNDARY][Location.BOUNDARY]
    matrix[Location.BOUNDARY][Location.EXTERIOR] = other.matrix[Location.BOUNDARY][Location.EXTERIOR]
    matrix[Location.EXTERIOR][Location.INTERIOR] = other.matrix[Location.EXTERIOR][Location.INTERIOR]
    matrix[Location.EXTERIOR][Location.BOUNDARY] = other.matrix[Location.EXTERIOR][Location.BOUNDARY]
    matrix[Location.EXTERIOR][Location.EXTERIOR] = other.matrix[Location.EXTERIOR][Location.EXTERIOR]
  }

  /**
   * Adds one matrix to another.
   * Addition is defined by taking the maximum dimension value of each position
   * in the summand matrices.
   *
   * @param im the matrix to add
   */
  fun add(im: IntersectionMatrix) {
    for (i in 0 until 3) {
      for (j in 0 until 3) {
        setAtLeast(i, j, im.get(i, j))
      }
    }
  }

  /**
   *  Changes the value of one of this `IntersectionMatrix`s
   *  elements.
   *
   * @param  row             the row of this `IntersectionMatrix`,
   *      indicating the interior, boundary or exterior of the first `Geometry`
   * @param  column          the column of this `IntersectionMatrix`,
   *      indicating the interior, boundary or exterior of the second `Geometry`
   * @param  dimensionValue  the new value of the element
   */
  fun set(row: Int, column: Int, dimensionValue: Int) {
    matrix[row][column] = dimensionValue
  }

  /**
   *  Changes the elements of this `IntersectionMatrix` to the
   *  dimension symbols in `dimensionSymbols`.
   *
   * @param  dimensionSymbols  nine dimension symbols to which to set this `IntersectionMatrix`
   *      s elements. Possible values are `{T, F, * , 0, 1, 2}`
   */
  fun set(dimensionSymbols: String) {
    for (i in 0 until dimensionSymbols.length) {
      val row = i / 3
      val col = i % 3
      matrix[row][col] = Dimension.toDimensionValue(dimensionSymbols[i])
    }
  }

  /**
   *  Changes the specified element to `minimumDimensionValue` if the
   *  element is less.
   *
   * @param  row                    the row of this `IntersectionMatrix`
   *      , indicating the interior, boundary or exterior of the first `Geometry`
   * @param  column                 the column of this `IntersectionMatrix`
   *      , indicating the interior, boundary or exterior of the second `Geometry`
   * @param  minimumDimensionValue  the dimension value with which to compare the
   *      element. The order of dimension values from least to greatest is
   *      `{DONTCARE, TRUE, FALSE, 0, 1, 2}`.
   */
  fun setAtLeast(row: Int, column: Int, minimumDimensionValue: Int) {
    if (matrix[row][column] < minimumDimensionValue) {
      matrix[row][column] = minimumDimensionValue
    }
  }

  /**
   *  If row >= 0 and column >= 0, changes the specified element to `minimumDimensionValue`
   *  if the element is less. Does nothing if row <0 or column < 0.
   *
   * @param  row                    the row of this `IntersectionMatrix`
   *      , indicating the interior, boundary or exterior of the first `Geometry`
   * @param  column                 the column of this `IntersectionMatrix`
   *      , indicating the interior, boundary or exterior of the second `Geometry`
   * @param  minimumDimensionValue  the dimension value with which to compare the
   *      element. The order of dimension values from least to greatest is
   *      `{DONTCARE, TRUE, FALSE, 0, 1, 2}`.
   */
  fun setAtLeastIfValid(row: Int, column: Int, minimumDimensionValue: Int) {
    if (row >= 0 && column >= 0) {
      setAtLeast(row, column, minimumDimensionValue)
    }
  }

  /**
   *  For each element in this `IntersectionMatrix`, changes the
   *  element to the corresponding minimum dimension symbol if the element is
   *  less.
   *
   * @param  minimumDimensionSymbols  nine dimension symbols with which to
   *      compare the elements of this `IntersectionMatrix`. The
   *      order of dimension values from least to greatest is `{DONTCARE, TRUE, FALSE, 0, 1, 2}`
   *      .
   */
  fun setAtLeast(minimumDimensionSymbols: String) {
    for (i in 0 until minimumDimensionSymbols.length) {
      val row = i / 3
      val col = i % 3
      setAtLeast(row, col, Dimension.toDimensionValue(minimumDimensionSymbols[i]))
    }
  }

  /**
   *  Changes the elements of this `IntersectionMatrix` to `dimensionValue`
   *  .
   *
   * @param  dimensionValue  the dimension value to which to set this `IntersectionMatrix`
   *      s elements. Possible values `{TRUE, FALSE, DONTCARE, 0, 1, 2}`
   *      .
   */
  fun setAll(dimensionValue: Int) {
    for (ai in 0 until 3) {
      for (bi in 0 until 3) {
        matrix[ai][bi] = dimensionValue
      }
    }
  }

  /**
   *  Returns the value of one of this matrix
   *  entries.
   *  The value of the provided index is one of the
   *  values from the [Location] class.
   *  The value returned is a constant
   *  from the [Dimension] class.
   *
   * @param  row     the row of this `IntersectionMatrix`, indicating
   *      the interior, boundary or exterior of the first `Geometry`
   * @param  column  the column of this `IntersectionMatrix`,
   *      indicating the interior, boundary or exterior of the second `Geometry`
   * @return         the dimension value at the given matrix position.
   */
  fun get(row: Int, column: Int): Int {
    return matrix[row][column]
  }

  /**
   * Tests if this matrix matches `[FF*FF****]`.
   *
   * @return    `true` if the two `Geometry`s related by
   *      this matrix are disjoint
   */
  fun isDisjoint(): Boolean {
    return matrix[Location.INTERIOR][Location.INTERIOR] == Dimension.FALSE &&
      matrix[Location.INTERIOR][Location.BOUNDARY] == Dimension.FALSE &&
      matrix[Location.BOUNDARY][Location.INTERIOR] == Dimension.FALSE &&
      matrix[Location.BOUNDARY][Location.BOUNDARY] == Dimension.FALSE
  }

  /**
   *  Tests if `isDisjoint` returns false.
   *
   * @return `true` if the two `Geometry`s related by
   *      this matrix intersect
   */
  fun isIntersects(): Boolean {
    return !isDisjoint()
  }

  /**
   *  Tests if this matrix matches
   *  `[FT*******]`, `[F**T*****]` or `[F***T****]`.
   *
   * @param  dimensionOfGeometryA  the dimension of the first `Geometry`
   * @param  dimensionOfGeometryB  the dimension of the second `Geometry`
   * @return                       `true` if the two `Geometry`
   *      s related by this matrix touch; Returns false
   *      if both `Geometry`s are points.
   */
  fun isTouches(dimensionOfGeometryA: Int, dimensionOfGeometryB: Int): Boolean {
    if (dimensionOfGeometryA > dimensionOfGeometryB) {
      //no need to get transpose because pattern matrix is symmetrical
      return isTouches(dimensionOfGeometryB, dimensionOfGeometryA)
    }
    if ((dimensionOfGeometryA == Dimension.A && dimensionOfGeometryB == Dimension.A) ||
      (dimensionOfGeometryA == Dimension.L && dimensionOfGeometryB == Dimension.L) ||
      (dimensionOfGeometryA == Dimension.L && dimensionOfGeometryB == Dimension.A) ||
      (dimensionOfGeometryA == Dimension.P && dimensionOfGeometryB == Dimension.A) ||
      (dimensionOfGeometryA == Dimension.P && dimensionOfGeometryB == Dimension.L)) {
      return matrix[Location.INTERIOR][Location.INTERIOR] == Dimension.FALSE &&
        (isTrue(matrix[Location.INTERIOR][Location.BOUNDARY]) ||
          isTrue(matrix[Location.BOUNDARY][Location.INTERIOR]) ||
          isTrue(matrix[Location.BOUNDARY][Location.BOUNDARY]))
    }
    return false
  }

  /**
   * Tests whether this geometry crosses the
   * specified geometry.
   * 
   * The `crosses` predicate has the following equivalent definitions:
   * 
   * - The geometries have some but not all interior points in common.
   * - The DE-9IM Intersection Matrix for the two geometries matches
   *   
   *    - `[T*T******]` (for P/L, P/A, and L/A situations)
   *    - `[T*****T**]` (for L/P, L/A, and A/L situations)
   *    - `[0********]` (for L/L situations)
   *   
   * 
   * For any other combination of dimensions this predicate returns `false`.
   * 
   * The SFS defined this predicate only for P/L, P/A, L/L, and L/A situations.
   * JTS extends the definition to apply to L/P, A/P and A/L situations as well.
   * This makes the relation symmetric.
   *
   * @param  dimensionOfGeometryA  the dimension of the first `Geometry`
   * @param  dimensionOfGeometryB  the dimension of the second `Geometry`
   * @return                       `true` if the two `Geometry`s
   *      related by this matrix cross.
   */
  fun isCrosses(dimensionOfGeometryA: Int, dimensionOfGeometryB: Int): Boolean {
    if ((dimensionOfGeometryA == Dimension.P && dimensionOfGeometryB == Dimension.L) ||
      (dimensionOfGeometryA == Dimension.P && dimensionOfGeometryB == Dimension.A) ||
      (dimensionOfGeometryA == Dimension.L && dimensionOfGeometryB == Dimension.A)) {
      return isTrue(matrix[Location.INTERIOR][Location.INTERIOR]) &&
        isTrue(matrix[Location.INTERIOR][Location.EXTERIOR])
    }
    if ((dimensionOfGeometryA == Dimension.L && dimensionOfGeometryB == Dimension.P) ||
      (dimensionOfGeometryA == Dimension.A && dimensionOfGeometryB == Dimension.P) ||
      (dimensionOfGeometryA == Dimension.A && dimensionOfGeometryB == Dimension.L)) {
      return isTrue(matrix[Location.INTERIOR][Location.INTERIOR]) &&
        isTrue(matrix[Location.EXTERIOR][Location.INTERIOR])
    }
    if (dimensionOfGeometryA == Dimension.L && dimensionOfGeometryB == Dimension.L) {
      return matrix[Location.INTERIOR][Location.INTERIOR] == 0
    }
    return false
  }

  /**
   * Tests whether this matrix matches `[T*F**F***]`.
   *
   * @return    `true` if the first `Geometry` is within
   *      the second
   */
  fun isWithin(): Boolean {
    return isTrue(matrix[Location.INTERIOR][Location.INTERIOR]) &&
      matrix[Location.INTERIOR][Location.EXTERIOR] == Dimension.FALSE &&
      matrix[Location.BOUNDARY][Location.EXTERIOR] == Dimension.FALSE
  }

  /**
   * Tests whether this matrix matches [T*****FF*[.
   *
   * @return    `true` if the first `Geometry` contains the
   *      second
   */
  fun isContains(): Boolean {
    return isTrue(matrix[Location.INTERIOR][Location.INTERIOR]) &&
      matrix[Location.EXTERIOR][Location.INTERIOR] == Dimension.FALSE &&
      matrix[Location.EXTERIOR][Location.BOUNDARY] == Dimension.FALSE
  }

  /**
   * Tests if this matrix matches
   *    `[T*****FF*]`
   * or `[*T****FF*]`
   * or `[***T**FF*]`
   * or `[****T*FF*]`
   *
   * @return    `true` if the first `Geometry` covers the
   *      second
   */
  fun isCovers(): Boolean {
    val hasPointInCommon = isTrue(matrix[Location.INTERIOR][Location.INTERIOR]) ||
      isTrue(matrix[Location.INTERIOR][Location.BOUNDARY]) ||
      isTrue(matrix[Location.BOUNDARY][Location.INTERIOR]) ||
      isTrue(matrix[Location.BOUNDARY][Location.BOUNDARY])

    return hasPointInCommon &&
      matrix[Location.EXTERIOR][Location.INTERIOR] == Dimension.FALSE &&
      matrix[Location.EXTERIOR][Location.BOUNDARY] == Dimension.FALSE
  }

  /**
   *Tests if this matrix matches
   *    `[T*F**F***]`
   * or `[*TF**F***]`
   * or `[**FT*F***]`
   * or `[**F*TF***]`
   *
   * @return    `true` if the first `Geometry`
   * is covered by the second
   */
  fun isCoveredBy(): Boolean {
    val hasPointInCommon = isTrue(matrix[Location.INTERIOR][Location.INTERIOR]) ||
      isTrue(matrix[Location.INTERIOR][Location.BOUNDARY]) ||
      isTrue(matrix[Location.BOUNDARY][Location.INTERIOR]) ||
      isTrue(matrix[Location.BOUNDARY][Location.BOUNDARY])

    return hasPointInCommon &&
      matrix[Location.INTERIOR][Location.EXTERIOR] == Dimension.FALSE &&
      matrix[Location.BOUNDARY][Location.EXTERIOR] == Dimension.FALSE
  }

  /**
   *  Tests whether the argument dimensions are equal and
   *  this matrix matches the pattern `[T*F**FFF*]`.
   *  
   *  <b>Note:</b> This pattern differs from the one stated in
   *  <i>Simple feature access - Part 1: Common architecture</i>.
   *  That document states the pattern as `[TFFFTFFFT]`.  This would
   *  specify that
   *  two identical `POINT`s are not equal, which is not desirable behaviour.
   *  The pattern used here has been corrected to compute equality in this situation.
   *
   * @param  dimensionOfGeometryA  the dimension of the first `Geometry`
   * @param  dimensionOfGeometryB  the dimension of the second `Geometry`
   * @return                       `true` if the two `Geometry`s
   *      related by this matrix are equal; the
   *      `Geometry`s must have the same dimension to be equal
   */
  fun isEquals(dimensionOfGeometryA: Int, dimensionOfGeometryB: Int): Boolean {
    if (dimensionOfGeometryA != dimensionOfGeometryB) {
      return false
    }
    return isTrue(matrix[Location.INTERIOR][Location.INTERIOR]) &&
      matrix[Location.INTERIOR][Location.EXTERIOR] == Dimension.FALSE &&
      matrix[Location.BOUNDARY][Location.EXTERIOR] == Dimension.FALSE &&
      matrix[Location.EXTERIOR][Location.INTERIOR] == Dimension.FALSE &&
      matrix[Location.EXTERIOR][Location.BOUNDARY] == Dimension.FALSE
  }

  /**
   * Tests if this matrix matches
   *  <UL>
   *    <LI>`[T*T***T**]` (for two points or two surfaces)
   *    <LI>`[1*T***T**]` (for two curves)
   *  </UL>.
   *
   * @param  dimensionOfGeometryA  the dimension of the first `Geometry`
   * @param  dimensionOfGeometryB  the dimension of the second `Geometry`
   * @return                       `true` if the two `Geometry`s
   *      related by this matrix overlap. For this
   *      function to return `true`, the `Geometry`s must
   *      be two points, two curves or two surfaces.
   */
  fun isOverlaps(dimensionOfGeometryA: Int, dimensionOfGeometryB: Int): Boolean {
    if ((dimensionOfGeometryA == Dimension.P && dimensionOfGeometryB == Dimension.P) ||
      (dimensionOfGeometryA == Dimension.A && dimensionOfGeometryB == Dimension.A)) {
      return isTrue(matrix[Location.INTERIOR][Location.INTERIOR]) &&
        isTrue(matrix[Location.INTERIOR][Location.EXTERIOR]) &&
        isTrue(matrix[Location.EXTERIOR][Location.INTERIOR])
    }
    if (dimensionOfGeometryA == Dimension.L && dimensionOfGeometryB == Dimension.L) {
      return matrix[Location.INTERIOR][Location.INTERIOR] == 1 &&
        isTrue(matrix[Location.INTERIOR][Location.EXTERIOR]) &&
        isTrue(matrix[Location.EXTERIOR][Location.INTERIOR])
    }
    return false
  }

  /**
   * Tests whether this matrix matches the given matrix pattern.
   *
   * @param  pattern A pattern containing nine dimension symbols with which to
   *      compare the entries of this matrix. Possible
   *      symbol values are `{T, F, * , 0, 1, 2}`.
   * @return `true` if this matrix matches the pattern
   */
  fun matches(pattern: String): Boolean {
    if (pattern.length != 9) {
      throw IllegalArgumentException("Should be length 9: " + pattern)
    }
    for (ai in 0 until 3) {
      for (bi in 0 until 3) {
        if (!IntersectionMatrix.matches(matrix[ai][bi], pattern[3 * ai + bi])) {
          return false
        }
      }
    }
    return true
  }

  /**
   *  Transposes this IntersectionMatrix.
   *
   * @return    this `IntersectionMatrix` as a convenience
   */
  fun transpose(): IntersectionMatrix {
    var temp = matrix[1][0]
    matrix[1][0] = matrix[0][1]
    matrix[0][1] = temp
    temp = matrix[2][0]
    matrix[2][0] = matrix[0][2]
    matrix[0][2] = temp
    temp = matrix[2][1]
    matrix[2][1] = matrix[1][2]
    matrix[1][2] = temp
    return this
  }

  /**
   *  Returns a nine-character `String` representation of this `IntersectionMatrix`
   *  .
   *
   * @return    the nine dimension symbols of this `IntersectionMatrix`
   *      in row-major order.
   */
  override fun toString(): String {
    val builder = StringBuilder("123456789")
    for (ai in 0 until 3) {
      for (bi in 0 until 3) {
        builder[3 * ai + bi] = Dimension.toDimensionSymbol(matrix[ai][bi])
      }
    }
    return builder.toString()
  }

  companion object {
    /**
     *  Tests if the dimension value matches `TRUE`
     *  (i.e.  has value 0, 1, 2 or TRUE).
     *
     * @param  actualDimensionValue     a number that can be stored in the `IntersectionMatrix`
     *      . Possible values are `{TRUE, FALSE, DONTCARE, 0, 1, 2}`.
     * @return true if the dimension value matches TRUE
     */
    @JvmStatic
    fun isTrue(actualDimensionValue: Int): Boolean {
      if (actualDimensionValue >= 0 || actualDimensionValue == Dimension.TRUE) {
        return true
      }
      return false
    }

    /**
     *  Tests if the dimension value satisfies the dimension symbol.
     *
     * @param  actualDimensionValue     a number that can be stored in the `IntersectionMatrix`
     *      . Possible values are `{TRUE, FALSE, DONTCARE, 0, 1, 2}`.
     * @param  requiredDimensionSymbol  a character used in the string
     *      representation of an `IntersectionMatrix`. Possible values
     *      are `{T, F, * , 0, 1, 2}`.
     * @return                          true if the dimension symbol matches
     *      the dimension value
     */
    @JvmStatic
    fun matches(actualDimensionValue: Int, requiredDimensionSymbol: Char): Boolean {
      if (requiredDimensionSymbol == Dimension.SYM_DONTCARE) {
        return true
      }
      if (requiredDimensionSymbol == Dimension.SYM_TRUE && (actualDimensionValue >= 0 || actualDimensionValue == Dimension.TRUE)) {
        return true
      }
      if (requiredDimensionSymbol == Dimension.SYM_FALSE && actualDimensionValue == Dimension.FALSE) {
        return true
      }
      if (requiredDimensionSymbol == Dimension.SYM_P && actualDimensionValue == Dimension.P) {
        return true
      }
      if (requiredDimensionSymbol == Dimension.SYM_L && actualDimensionValue == Dimension.L) {
        return true
      }
      if (requiredDimensionSymbol == Dimension.SYM_A && actualDimensionValue == Dimension.A) {
        return true
      }
      return false
    }

    /**
     *  Tests if each of the actual dimension symbols in a matrix string satisfies the
     *  corresponding required dimension symbol in a pattern string.
     *
     * @param  actualDimensionSymbols    nine dimension symbols to validate.
     *      Possible values are `{T, F, * , 0, 1, 2}`.
     * @param  requiredDimensionSymbols  nine dimension symbols to validate
     *      against. Possible values are `{T, F, * , 0, 1, 2}`.
     * @return                           true if each of the required dimension
     *      symbols encompass the corresponding actual dimension symbol
     */
    @JvmStatic
    fun matches(actualDimensionSymbols: String, requiredDimensionSymbols: String): Boolean {
      val m = IntersectionMatrix(actualDimensionSymbols)
      return m.matches(requiredDimensionSymbols)
    }
  }
}
