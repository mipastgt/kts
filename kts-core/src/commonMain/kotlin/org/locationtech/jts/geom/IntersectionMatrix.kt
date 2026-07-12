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
 * specify the topological relationship between two {@link Geometry}s.
 * This class can also represent matrix patterns (such as "T*T******")
 * which are used for matching instances of DE-9IM matrices.
 * <p>
 * DE-9IM matrices are 3x3 matrices with integer entries.
 * The matrix indices {0,1,2} represent the topological locations
 * that occur in a geometry (Interior, Boundary, Exterior).
 * These are provided by the constants
 * {@link Location#INTERIOR}, {@link Location#BOUNDARY}, and {@link Location#EXTERIOR}.
 * <p>
 * When used to specify the topological relationship between two geometries,
 * the matrix entries represent the possible dimensions of each intersection:
 * {@link Dimension#A} = 2, {@link Dimension#L} = 1, {@link Dimension#P} = 0 and {@link Dimension#FALSE} = -1.
 * When used to represent a matrix pattern entries can have the additional values
 * {@link Dimension#TRUE} {"T") and {@link Dimension#DONTCARE} ("*").
 * <p>
 * For a description of the DE-9IM and the spatial predicates derived from it,
 * see the following references:
 * <ul>
 * <li><i><a href="http://www.opengis.org/techno/specs.htm">
 * OGC 99-049 OpenGIS Simple Features Specification for SQL</a></i>
 * , Section 2.1.13</li>
 * <li><i><a href="http://portal.opengeospatial.org/files/?artifact_id=25355">
 * OGC 06-103r4 OpenGIS Implementation Standard for Geographic information - Simple feature access - Part 1: Common architecture</a></i>
 * , Section 6.1.15 (which provides some further details on certain predicate specifications).
 * </li>
 * <li>Wikipedia article on <a href="https://en.wikipedia.org/wiki/DE-9IM">DE-9IM</a></li>
 * </ul>
 * <p>
 * Methods are provided to:
 *  <UL>
 *    <LI>set and query the elements of the matrix in a convenient fashion
 *    <LI>convert to and from the standard string representation (specified in
 *    SFS Section 2.1.13.2).
 *    <LI>test if a matrix matches a given pattern string.
 *    <li>test if a matrix (possibly with geometry dimensions) matches a standard named spatial predicate
 *  </UL>
 *
 * @version 1.7
 */
open class IntersectionMatrix {
  /**
   *  Internal representation of this <code>IntersectionMatrix</code>.
   */
  private var matrix: Array<IntArray> = Array(3) { IntArray(3) }

  /**
   *  Creates an <code>IntersectionMatrix</code> with <code>FALSE</code>
   *  dimension values.
   */
  constructor() {
    setAll(Dimension.FALSE)
  }

  /**
   *  Creates an <code>IntersectionMatrix</code> with the given dimension
   *  symbols.
   *
   * @param  elements  a String of nine dimension symbols in row major order
   */
  constructor(elements: String) : this() {
    set(elements)
  }

  /**
   *  Creates an <code>IntersectionMatrix</code> with the same elements as
   *  <code>other</code>.
   *
   * @param  other  an <code>IntersectionMatrix</code> to copy
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
   *  Changes the value of one of this <code>IntersectionMatrix</code>s
   *  elements.
   *
   * @param  row             the row of this <code>IntersectionMatrix</code>,
   *      indicating the interior, boundary or exterior of the first <code>Geometry</code>
   * @param  column          the column of this <code>IntersectionMatrix</code>,
   *      indicating the interior, boundary or exterior of the second <code>Geometry</code>
   * @param  dimensionValue  the new value of the element
   */
  fun set(row: Int, column: Int, dimensionValue: Int) {
    matrix[row][column] = dimensionValue
  }

  /**
   *  Changes the elements of this <code>IntersectionMatrix</code> to the
   *  dimension symbols in <code>dimensionSymbols</code>.
   *
   * @param  dimensionSymbols  nine dimension symbols to which to set this <code>IntersectionMatrix</code>
   *      s elements. Possible values are <code>{T, F, * , 0, 1, 2}</code>
   */
  fun set(dimensionSymbols: String) {
    for (i in 0 until dimensionSymbols.length) {
      val row = i / 3
      val col = i % 3
      matrix[row][col] = Dimension.toDimensionValue(dimensionSymbols[i])
    }
  }

  /**
   *  Changes the specified element to <code>minimumDimensionValue</code> if the
   *  element is less.
   *
   * @param  row                    the row of this <code>IntersectionMatrix</code>
   *      , indicating the interior, boundary or exterior of the first <code>Geometry</code>
   * @param  column                 the column of this <code>IntersectionMatrix</code>
   *      , indicating the interior, boundary or exterior of the second <code>Geometry</code>
   * @param  minimumDimensionValue  the dimension value with which to compare the
   *      element. The order of dimension values from least to greatest is
   *      <code>{DONTCARE, TRUE, FALSE, 0, 1, 2}</code>.
   */
  fun setAtLeast(row: Int, column: Int, minimumDimensionValue: Int) {
    if (matrix[row][column] < minimumDimensionValue) {
      matrix[row][column] = minimumDimensionValue
    }
  }

  /**
   *  If row &gt;= 0 and column &gt;= 0, changes the specified element to <code>minimumDimensionValue</code>
   *  if the element is less. Does nothing if row &lt;0 or column &lt; 0.
   *
   * @param  row                    the row of this <code>IntersectionMatrix</code>
   *      , indicating the interior, boundary or exterior of the first <code>Geometry</code>
   * @param  column                 the column of this <code>IntersectionMatrix</code>
   *      , indicating the interior, boundary or exterior of the second <code>Geometry</code>
   * @param  minimumDimensionValue  the dimension value with which to compare the
   *      element. The order of dimension values from least to greatest is
   *      <code>{DONTCARE, TRUE, FALSE, 0, 1, 2}</code>.
   */
  fun setAtLeastIfValid(row: Int, column: Int, minimumDimensionValue: Int) {
    if (row >= 0 && column >= 0) {
      setAtLeast(row, column, minimumDimensionValue)
    }
  }

  /**
   *  For each element in this <code>IntersectionMatrix</code>, changes the
   *  element to the corresponding minimum dimension symbol if the element is
   *  less.
   *
   * @param  minimumDimensionSymbols  nine dimension symbols with which to
   *      compare the elements of this <code>IntersectionMatrix</code>. The
   *      order of dimension values from least to greatest is <code>{DONTCARE, TRUE, FALSE, 0, 1, 2}</code>
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
   *  Changes the elements of this <code>IntersectionMatrix</code> to <code>dimensionValue</code>
   *  .
   *
   * @param  dimensionValue  the dimension value to which to set this <code>IntersectionMatrix</code>
   *      s elements. Possible values <code>{TRUE, FALSE, DONTCARE, 0, 1, 2}</code>
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
   *  values from the {@link Location} class.
   *  The value returned is a constant
   *  from the {@link Dimension} class.
   *
   * @param  row     the row of this <code>IntersectionMatrix</code>, indicating
   *      the interior, boundary or exterior of the first <code>Geometry</code>
   * @param  column  the column of this <code>IntersectionMatrix</code>,
   *      indicating the interior, boundary or exterior of the second <code>Geometry</code>
   * @return         the dimension value at the given matrix position.
   */
  fun get(row: Int, column: Int): Int {
    return matrix[row][column]
  }

  /**
   * Tests if this matrix matches <code>[FF*FF****]</code>.
   *
   * @return    <code>true</code> if the two <code>Geometry</code>s related by
   *      this matrix are disjoint
   */
  fun isDisjoint(): Boolean {
    return matrix[Location.INTERIOR][Location.INTERIOR] == Dimension.FALSE &&
      matrix[Location.INTERIOR][Location.BOUNDARY] == Dimension.FALSE &&
      matrix[Location.BOUNDARY][Location.INTERIOR] == Dimension.FALSE &&
      matrix[Location.BOUNDARY][Location.BOUNDARY] == Dimension.FALSE
  }

  /**
   *  Tests if <code>isDisjoint</code> returns false.
   *
   * @return <code>true</code> if the two <code>Geometry</code>s related by
   *      this matrix intersect
   */
  fun isIntersects(): Boolean {
    return !isDisjoint()
  }

  /**
   *  Tests if this matrix matches
   *  <code>[FT*******]</code>, <code>[F**T*****]</code> or <code>[F***T****]</code>.
   *
   * @param  dimensionOfGeometryA  the dimension of the first <code>Geometry</code>
   * @param  dimensionOfGeometryB  the dimension of the second <code>Geometry</code>
   * @return                       <code>true</code> if the two <code>Geometry</code>
   *      s related by this matrix touch; Returns false
   *      if both <code>Geometry</code>s are points.
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
   * <p>
   * The <code>crosses</code> predicate has the following equivalent definitions:
   * <ul>
   * <li>The geometries have some but not all interior points in common.
   * <li>The DE-9IM Intersection Matrix for the two geometries matches
   *   <ul>
   *    <li><code>[T*T******]</code> (for P/L, P/A, and L/A situations)
   *    <li><code>[T*****T**]</code> (for L/P, L/A, and A/L situations)
   *    <li><code>[0********]</code> (for L/L situations)
   *   </ul>
   * </ul>
   * For any other combination of dimensions this predicate returns <code>false</code>.
   * <p>
   * The SFS defined this predicate only for P/L, P/A, L/L, and L/A situations.
   * JTS extends the definition to apply to L/P, A/P and A/L situations as well.
   * This makes the relation symmetric.
   *
   * @param  dimensionOfGeometryA  the dimension of the first <code>Geometry</code>
   * @param  dimensionOfGeometryB  the dimension of the second <code>Geometry</code>
   * @return                       <code>true</code> if the two <code>Geometry</code>s
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
   * Tests whether this matrix matches <code>[T*F**F***]</code>.
   *
   * @return    <code>true</code> if the first <code>Geometry</code> is within
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
   * @return    <code>true</code> if the first <code>Geometry</code> contains the
   *      second
   */
  fun isContains(): Boolean {
    return isTrue(matrix[Location.INTERIOR][Location.INTERIOR]) &&
      matrix[Location.EXTERIOR][Location.INTERIOR] == Dimension.FALSE &&
      matrix[Location.EXTERIOR][Location.BOUNDARY] == Dimension.FALSE
  }

  /**
   * Tests if this matrix matches
   *    <code>[T*****FF*]</code>
   * or <code>[*T****FF*]</code>
   * or <code>[***T**FF*]</code>
   * or <code>[****T*FF*]</code>
   *
   * @return    <code>true</code> if the first <code>Geometry</code> covers the
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
   *    <code>[T*F**F***]</code>
   * or <code>[*TF**F***]</code>
   * or <code>[**FT*F***]</code>
   * or <code>[**F*TF***]</code>
   *
   * @return    <code>true</code> if the first <code>Geometry</code>
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
   *  this matrix matches the pattern <tt>[T*F**FFF*]</tt>.
   *  <p>
   *  <b>Note:</b> This pattern differs from the one stated in
   *  <i>Simple feature access - Part 1: Common architecture</i>.
   *  That document states the pattern as <tt>[TFFFTFFFT]</tt>.  This would
   *  specify that
   *  two identical <tt>POINT</tt>s are not equal, which is not desirable behaviour.
   *  The pattern used here has been corrected to compute equality in this situation.
   *
   * @param  dimensionOfGeometryA  the dimension of the first <code>Geometry</code>
   * @param  dimensionOfGeometryB  the dimension of the second <code>Geometry</code>
   * @return                       <code>true</code> if the two <code>Geometry</code>s
   *      related by this matrix are equal; the
   *      <code>Geometry</code>s must have the same dimension to be equal
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
   *    <LI><tt>[T*T***T**]</tt> (for two points or two surfaces)
   *    <LI><tt>[1*T***T**]</tt> (for two curves)
   *  </UL>.
   *
   * @param  dimensionOfGeometryA  the dimension of the first <code>Geometry</code>
   * @param  dimensionOfGeometryB  the dimension of the second <code>Geometry</code>
   * @return                       <code>true</code> if the two <code>Geometry</code>s
   *      related by this matrix overlap. For this
   *      function to return <code>true</code>, the <code>Geometry</code>s must
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
   *      symbol values are <code>{T, F, * , 0, 1, 2}</code>.
   * @return <code>true</code> if this matrix matches the pattern
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
   * @return    this <code>IntersectionMatrix</code> as a convenience
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
   *  Returns a nine-character <code>String</code> representation of this <code>IntersectionMatrix</code>
   *  .
   *
   * @return    the nine dimension symbols of this <code>IntersectionMatrix</code>
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
     *  Tests if the dimension value matches <tt>TRUE</tt>
     *  (i.e.  has value 0, 1, 2 or TRUE).
     *
     * @param  actualDimensionValue     a number that can be stored in the <code>IntersectionMatrix</code>
     *      . Possible values are <code>{TRUE, FALSE, DONTCARE, 0, 1, 2}</code>.
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
     * @param  actualDimensionValue     a number that can be stored in the <code>IntersectionMatrix</code>
     *      . Possible values are <code>{TRUE, FALSE, DONTCARE, 0, 1, 2}</code>.
     * @param  requiredDimensionSymbol  a character used in the string
     *      representation of an <code>IntersectionMatrix</code>. Possible values
     *      are <code>{T, F, * , 0, 1, 2}</code>.
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
     *      Possible values are <code>{T, F, * , 0, 1, 2}</code>.
     * @param  requiredDimensionSymbols  nine dimension symbols to validate
     *      against. Possible values are <code>{T, F, * , 0, 1, 2}</code>.
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
