/*
 * Copyright (c) 2019 Martin Davis.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.locationtech.jts.operation.polygonize

import kotlin.jvm.JvmStatic

import org.locationtech.jts.geom.Envelope
import org.locationtech.jts.index.SpatialIndex
import org.locationtech.jts.index.strtree.STRtree

/**
 * Assigns hole rings to shell rings
 * during polygonization.
 * Uses spatial indexing to improve performance
 * of shell lookup.
 *
 * @author mdavis
 */
class HoleAssigner
/**
 * Creates a new hole assigner.
 *
 * @param shells the shells to be assigned to
 */
(private val shells: List<EdgeRing>) {

  private lateinit var shellIndex: SpatialIndex

  init {
    buildIndex()
  }

  private fun buildIndex() {
    shellIndex = STRtree()
    for (shell in shells) {
      shellIndex.insert(shell.getRing()!!.getEnvelopeInternal(), shell)
    }
  }

  /**
   * Assigns holes to the shells.
   *
   * @param holeList list of hole rings to assign
   */
  fun assignHolesToShells(holeList: List<EdgeRing>) {
    for (holeER in holeList) {
      assignHoleToShell(holeER)
    }
  }

  private fun assignHoleToShell(holeER: EdgeRing) {
    val shell = findShellContaining(holeER)
    if (shell != null) {
      shell.addHole(holeER)
    }
  }

  @Suppress("UNCHECKED_CAST")
  private fun queryOverlappingShells(ringEnv: Envelope): List<EdgeRing> {
    return shellIndex.query(ringEnv) as List<EdgeRing>
  }

  /**
   * Find the innermost enclosing shell EdgeRing containing the argument EdgeRing, if any.
   *
   * @return containing shell EdgeRing, if there is one
   * or null if no containing EdgeRing is found
   */
  private fun findShellContaining(testEr: EdgeRing): EdgeRing? {
    val testEnv = testEr.getRing()!!.getEnvelopeInternal()
    val candidateShells = queryOverlappingShells(testEnv)
    return EdgeRing.findEdgeRingContaining(testEr, candidateShells)
  }

  companion object {
    /**
     * Assigns hole rings to shell rings.
     *
     * @param holes list of hole rings to assign
     * @param shells list of shell rings
     */
    @JvmStatic
    fun assignHolesToShells(holes: List<EdgeRing>, shells: List<EdgeRing>) {
      val assigner = HoleAssigner(shells)
      assigner.assignHolesToShells(holes)
    }
  }
}
