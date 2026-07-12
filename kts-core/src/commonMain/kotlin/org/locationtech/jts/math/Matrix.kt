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

import kotlin.math.abs

/**
 * Implements some 2D matrix operations
 * (in particular, solving systems of linear equations).
 *
 * @author Martin Davis
 *
 */
class Matrix {
  companion object {
    private fun swapRows(m: Array<DoubleArray>, i: Int, j: Int) {
      if (i == j) return
      for (col in 0 until m[0].size) {
        val temp = m[i][col]
        m[i][col] = m[j][col]
        m[j][col] = temp
      }
    }

    private fun swapRows(m: DoubleArray, i: Int, j: Int) {
      if (i == j) return
      val temp = m[i]
      m[i] = m[j]
      m[j] = temp
    }

    /**
     * Solves a system of equations using Gaussian Elimination.
     * In order to avoid overhead the algorithm runs in-place
     * on A - if A should not be modified the client must supply a copy.
     *
     * @param a an nxn matrix in row/column order )modified by this method)
     * @param b a vector of length n
     *
     * @return a vector containing the solution (if any)
     * or null if the system has no or no unique solution
     *
     * @throws IllegalArgumentException if the matrix is the wrong size
     */
    @JvmStatic
    fun solve(a: Array<DoubleArray>, b: DoubleArray): DoubleArray? {
      val n = b.size
      if (a.size != n || a[0].size != n)
        throw IllegalArgumentException("Matrix A is incorrectly sized")

      // Use Gaussian Elimination with partial pivoting.
      // Iterate over each row
      for (i in 0 until n) {
        // Find the largest pivot in the rows below the current one.
        var maxElementRow = i
        for (j in i + 1 until n)
          if (abs(a[j][i]) > abs(a[maxElementRow][i]))
            maxElementRow = j

        if (a[maxElementRow][i] == 0.0)
          return null

        // Exchange current row and maxElementRow in A and b.
        swapRows(a, i, maxElementRow)
        swapRows(b, i, maxElementRow)

        // Eliminate using row i
        for (j in i + 1 until n) {
          val rowFactor = a[j][i] / a[i][i]
          for (k in n - 1 downTo i)
            a[j][k] -= a[i][k] * rowFactor
          b[j] -= b[i] * rowFactor
        }
      }

      /**
       * A is now (virtually) in upper-triangular form.
       * The solution vector is determined by back-substitution.
       */
      val solution = DoubleArray(n)
      for (j in n - 1 downTo 0) {
        var t = 0.0
        for (k in j + 1 until n)
          t += a[j][k] * solution[k]
        solution[j] = (b[j] - t) / a[j][j]
      }
      return solution
    }
  }
}
