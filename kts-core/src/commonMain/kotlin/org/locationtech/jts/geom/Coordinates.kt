/*
 * Copyright (c) 2018 Contributors to the Eclipse Foundation
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
 * Useful utility functions for handling Coordinate objects.
 */
class Coordinates {
  companion object {
    /**
     * Factory method providing access to common Coordinate implementations.
     *
     * @param dimension
     * @return created coordinate
     */
    @JvmStatic
    fun create(dimension: Int): Coordinate {
      return create(dimension, 0)
    }

    /**
     * Factory method providing access to common Coordinate implementations.
     *
     * @param dimension
     * @param measures
     * @return created coordinate
     */
    @JvmStatic
    fun create(dimension: Int, measures: Int): Coordinate {
      if (dimension == 2) {
        return CoordinateXY()
      } else if (dimension == 3 && measures == 0) {
        return Coordinate()
      } else if (dimension == 3 && measures == 1) {
        return CoordinateXYM()
      } else if (dimension == 4 && measures == 1) {
        return CoordinateXYZM()
      }
      return Coordinate()
    }

    /**
     * Determine dimension based on subclass of {@link Coordinate}.
     *
     * @param coordinate supplied coordinate
     * @return number of ordinates recorded
     */
    @JvmStatic
    fun dimension(coordinate: Coordinate?): Int {
      if (coordinate is CoordinateXY) {
        return 2
      } else if (coordinate is CoordinateXYM) {
        return 3
      } else if (coordinate is CoordinateXYZM) {
        return 4
      } else if (coordinate is Coordinate) {
        return 3
      }
      return 3
    }

    /**
     * Check if coordinate can store Z valye, based on subclass of {@link Coordinate}.
     *
     * @param coordinate supplied coordinate
     * @return true if setZ is available
     */
    @JvmStatic
    fun hasZ(coordinate: Coordinate?): Boolean {
      if (coordinate is CoordinateXY) {
        return false
      } else if (coordinate is CoordinateXYM) {
        return false
      } else if (coordinate is CoordinateXYZM) {
        return true
      } else if (coordinate is Coordinate) {
        return true
      }
      return true
    }

    /**
     * Determine number of measures based on subclass of {@link Coordinate}.
     *
     * @param coordinate supplied coordinate
     * @return number of measures recorded
     */
    @JvmStatic
    fun measures(coordinate: Coordinate?): Int {
      if (coordinate is CoordinateXY) {
        return 0
      } else if (coordinate is CoordinateXYM) {
        return 1
      } else if (coordinate is CoordinateXYZM) {
        return 1
      } else if (coordinate is Coordinate) {
        return 0
      }
      return 0
    }
  }
}
