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

package org.locationtech.jts.util

import kotlin.jvm.JvmStatic
import kotlin.math.abs

class NumberUtil {
  companion object {
    @JvmStatic
    fun equalsWithTolerance(x1: Double, x2: Double, tolerance: Double): Boolean {
      return abs(x1 - x2) <= tolerance
    }
  }
}
