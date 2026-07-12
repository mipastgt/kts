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

package org.locationtech.jts.operation.distance3d

import kotlin.jvm.JvmStatic

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.CoordinateSequence
import org.locationtech.jts.geom.Envelope

/**
 * A CoordinateSequence wrapper which
 * projects 3D coordinates into one of the
 * three Cartesian axis planes,
 * using the standard orthonormal projection
 * (i.e. simply selecting the appropriate ordinates into the XY ordinates).
 * The projected data is represented as 2D coordinates.
 *
 * @author mdavis
 */
class AxisPlaneCoordinateSequence private constructor(
  private val seq: CoordinateSequence,
  private val indexMap: IntArray
) : CoordinateSequence {

  override fun getDimension(): Int {
    return 2
  }

  override fun getCoordinate(i: Int): Coordinate {
    return getCoordinateCopy(i)
  }

  override fun getCoordinateCopy(i: Int): Coordinate {
    return Coordinate(getX(i), getY(i), getZ(i))
  }

  override fun getCoordinate(index: Int, coord: Coordinate) {
    coord.x = getOrdinate(index, CoordinateSequence.X)
    coord.y = getOrdinate(index, CoordinateSequence.Y)
    coord.setZ(getOrdinate(index, CoordinateSequence.Z))
  }

  override fun getX(index: Int): Double {
    return getOrdinate(index, CoordinateSequence.X)
  }

  override fun getY(index: Int): Double {
    return getOrdinate(index, CoordinateSequence.Y)
  }

  override fun getZ(index: Int): Double {
    return getOrdinate(index, CoordinateSequence.Z)
  }

  override fun getOrdinate(index: Int, ordinateIndex: Int): Double {
    // Z ord is always 0
    if (ordinateIndex > 1) return 0.0
    return seq.getOrdinate(index, indexMap[ordinateIndex])
  }

  override fun size(): Int {
    return seq.size()
  }

  override fun setOrdinate(index: Int, ordinateIndex: Int, value: Double) {
    throw UnsupportedOperationException()
  }

  override fun toCoordinateArray(): Array<Coordinate> {
    throw UnsupportedOperationException()
  }

  override fun expandEnvelope(env: Envelope): Envelope {
    throw UnsupportedOperationException()
  }

  override fun clone(): Any {
    throw UnsupportedOperationException()
  }

  override fun copy(): AxisPlaneCoordinateSequence {
    throw UnsupportedOperationException()
  }

  companion object {
    /**
     * Creates a wrapper projecting to the XY plane.
     *
     * @param seq the sequence to be projected
     * @return a sequence which projects coordinates
     */
    @JvmStatic
    fun projectToXY(seq: CoordinateSequence): CoordinateSequence {
      /**
       * This is just a no-op, but return a wrapper
       * to allow better testing
       */
      return AxisPlaneCoordinateSequence(seq, XY_INDEX)
    }

    /**
     * Creates a wrapper projecting to the XZ plane.
     *
     * @param seq the sequence to be projected
     * @return a sequence which projects coordinates
     */
    @JvmStatic
    fun projectToXZ(seq: CoordinateSequence): CoordinateSequence {
      return AxisPlaneCoordinateSequence(seq, XZ_INDEX)
    }

    /**
     * Creates a wrapper projecting to the YZ plane.
     *
     * @param seq the sequence to be projected
     * @return a sequence which projects coordinates
     */
    @JvmStatic
    fun projectToYZ(seq: CoordinateSequence): CoordinateSequence {
      return AxisPlaneCoordinateSequence(seq, YZ_INDEX)
    }

    private val XY_INDEX = intArrayOf(0, 1)
    private val XZ_INDEX = intArrayOf(0, 2)
    private val YZ_INDEX = intArrayOf(1, 2)
  }
}
