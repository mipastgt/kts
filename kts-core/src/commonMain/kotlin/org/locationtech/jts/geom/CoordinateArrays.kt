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
import kotlin.math.max

import org.locationtech.jts.math.MathUtil

/**
 * Useful utility functions for handling Coordinate arrays
 *
 */
class CoordinateArrays private constructor() {

  /**
   * A [Comparator] for [Coordinate] arrays
   * in the forward direction of their coordinates,
   * using lexicographic ordering.
   */
  class ForwardComparator : Comparator<Any?> {
    override fun compare(o1: Any?, o2: Any?): Int {
      @Suppress("UNCHECKED_CAST")
      val pts1 = o1 as Array<Coordinate>
      @Suppress("UNCHECKED_CAST")
      val pts2 = o2 as Array<Coordinate>

      return CoordinateArrays.compare(pts1, pts2)
    }
  }

  /**
   * A [Comparator] for [Coordinate] arrays
   * modulo their directionality.
   * E.g. if two coordinate arrays are identical but reversed
   * they will compare as equal under this ordering.
   * If the arrays are not equal, the ordering returned
   * is the ordering in the forward direction.
   */
  class BidirectionalComparator : Comparator<Any?> {
    override fun compare(o1: Any?, o2: Any?): Int {
      @Suppress("UNCHECKED_CAST")
      val pts1 = o1 as Array<Coordinate>
      @Suppress("UNCHECKED_CAST")
      val pts2 = o2 as Array<Coordinate>

      if (pts1.size < pts2.size) return -1
      if (pts1.size > pts2.size) return 1

      if (pts1.size == 0) return 0

      val forwardComp = CoordinateArrays.compare(pts1, pts2)
      val isEqualRev = CoordinateArrays.isEqualReversed(pts1, pts2)
      if (isEqualRev)
        return 0
      return forwardComp
    }

    fun OLDcompare(o1: Any?, o2: Any?): Int {
      @Suppress("UNCHECKED_CAST")
      val pts1 = o1 as Array<Coordinate>
      @Suppress("UNCHECKED_CAST")
      val pts2 = o2 as Array<Coordinate>

      if (pts1.size < pts2.size) return -1
      if (pts1.size > pts2.size) return 1

      if (pts1.size == 0) return 0

      val dir1 = CoordinateArrays.increasingDirection(pts1)
      val dir2 = CoordinateArrays.increasingDirection(pts2)

      var i1 = if (dir1 > 0) 0 else pts1.size - 1
      var i2 = if (dir2 > 0) 0 else pts1.size - 1

      for (i in 0 until pts1.size) {
        val comparePt = pts1[i1].compareTo(pts2[i2])
        if (comparePt != 0)
          return comparePt
        i1 += dir1
        i2 += dir2
      }
      return 0
    }
  }

  companion object {
    /**
     * Determine dimension based on subclass of [Coordinate].
     *
     * @param pts supplied coordinates
     * @return number of ordinates recorded
     */
    @JvmStatic
    fun dimension(pts: Array<Coordinate>?): Int {
      if (pts == null || pts.size == 0) {
        return 3 // unknown, assume default
      }
      var dimension = 0
      for (coordinate in pts) {
        dimension = max(dimension, Coordinates.dimension(coordinate))
      }
      return dimension
    }

    /**
     * Determine number of measures based on subclass of [Coordinate].
     *
     * @param pts supplied coordinates
     * @return number of measures recorded
     */
    @JvmStatic
    fun measures(pts: Array<Coordinate>?): Int {
      if (pts == null || pts.size == 0) {
        return 0 // unknown, assume default
      }
      var measures = 0
      for (coordinate in pts) {
        measures = max(measures, Coordinates.measures(coordinate))
      }
      return measures
    }

    /**
     * Utility method ensuring array contents are of consistent dimension and measures.
     * 
     * Array is modified in place if required, coordinates are replaced in the array as required
     * to ensure all coordinates have the same dimension and measures. The final dimension and
     * measures used are the maximum found when checking the array.
     * 
     *
     * @param array Modified in place to coordinates of consistent dimension and measures.
     */
    @JvmStatic
    fun enforceConsistency(array: Array<Coordinate?>?) {
      if (array == null) {
        return
      }
      // step one check
      var maxDimension = -1
      var maxMeasures = -1
      var isConsistent = true
      for (i in 0 until array.size) {
        val coordinate = array[i]
        if (coordinate != null) {
          val d = Coordinates.dimension(coordinate)
          val m = Coordinates.measures(coordinate)
          if (maxDimension == -1) {
            maxDimension = d
            maxMeasures = m
            continue
          }
          if (d != maxDimension || m != maxMeasures) {
            isConsistent = false
            maxDimension = max(maxDimension, d)
            maxMeasures = max(maxMeasures, m)
          }
        }
      }
      if (!isConsistent) {
        // step two fix
        val sample = Coordinates.create(maxDimension, maxMeasures)
        val type = sample::class

        for (i in 0 until array.size) {
          val coordinate = array[i]
          if (coordinate != null && coordinate::class != type) {
            val duplicate = Coordinates.create(maxDimension, maxMeasures)
            duplicate.setCoordinate(coordinate)
            array[i] = duplicate
          }
        }
      }
    }

    /**
     * Utility method ensuring array contents are of the specified dimension and measures.
     * 
     * Array is returned unmodified if consistent, or a copy of the array is made with
     * each inconsistent coordinate duplicated into an instance of the correct dimension and measures.
     * </>
     *
     * @param array coordinate array
     * @param dimension
     * @param measures
     * @return array returned, or copy created if required to enforce consistency.
     */
    @JvmStatic
    fun enforceConsistency(array: Array<Coordinate?>, dimension: Int, measures: Int): Array<Coordinate?> {
      val sample = Coordinates.create(dimension, measures)
      val type = sample::class
      var isConsistent = true
      for (i in 0 until array.size) {
        val coordinate = array[i]
        if (coordinate != null && coordinate::class != type) {
          isConsistent = false
          break
        }
      }
      if (isConsistent) {
        return array
      } else {
        val copy = arrayOfNulls<Coordinate>(array.size)
        for (i in 0 until copy.size) {
          val coordinate = array[i]
          if (coordinate != null && coordinate::class != type) {
            val duplicate = Coordinates.create(dimension, measures)
            duplicate.setCoordinate(coordinate)
            copy[i] = duplicate
          } else {
            copy[i] = coordinate
          }
        }
        return copy
      }
    }

    /**
     * Tests whether an array of [Coordinate]s forms a ring,
     * by checking length and closure.
     * Self-intersection is not checked.
     *
     * @param pts an array of Coordinates
     * @return true if the coordinate form a ring.
     */
    @JvmStatic
    fun isRing(pts: Array<Coordinate>): Boolean {
      if (pts.size < 4) return false
      if (!pts[0].equals2D(pts[pts.size - 1])) return false
      return true
    }

    /**
     * Finds a point in a list of points which is not contained in another list of points
     *
     * @param testPts the [Coordinate]s to test
     * @param pts     an array of [Coordinate]s to test the input points against
     * @return a [Coordinate] from `testPts` which is not in `pts`, '
     * or `null`
     */
    @JvmStatic
    fun ptNotInList(testPts: Array<Coordinate>, pts: Array<Coordinate>): Coordinate? {
      for (i in 0 until testPts.size) {
        val testPt = testPts[i]
        if (indexOf(testPt, pts) < 0)
          return testPt
      }
      return null
    }

    /**
     * Compares two [Coordinate] arrays
     * in the forward direction of their coordinates,
     * using lexicographic ordering.
     *
     * @param pts1
     * @param pts2
     * @return an integer indicating the order
     */
    @JvmStatic
    fun compare(pts1: Array<Coordinate>, pts2: Array<Coordinate>): Int {
      var i = 0
      while (i < pts1.size && i < pts2.size) {
        val compare = pts1[i].compareTo(pts2[i])
        if (compare != 0)
          return compare
        i++
      }
      // handle situation when arrays are of different length
      if (i < pts2.size) return -1
      if (i < pts1.size) return 1

      return 0
    }

    /**
     * Determines which orientation of the [Coordinate] array
     * is (overall) increasing.
     * In other words, determines which end of the array is "smaller"
     * (using the standard ordering on [Coordinate]).
     * Returns an integer indicating the increasing direction.
     * If the sequence is a palindrome, it is defined to be
     * oriented in a positive direction.
     *
     * @param pts the array of Coordinates to test
     * @return `1` if the array is smaller at the start
     * or is a palindrome,
     * `-1` if smaller at the end
     */
    @JvmStatic
    fun increasingDirection(pts: Array<Coordinate>): Int {
      for (i in 0 until pts.size / 2) {
        val j = pts.size - 1 - i
        // skip equal points on both ends
        val comp = pts[i].compareTo(pts[j])
        if (comp != 0)
          return comp
      }
      // array must be a palindrome - defined to be in positive direction
      return 1
    }

    /**
     * Determines whether two [Coordinate] arrays of equal length
     * are equal in opposite directions.
     *
     * @param pts1
     * @param pts2
     * @return `true` if the two arrays are equal in opposite directions.
     */
    private fun isEqualReversed(pts1: Array<Coordinate>, pts2: Array<Coordinate>): Boolean {
      for (i in 0 until pts1.size) {
        val p1 = pts1[i]
        val p2 = pts2[pts1.size - i - 1]
        if (p1.compareTo(p2) != 0)
          return false
      }
      return true
    }

    /**
     * Creates a deep copy of the argument [Coordinate] array.
     *
     * @param coordinates an array of Coordinates
     * @return a deep copy of the input
     */
    @JvmStatic
    fun copyDeep(coordinates: Array<Coordinate>): Array<Coordinate> {
      @Suppress("UNCHECKED_CAST")
      val copy = arrayOfNulls<Coordinate>(coordinates.size) as Array<Coordinate>
      for (i in 0 until coordinates.size) {
        copy[i] = coordinates[i].copy()
      }
      return copy
    }

    /**
     * Creates a deep copy of a given section of a source [Coordinate] array
     * into a destination Coordinate array.
     * The destination array must be an appropriate size to receive
     * the copied coordinates.
     *
     * @param src       an array of Coordinates
     * @param srcStart  the index to start copying from
     * @param dest      the
     * @param destStart the destination index to start copying to
     * @param length    the number of items to copy
     */
    @JvmStatic
    fun copyDeep(src: Array<Coordinate>, srcStart: Int, dest: Array<Coordinate>, destStart: Int, length: Int) {
      for (i in 0 until length) {
        dest[destStart + i] = src[srcStart + i].copy()
      }
    }

    /**
     * Converts the given Collection of Coordinates into a Coordinate array.
     */
    @JvmStatic
    fun toCoordinateArray(coordList: Collection<Coordinate>): Array<Coordinate> {
      return coordList.toTypedArray()
    }

    /**
     * Tests whether [Coordinate.equals] returns true for any two consecutive Coordinates
     * in the given array.
     *
     * @param coord an array of coordinates
     * @return true if the array has repeated points
     */
    @JvmStatic
    fun hasRepeatedPoints(coord: Array<Coordinate>): Boolean {
      for (i in 1 until coord.size) {
        if (coord[i - 1].equals(coord[i])) {
          return true
        }
      }
      return false
    }

    /**
     * Returns either the given coordinate array if its length is greater than the
     * given amount, or an empty coordinate array.
     */
    @JvmStatic
    fun atLeastNCoordinatesOrNothing(n: Int, c: Array<Coordinate>): Array<Coordinate> {
      return if (c.size >= n) c else emptyArray()
    }

    /**
     * If the coordinate array argument has repeated points,
     * constructs a new array containing no repeated points.
     * Otherwise, returns the argument.
     *
     * @param coord an array of coordinates
     * @return the array with repeated coordinates removed
     * @see #hasRepeatedPoints(Coordinate[])
     */
    @JvmStatic
    fun removeRepeatedPoints(coord: Array<Coordinate>): Array<Coordinate> {
      if (!hasRepeatedPoints(coord)) return coord
      val coordList = CoordinateList(coord, false)
      return coordList.toCoordinateArray()
    }

    /**
     * Tests whether an array has any repeated or invalid coordinates.
     *
     * @param coord an array of coordinates
     * @return true if the array contains repeated or invalid coordinates
     * @see Coordinate#isValid()
     */
    @JvmStatic
    fun hasRepeatedOrInvalidPoints(coord: Array<Coordinate>): Boolean {
      for (i in 1 until coord.size) {
        if (!coord[i].isValid())
          return true
        if (coord[i - 1].equals(coord[i])) {
          return true
        }
      }
      return false
    }

    /**
     * If the coordinate array argument has repeated or invalid points,
     * constructs a new array containing no repeated points.
     * Otherwise, returns the argument.
     *
     * @param coord an array of coordinates
     * @return the array with repeated and invalid coordinates removed
     * @see #hasRepeatedOrInvalidPoints(Coordinate[])
     * @see Coordinate#isValid()
     */
    @JvmStatic
    fun removeRepeatedOrInvalidPoints(coord: Array<Coordinate>): Array<Coordinate> {
      if (!hasRepeatedOrInvalidPoints(coord)) return coord
      val coordList = CoordinateList()
      for (i in 0 until coord.size) {
        if (!coord[i].isValid()) continue
        coordList.add(coord[i], false)
      }
      return coordList.toCoordinateArray()
    }

    /**
     * Collapses a coordinate array to remove all null elements.
     *
     * @param coord the coordinate array to collapse
     * @return an array containing only non-null elements
     */
    @JvmStatic
    fun removeNull(coord: Array<Coordinate?>): Array<Coordinate> {
      var nonNull = 0
      for (i in 0 until coord.size) {
        if (coord[i] != null) nonNull++
      }
      @Suppress("UNCHECKED_CAST")
      val newCoord = arrayOfNulls<Coordinate>(nonNull) as Array<Coordinate>
      // empty case
      if (nonNull == 0) return newCoord

      var j = 0
      for (i in 0 until coord.size) {
        if (coord[i] != null) newCoord[j++] = coord[i]!!
      }
      return newCoord
    }

    /**
     * Reverses the coordinates in an array in-place.
     */
    @JvmStatic
    fun reverse(coord: Array<Coordinate>) {
      if (coord.size <= 1)
        return

      val last = coord.size - 1
      val mid = last / 2
      for (i in 0..mid) {
        val tmp = coord[i]
        coord[i] = coord[last - i]
        coord[last - i] = tmp
      }
    }

    /**
     * Returns true if the two arrays are identical, both null, or pointwise
     * equal (as compared using Coordinate#equals)
     *
     * @see Coordinate#equals(Object)
     */
    @JvmStatic
    fun equals(coord1: Array<Coordinate>?, coord2: Array<Coordinate>?): Boolean {
      if (coord1 === coord2) return true
      if (coord1 == null || coord2 == null) return false
      if (coord1.size != coord2.size) return false
      for (i in 0 until coord1.size) {
        if (!coord1[i].equals(coord2[i])) return false
      }
      return true
    }

    /**
     * Returns true if the two arrays are identical, both null, or pointwise
     * equal, using a user-defined [Comparator] for [Coordinate] s
     *
     * @param coord1               an array of Coordinates
     * @param coord2               an array of Coordinates
     * @param coordinateComparator a Comparator for Coordinates
     */
    @JvmStatic
    fun equals(
      coord1: Array<Coordinate>?,
      coord2: Array<Coordinate>?,
      coordinateComparator: Comparator<in Coordinate>
    ): Boolean {
      if (coord1 === coord2) return true
      if (coord1 == null || coord2 == null) return false
      if (coord1.size != coord2.size) return false
      for (i in 0 until coord1.size) {
        if (coordinateComparator.compare(coord1[i], coord2[i]) != 0)
          return false
      }
      return true
    }

    /**
     * Returns the minimum coordinate, using the usual lexicographic comparison.
     *
     * @param coordinates the array to search
     * @return the minimum coordinate in the array, found using `compareTo`
     * @see Coordinate#compareTo(Coordinate)
     */
    @JvmStatic
    fun minCoordinate(coordinates: Array<Coordinate>): Coordinate? {
      var minCoord: Coordinate? = null
      for (i in 0 until coordinates.size) {
        if (minCoord == null || minCoord.compareTo(coordinates[i]) > 0) {
          minCoord = coordinates[i]
        }
      }
      return minCoord
    }

    /**
     * Shifts the positions of the coordinates until `firstCoordinate`
     * is first.
     *
     * @param coordinates     the array to rearrange
     * @param firstCoordinate the coordinate to make first
     */
    @JvmStatic
    fun scroll(coordinates: Array<Coordinate>, firstCoordinate: Coordinate) {
      val i = indexOf(firstCoordinate, coordinates)
      scroll(coordinates, i)
    }

    /**
     * Shifts the positions of the coordinates until the coordinate
     * at `firstCoordinate` is first.
     *
     * @param coordinates            the array to rearrange
     * @param indexOfFirstCoordinate the index of the coordinate to make first
     */
    @JvmStatic
    fun scroll(coordinates: Array<Coordinate>, indexOfFirstCoordinate: Int) {
      scroll(coordinates, indexOfFirstCoordinate, isRing(coordinates))
    }

    /**
     * Shifts the positions of the coordinates until the coordinate
     * at `indexOfFirstCoordinate` is first.
     * <p/>
     * If `ensureRing` is `true`, first and last
     * coordinate of the returned array are equal.
     *
     * @param coordinates            the array to rearrange
     * @param indexOfFirstCoordinate the index of the coordinate to make first
     * @param ensureRing             flag indicating if returned array should form a ring.
     */
    @JvmStatic
    fun scroll(coordinates: Array<Coordinate>, indexOfFirstCoordinate: Int, ensureRing: Boolean) {
      val i = indexOfFirstCoordinate
      if (i <= 0) return

      val newCoordinates = arrayOfNulls<Coordinate>(coordinates.size)
      if (!ensureRing) {
        coordinates.copyInto(newCoordinates, destinationOffset = 0, startIndex = i, endIndex = coordinates.size)
        coordinates.copyInto(newCoordinates, destinationOffset = coordinates.size - i, startIndex = 0, endIndex = i)
      } else {
        val last = coordinates.size - 1

        // fill in values
        var j = 0
        while (j < last) {
          newCoordinates[j] = coordinates[(i + j) % last]
          j++
        }

        // Fix the ring (first == last)
        newCoordinates[j] = newCoordinates[0]!!.copy()
      }
      @Suppress("UNCHECKED_CAST")
      (newCoordinates as Array<Coordinate>).copyInto(coordinates, destinationOffset = 0, startIndex = 0, endIndex = coordinates.size)
    }

    /**
     * Returns the index of `coordinate` in `coordinates`.
     * The first position is 0; the second, 1; etc.
     *
     * @param coordinate  the `Coordinate` to search for
     * @param coordinates the array to search
     * @return the position of `coordinate`, or -1 if it is
     * not found
     */
    @JvmStatic
    fun indexOf(coordinate: Coordinate, coordinates: Array<Coordinate>): Int {
      for (i in 0 until coordinates.size) {
        if (coordinate.equals(coordinates[i])) {
          return i
        }
      }
      return -1
    }

    /**
     * Extracts a subsequence of the input [Coordinate] array
     * from indices `start` to
     * `end` (inclusive).
     * The input indices are clamped to the array size;
     * If the end index is less than the start index,
     * the extracted array will be empty.
     *
     * @param pts   the input array
     * @param start the index of the start of the subsequence to extract
     * @param end   the index of the end of the subsequence to extract
     * @return a subsequence of the input array
     */
    @JvmStatic
    fun extract(pts: Array<Coordinate>, start: Int, end: Int): Array<Coordinate> {
      val startClamped = MathUtil.clamp(start, 0, pts.size)
      val endClamped = MathUtil.clamp(end, -1, pts.size)

      var npts = endClamped - startClamped + 1
      if (endClamped < 0) npts = 0
      if (startClamped >= pts.size) npts = 0
      if (endClamped < startClamped) npts = 0

      @Suppress("UNCHECKED_CAST")
      val extractPts = arrayOfNulls<Coordinate>(npts) as Array<Coordinate>
      if (npts == 0) return extractPts

      var iPts = 0
      for (i in startClamped..endClamped) {
        extractPts[iPts++] = pts[i]
      }
      return extractPts
    }

    /**
     * Computes the envelope of the coordinates.
     *
     * @param coordinates the coordinates to scan
     * @return the envelope of the coordinates
     */
    @JvmStatic
    fun envelope(coordinates: Array<Coordinate>): Envelope {
      val env = Envelope()
      for (i in 0 until coordinates.size) {
        env.expandToInclude(coordinates[i])
      }
      return env
    }

    /**
     * Extracts the coordinates which intersect an [Envelope].
     *
     * @param coordinates the coordinates to scan
     * @param env         the envelope to intersect with
     * @return an array of the coordinates which intersect the envelope
     */
    @JvmStatic
    fun intersection(coordinates: Array<Coordinate>, env: Envelope): Array<Coordinate> {
      val coordList = CoordinateList()
      for (i in 0 until coordinates.size) {
        if (env.intersects(coordinates[i]))
          coordList.add(coordinates[i], true)
      }
      return coordList.toCoordinateArray()
    }
  }
}
