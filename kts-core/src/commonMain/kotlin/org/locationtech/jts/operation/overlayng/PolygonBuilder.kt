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
package org.locationtech.jts.operation.overlayng

import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.Polygon
import org.locationtech.jts.geom.TopologyException
import org.locationtech.jts.util.Assert

class PolygonBuilder(
  resultAreaEdges: List<OverlayEdge>,
  geomFact: GeometryFactory,
  private val isEnforcePolygonal: Boolean
) {

  private val geometryFactory: GeometryFactory = geomFact
  private val shellList: MutableList<OverlayEdgeRing> = ArrayList()
  private val freeHoleList: MutableList<OverlayEdgeRing> = ArrayList()

  init {
    buildRings(resultAreaEdges)
  }

  constructor(resultAreaEdges: List<OverlayEdge>, geomFact: GeometryFactory) :
    this(resultAreaEdges, geomFact, true)

  fun getPolygons(): MutableList<Polygon> {
    return computePolygons(shellList)
  }

  fun getShellRings(): MutableList<OverlayEdgeRing> {
    return shellList
  }

  private fun computePolygons(shellList: List<OverlayEdgeRing>): MutableList<Polygon> {
    val resultPolyList = ArrayList<Polygon>()
    // add Polygons for all shells
    for (er in shellList) {
      val poly = er.toPolygon(geometryFactory)
      resultPolyList.add(poly)
    }
    return resultPolyList
  }

  private fun buildRings(resultAreaEdges: List<OverlayEdge>) {
    linkResultAreaEdgesMax(resultAreaEdges)
    val maxRings = buildMaximalRings(resultAreaEdges)
    buildMinimalRings(maxRings)
    placeFreeHoles(shellList, freeHoleList)
    //Assert: every hole on freeHoleList has a shell assigned to it
  }

  private fun linkResultAreaEdgesMax(resultEdges: List<OverlayEdge>) {
    for (edge in resultEdges) {
      // TODO: find some way to skip nodes which are already linked
      MaximalEdgeRing.linkResultAreaMaxRingAtNode(edge)
    }
  }

  private fun buildMinimalRings(maxRings: List<MaximalEdgeRing>) {
    for (erMax in maxRings) {
      val minRings = erMax.buildMinimalRings(geometryFactory)
      assignShellsAndHoles(minRings)
    }
  }

  private fun assignShellsAndHoles(minRings: List<OverlayEdgeRing>) {
    /**
     * Two situations may occur:
     * - the rings are a shell and some holes
     * - rings are a set of holes
     * This code identifies the situation
     * and places the rings appropriately
     */
    val shell = findSingleShell(minRings)
    if (shell != null) {
      assignHoles(shell, minRings)
      shellList.add(shell)
    } else {
      // all rings are holes; their shell will be found later
      freeHoleList.addAll(minRings)
    }
  }

  /**
   * Finds the single shell, if any, out of
   * a list of minimal rings derived from a maximal ring.
   *
   * @return the shell ring, if there is one
   * or null, if all rings are holes
   */
  private fun findSingleShell(edgeRings: List<OverlayEdgeRing>): OverlayEdgeRing? {
    var shellCount = 0
    var shell: OverlayEdgeRing? = null
    for (er in edgeRings) {
      if (!er.isHole()) {
        shell = er
        shellCount++
      }
    }
    Assert.isTrue(shellCount <= 1, "found two shells in EdgeRing list")
    return shell
  }

  /**
   * Place holes have not yet been assigned to a shell.
   *
   * @throws TopologyException if a hole cannot be assigned to a shell
   */
  private fun placeFreeHoles(shellList: List<OverlayEdgeRing>, freeHoleList: List<OverlayEdgeRing>) {
    // TODO: use a spatial index to improve performance
    for (hole in freeHoleList) {
      // only place this hole if it doesn't yet have a shell
      if (hole.getShell() == null) {
        val shell = hole.findEdgeRingContaining(shellList)
        // only when building a polygon-valid result
        if (isEnforcePolygonal && shell == null) {
          throw TopologyException("unable to assign free hole to a shell", hole.getCoordinate())
        }
        hole.setShell(shell)
      }
    }
  }

  companion object {
    /**
     * For all OverlayEdges in result, form them into MaximalEdgeRings
     */
    private fun buildMaximalRings(edges: Collection<OverlayEdge>): MutableList<MaximalEdgeRing> {
      val edgeRings = ArrayList<MaximalEdgeRing>()
      for (e in edges) {
        if (e.isInResultArea() && e.getLabel().isBoundaryEither()) {
          // if this edge has not yet been processed
          if (e.getEdgeRingMax() == null) {
            val er = MaximalEdgeRing(e)
            edgeRings.add(er)
          }
        }
      }
      return edgeRings
    }

    /**
     * For the set of minimal rings comprising a maximal ring,
     * assigns the holes to the shell known to contain them.
     */
    private fun assignHoles(shell: OverlayEdgeRing, edgeRings: List<OverlayEdgeRing>) {
      for (er in edgeRings) {
        if (er.isHole()) {
          er.setShell(shell)
        }
      }
    }
  }
}
