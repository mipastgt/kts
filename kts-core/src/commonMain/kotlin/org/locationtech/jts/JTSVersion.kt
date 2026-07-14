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

package org.locationtech.jts

import kotlin.jvm.JvmStatic
import kotlin.jvm.JvmField

/**
 * JTS API version information.
 *
 * Versions consist of a 3-part version number: `major.minor.patch`
 * An optional release status string may be present in the string version of
 * the version.
 *
 */
class JTSVersion private constructor() {

  /**
   * Gets the major number of the release version.
   *
   * @return the major number of the release version.
   */
  fun getMajor(): Int = MAJOR

  /**
   * Gets the minor number of the release version.
   *
   * @return the minor number of the release version.
   */
  fun getMinor(): Int = MINOR

  /**
   * Gets the patch number of the release version.
   *
   * @return the patch number of the release version.
   */
  fun getPatch(): Int = PATCH

  /**
   * Gets the full version number, suitable for display.
   *
   * @return the full version number, suitable for display.
   */
  override fun toString(): String {
    val ver = "$MAJOR.$MINOR.$PATCH"
    if (RELEASE_INFO.isNotEmpty())
      return "$ver $RELEASE_INFO"
    return ver
  }

  companion object {
    /**
     * The current version number of the JTS API.
     */
    @JvmField
    val CURRENT_VERSION: JTSVersion = JTSVersion()

    /**
     * The major version number.
     */
    const val MAJOR: Int = 1

    /**
     * The minor version number.
     */
    const val MINOR: Int = 20

    /**
     * The patch version number.
     */
    const val PATCH: Int = 0

    /**
     * An optional string providing further release info (such as "alpha 1");
     */
    private const val RELEASE_INFO: String = ""

    /**
     * Prints the current JTS version to stdout.
     *
     * @param args the command-line arguments (none are required).
     */
    @JvmStatic
    fun main(args: Array<String>) {
      println(CURRENT_VERSION)
    }
  }
}
