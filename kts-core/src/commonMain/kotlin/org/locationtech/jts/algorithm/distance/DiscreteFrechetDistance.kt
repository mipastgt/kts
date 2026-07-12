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
package org.locationtech.jts.algorithm.distance

import kotlin.jvm.JvmStatic
import kotlin.math.max
import kotlin.math.min

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Geometry

/**
 * The Fréchet distance is a measure of similarity between curves. Thus, it can
 * be used like the Hausdorff distance.
 *
 * This implementation is based on an optimized Fréchet distance algorithm.
 * Several matrix storage implementations are provided.
 */
class DiscreteFrechetDistance(private val g0: Geometry, private val g1: Geometry) {

  private var ptDist: PointPairDistance? = null

  /**
   * Computes the [Discrete Fréchet Distance] between the input geometries
   *
   * @return the Discrete Fréchet Distance
   */
  private fun distance(): Double {
    val coords0 = g0.getCoordinates()
    val coords1 = g1.getCoordinates()

    val distances = createMatrixStorage(coords0.size, coords1.size)
    val diagonal = bresenhamDiagonal(coords0.size, coords1.size)

    val distanceToPair = HashMap<Double, IntArray>()
    computeCoordinateDistances(coords0, coords1, diagonal, distances, distanceToPair)
    ptDist = computeFrechet(coords0, coords1, diagonal, distances, distanceToPair)

    return ptDist!!.getDistance()
  }

  /**
   * Gets the pair of [Coordinate]s at which the distance is obtained.
   *
   * @return the pair of Coordinates at which the distance is obtained
   */
  fun getCoordinates(): Array<Coordinate> {
    if (ptDist == null)
      distance()

    return ptDist!!.getCoordinates()
  }

  /**
   * Computes relevant distances between pairs of [Coordinate]s for the
   * computation of the `Discrete Fréchet Distance`.
   */
  private fun computeCoordinateDistances(
    coords0: Array<Coordinate>, coords1: Array<Coordinate>, diagonal: IntArray,
    distances: MatrixStorage, distanceToPair: HashMap<Double, IntArray>
  ) {
    val numDiag = diagonal.size
    var maxDistOnDiag = 0.0
    var imin = 0
    var jmin = 0
    val numCoords0 = coords0.size
    val numCoords1 = coords1.size

    // First compute all the distances along the diagonal.
    // Record the maximum distance.

    var k = 0
    while (k < numDiag) {
      val i0 = diagonal[k]
      val j0 = diagonal[k + 1]
      val diagDist = coords0[i0].distance(coords1[j0])
      if (diagDist > maxDistOnDiag) maxDistOnDiag = diagDist
      distances.set(i0, j0, diagDist)
      distanceToPair.getOrPut(diagDist) { intArrayOf(i0, j0) }
      k += 2
    }

    // Check for distances shorter than maxDistOnDiag along the diagonal
    k = 0
    while (k < numDiag - 2) {
      // Decode index
      val i0 = diagonal[k]
      val j0 = diagonal[k + 1]

      // Get reference coordinates for col and row
      val coord0 = coords0[i0]
      val coord1 = coords1[j0]

      // Check for shorter distances in this row
      var i = i0 + 1
      while (i < numCoords0) {
        if (!distances.isValueSet(i, j0)) {
          val dist = coords0[i].distance(coord1)
          if (dist < maxDistOnDiag || i < imin) {
            distances.set(i, j0, dist)
            distanceToPair.getOrPut(dist) { intArrayOf(i, j0) }
          } else
            break
        } else
          break
        i++
      }
      imin = i

      // Check for shorter distances in this column
      var j = j0 + 1
      while (j < numCoords1) {
        if (!distances.isValueSet(i0, j)) {
          val dist = coord0.distance(coords1[j])
          if (dist < maxDistOnDiag || j < jmin) {
            distances.set(i0, j, dist)
            distanceToPair.getOrPut(dist) { intArrayOf(i0, j) }
          } else
            break
        } else
          break
        j++
      }
      jmin = j
      k += 2
    }
  }

  /**
   * Abstract base class for storing 2d matrix data
   */
  abstract class MatrixStorage(
    protected val numRows: Int,
    protected val numCols: Int,
    protected val defaultValue: Double
  ) {

    /**
     * Gets the matrix value at i, j
     */
    abstract fun get(i: Int, j: Int): Double

    /**
     * Sets the matrix value at i, j
     */
    abstract fun set(i: Int, j: Int, value: Double)

    /**
     * Gets a flag indicating if the matrix has a set value, e.g. one that is different
     * than [defaultValue].
     */
    abstract fun isValueSet(i: Int, j: Int): Boolean
  }

  /**
   * Straight forward implementation of a rectangular matrix
   */
  class RectMatrix(numRows: Int, numCols: Int, defaultValue: Double) :
    MatrixStorage(numRows, numCols, defaultValue) {

    private val matrix: DoubleArray = DoubleArray(numRows * numCols)

    init {
      matrix.fill(defaultValue)
    }

    override fun get(i: Int, j: Int): Double = matrix[i * numCols + j]

    override fun set(i: Int, j: Int, value: Double) {
      matrix[i * numCols + j] = value
    }

    override fun isValueSet(i: Int, j: Int): Boolean {
      return get(i, j).toBits() != defaultValue.toBits()
    }
  }

  /**
   * A matrix implementation that adheres to the Compressed sparse row format.
   * Note: Unfortunately not as fast as expected.
   */
  class CsrMatrix : MatrixStorage {

    private var v: DoubleArray
    private val ri: IntArray
    private var ci: IntArray

    constructor(numRows: Int, numCols: Int, defaultValue: Double) :
      this(numRows, numCols, defaultValue, expectedValuesHeuristic(numRows, numCols))

    constructor(numRows: Int, numCols: Int, defaultValue: Double, expectedValues: Int) :
      super(numRows, numCols, defaultValue) {
      this.v = DoubleArray(expectedValues)
      this.ci = IntArray(expectedValues)
      this.ri = IntArray(numRows + 1)
    }

    private fun indexOf(i: Int, j: Int): Int {
      val cLow = ri[i]
      val cHigh = ri[i + 1]
      if (cHigh <= cLow) return cLow.inv()

      return intBinarySearch(ci, j, cLow, cHigh)
    }

    override fun get(i: Int, j: Int): Double {
      // get the index in the vector
      val vi = indexOf(i, j)

      // if the vector index is negative, return default value
      if (vi < 0)
        return defaultValue

      return v[vi]
    }

    override fun set(i: Int, j: Int, value: Double) {
      // get the index in the vector
      var vi = indexOf(i, j)

      // do we already have a value?
      if (vi < 0) {
        // no, we don't, we need to ensure space!
        ensureCapacity(ri[numRows] + 1)

        // update row indices
        for (ii in i + 1..numRows)
          ri[ii] += 1

        // move and update column indices, move values
        vi = vi.inv()
        var ii = ri[numRows]
        while (ii > vi) {
          ci[ii] = ci[ii - 1]
          v[ii] = v[ii - 1]
          ii--
        }

        // insert column index
        ci[vi] = j
      }

      // set the new value
      v[vi] = value
    }

    override fun isValueSet(i: Int, j: Int): Boolean {
      return indexOf(i, j) >= 0
    }

    /**
     * Ensures that the column index vector (ci) and value vector (v) are sufficiently large.
     */
    private fun ensureCapacity(required: Int) {
      if (required < v.size)
        return

      val increment = max(numRows, numCols)
      v = v.copyOf(v.size + increment)
      ci = ci.copyOf(v.size + increment)
    }

    companion object {
      private fun expectedValuesHeuristic(numRows: Int, numCols: Int): Int {
        val max = max(numRows, numCols)
        return max * max / 10
      }
    }
  }

  /**
   * A sparse matrix based on java's [HashMap].
   */
  class HashMapMatrix(numRows: Int, numCols: Int, defaultValue: Double) :
    MatrixStorage(numRows, numCols, defaultValue) {

    private val matrix = HashMap<Long, Double>()

    override fun get(i: Int, j: Int): Double {
      val key = (i.toLong() shl 32) or j.toLong()
      return matrix[key] ?: defaultValue
    }

    override fun set(i: Int, j: Int, value: Double) {
      val key = (i.toLong() shl 32) or j.toLong()
      matrix.put(key, value)
    }

    override fun isValueSet(i: Int, j: Int): Boolean {
      val key = (i.toLong() shl 32) or j.toLong()
      return matrix.containsKey(key)
    }
  }

  companion object {
    /**
     * Computes the Discrete Fréchet Distance between two [Geometry]s
     * using a `Cartesian` distance computation function.
     *
     * @param g0 the 1st geometry
     * @param g1 the 2nd geometry
     * @return the cartesian distance between g0 and g1
     */
    @JvmStatic
    fun distance(g0: Geometry, g1: Geometry): Double {
      val dist = DiscreteFrechetDistance(g0, g1)
      return dist.distance()
    }

    /**
     * Creates a matrix to store the computed distances.
     */
    private fun createMatrixStorage(rows: Int, cols: Int): MatrixStorage {
      val max = max(rows, cols)
      // NOTE: these constraints need to be verified
      if (max < 1024)
        return RectMatrix(rows, cols, Double.POSITIVE_INFINITY)

      return CsrMatrix(rows, cols, Double.POSITIVE_INFINITY)
    }

    /**
     * Computes the Fréchet Distance for the given distance matrix.
     */
    private fun computeFrechet(
      coords0: Array<Coordinate>, coords1: Array<Coordinate>, diagonal: IntArray,
      distances: MatrixStorage, distanceToPair: HashMap<Double, IntArray>
    ): PointPairDistance {
      var d = 0
      while (d < diagonal.size) {
        val i0 = diagonal[d]
        val j0 = diagonal[d + 1]

        for (i in i0 until coords0.size) {
          if (distances.isValueSet(i, j0)) {
            val dist = getMinDistanceAtCorner(distances, i, j0)
            if (dist > distances.get(i, j0))
              distances.set(i, j0, dist)
          } else {
            break
          }
        }
        for (j in j0 + 1 until coords1.size) {
          if (distances.isValueSet(i0, j)) {
            val dist = getMinDistanceAtCorner(distances, i0, j)
            if (dist > distances.get(i0, j))
              distances.set(i0, j, dist)
          } else {
            break
          }
        }
        d += 2
      }

      val result = PointPairDistance()
      val distance = distances.get(coords0.size - 1, coords1.size - 1)
      val index = distanceToPair.get(distance)
        ?: throw IllegalStateException("Pair of points not recorded for computed distance")
      result.initialize(coords0[index[0]], coords1[index[1]], distance)
      return result
    }

    /**
     * Returns the minimum distance at the corner (`i, j`).
     */
    private fun getMinDistanceAtCorner(matrix: MatrixStorage, i: Int, j: Int): Double {
      if (i > 0 && j > 0) {
        val d0 = matrix.get(i - 1, j - 1)
        val d1 = matrix.get(i - 1, j)
        val d2 = matrix.get(i, j - 1)
        return min(min(d0, d1), d2)
      }
      if (i == 0 && j == 0)
        return matrix.get(0, 0)

      if (i == 0)
        return matrix.get(0, j - 1)

      // j == 0
      return matrix.get(i - 1, 0)
    }

    /**
     * Computes the indices for the diagonal of a `numCols x numRows` grid
     * using the Bresenham line algorithm.
     *
     * @param numCols the number of columns
     * @param numRows the number of rows
     * @return a packed array of column and row indices
     */
    @JvmStatic
    fun bresenhamDiagonal(numCols: Int, numRows: Int): IntArray {
      val dim = max(numCols, numRows)
      val diagXY = IntArray(2 * dim)

      val dx = numCols - 1
      val dy = numRows - 1
      var err: Int
      var i = 0
      if (numCols > numRows) {
        var y = 0
        err = 2 * dy - dx
        for (x in 0 until numCols) {
          diagXY[i++] = x
          diagXY[i++] = y
          if (err > 0) {
            y += 1
            err -= 2 * dx
          }
          err += 2 * dy
        }
      } else {
        var x = 0
        err = 2 * dx - dy
        for (y in 0 until numRows) {
          diagXY[i++] = x
          diagXY[i++] = y
          if (err > 0) {
            x += 1
            err -= 2 * dy
          }
          err += 2 * dx
        }
      }
      return diagXY
    }
  }
}

/**
 * Searches `[fromIndex, toIndex)` of a sorted [IntArray] for [key], returning the index if
 * found, otherwise `-(insertionPoint) - 1` (the standard binary-search convention; a
 * `kotlin`-common replacement for the JVM-only `IntArray.binarySearch`).
 */
private fun intBinarySearch(a: IntArray, key: Int, fromIndex: Int, toIndex: Int): Int {
  var low = fromIndex
  var high = toIndex - 1
  while (low <= high) {
    val mid = (low + high) ushr 1
    val midVal = a[mid]
    when {
      midVal < key -> low = mid + 1
      midVal > key -> high = mid - 1
      else -> return mid
    }
  }
  return -(low + 1)
}
