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
import kotlin.math.ceil

/**
 * Utility functions to report JVM memory usage.
 *
 * @author mbdavis
 *
 */
class Memory {
  companion object {
    @JvmStatic
    fun used(): Long {
      val runtime = Runtime.getRuntime()
      return runtime.totalMemory() - runtime.freeMemory()
    }

    @JvmStatic
    fun usedString(): String {
      return format(used())
    }

    @JvmStatic
    fun free(): Long {
      val runtime = Runtime.getRuntime()
      return runtime.freeMemory()
    }

    @JvmStatic
    fun freeString(): String {
      return format(free())
    }

    @JvmStatic
    fun total(): Long {
      val runtime = Runtime.getRuntime()
      return runtime.totalMemory()
    }

    @JvmStatic
    fun totalString(): String {
      return format(total())
    }

    @JvmStatic
    fun usedTotalString(): String {
      return ("Used: " + usedString()
        + "   Total: " + totalString())
    }

    @JvmStatic
    fun allString(): String {
      return ("Used: " + usedString()
        + "   Free: " + freeString()
        + "   Total: " + totalString())
    }

    const val KB = 1024.0
    const val MB = 1048576.0
    const val GB = 1073741824.0

    @JvmStatic
    fun format(mem: Long): String {
      if (mem < 2 * KB)
        return "$mem bytes"
      if (mem < 2 * MB)
        return "${round(mem / KB)} KB"
      if (mem < 2 * GB)
        return "${round(mem / MB)} MB"
      return "${round(mem / GB)} GB"
    }

    @JvmStatic
    fun round(d: Double): Double {
      return ceil(d * 100) / 100
    }
  }
}
