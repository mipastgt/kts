/*
 * Copyright (c) 2018 Felix Obermaier
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.locationtech.jts.io

import kotlin.jvm.JvmStatic

/**
 * An enumeration of possible Well-Known-Text or Well-Known-Binary ordinates.
 *
 * Intended to be used as a `Set<Ordinate>`; the create methods [createXY], [createXYM],
 * [createXYZ] and [createXYZM] return a fresh mutable set each call.
 */
enum class Ordinate {
  /** X-ordinate  */
  X,

  /** Y-ordinate  */
  Y,

  /** Z-ordinate  */
  Z,

  /** Measure-ordinate  */
  M;

  companion object {
    /**
     * A fresh mutable set of the X and Y ordinates.
     */
    @JvmStatic
    fun createXY(): MutableSet<Ordinate> = mutableSetOf(X, Y)

    /**
     * A fresh mutable set of the X, Y and Z ordinates.
     */
    @JvmStatic
    fun createXYZ(): MutableSet<Ordinate> = mutableSetOf(X, Y, Z)

    /**
     * A fresh mutable set of the X, Y and M ordinates.
     */
    @JvmStatic
    fun createXYM(): MutableSet<Ordinate> = mutableSetOf(X, Y, M)

    /**
     * A fresh mutable set of the X, Y, Z and M ordinates.
     */
    @JvmStatic
    fun createXYZM(): MutableSet<Ordinate> = mutableSetOf(X, Y, Z, M)
  }
}
