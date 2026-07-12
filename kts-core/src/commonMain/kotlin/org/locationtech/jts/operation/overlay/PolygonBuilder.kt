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
package org.locationtech.jts.operation.overlay

import org.locationtech.jts.algorithm.PointLocation
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.CoordinateArrays
import org.locationtech.jts.geom.Envelope
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.Polygon
import org.locationtech.jts.geom.TopologyException
import org.locationtech.jts.geomgraph.DirectedEdge
import org.locationtech.jts.geomgraph.EdgeRing
import org.locationtech.jts.geomgraph.PlanarGraph
import org.locationtech.jts.util.Assert

/**
 * Forms [Polygon]s out of a graph of [DirectedEdge]s.
 * The edges to use are marked as being in the result Area.
 *
 * @version 1.7
 */
class PolygonBuilder(private val geometryFactory: GeometryFactory) {

  private val shellList: MutableList<EdgeRing> = ArrayList()

  /**
   * Add a complete graph.
   * The graph is assumed to contain one or more polygons,
   * possibly with holes.
   */
  fun add(graph: PlanarGraph) {
    add(graph.getEdgeEnds(), graph.getNodes())
  }

  /**
   * Add a set of edges and nodes, which form a graph.
   * The graph is assumed to contain one or more polygons,
   * possibly with holes.
   */
  fun add(dirEdges: Collection<*>, nodes: Collection<*>) {
    PlanarGraph.linkResultDirectedEdges(nodes)
    val maxEdgeRings = buildMaximalEdgeRings(dirEdges)
    val freeHoleList: MutableList<EdgeRing> = ArrayList()
    val edgeRings = buildMinimalEdgeRings(maxEdgeRings, shellList, freeHoleList)
    sortShellsAndHoles(edgeRings, shellList, freeHoleList)
    placeFreeHoles(shellList, freeHoleList)
    //Assert: every hole on freeHoleList has a shell assigned to it
  }

  fun getPolygons(): MutableList<Polygon> {
    return computePolygons(shellList)
  }

  /**
   * for all DirectedEdges in result, form them into MaximalEdgeRings
   */
  private fun buildMaximalEdgeRings(dirEdges: Collection<*>): MutableList<MaximalEdgeRing> {
    val maxEdgeRings: MutableList<MaximalEdgeRing> = ArrayList()
    val it = dirEdges.iterator()
    while (it.hasNext()) {
      val de = it.next() as DirectedEdge
      if (de.isInResult() && de.getLabel()!!.isArea()) {
        // if this edge has not yet been processed
        if (de.getEdgeRing() == null) {
          val er = MaximalEdgeRing(de, geometryFactory)
          maxEdgeRings.add(er)
          er.setInResult()
//System.out.println("max node degree = " + er.getMaxDegree());
        }
      }
    }
    return maxEdgeRings
  }

  private fun buildMinimalEdgeRings(
    maxEdgeRings: MutableList<MaximalEdgeRing>,
    shellList: MutableList<EdgeRing>,
    freeHoleList: MutableList<EdgeRing>
  ): MutableList<EdgeRing> {
    val edgeRings: MutableList<EdgeRing> = ArrayList()
    val it = maxEdgeRings.iterator()
    while (it.hasNext()) {
      val er = it.next()
      if (er.getMaxNodeDegree() > 2) {
        er.linkDirectedEdgesForMinimalEdgeRings()
        val minEdgeRings = er.buildMinimalRings()
        // at this point we can go ahead and attempt to place holes, if this EdgeRing is a polygon
        val shell = findShell(minEdgeRings)
        if (shell != null) {
          placePolygonHoles(shell, minEdgeRings)
          shellList.add(shell)
        } else {
          freeHoleList.addAll(minEdgeRings)
        }
      } else {
        edgeRings.add(er)
      }
    }
    return edgeRings
  }

  /**
   * This method takes a list of MinimalEdgeRings derived from a MaximalEdgeRing,
   * and tests whether they form a Polygon.  This is the case if there is a single shell
   * in the list.  In this case the shell is returned.
   * The other possibility is that they are a series of connected holes, in which case
   * no shell is returned.
   *
   * @return the shell EdgeRing, if there is one
   * or null, if all the rings are holes
   */
  private fun findShell(minEdgeRings: MutableList<EdgeRing>): EdgeRing? {
    var shellCount = 0
    var shell: EdgeRing? = null
    val it = minEdgeRings.iterator()
    while (it.hasNext()) {
      val er = it.next()
      if (!er.isHole()) {
        shell = er
        shellCount++
      }
    }
    Assert.isTrue(shellCount <= 1, "found two shells in MinimalEdgeRing list")
    return shell
  }

  /**
   * This method assigns the holes for a Polygon (formed from a list of
   * MinimalEdgeRings) to its shell.
   * Determining the holes for a MinimalEdgeRing polygon serves two purposes:
   *
   *  * it is faster than using a point-in-polygon check later on.
   *  * it ensures correctness, since if the PIP test was used the point
   * chosen might lie on the shell, which might return an incorrect result from the
   * PIP test
   *
   */
  private fun placePolygonHoles(shell: EdgeRing, minEdgeRings: MutableList<EdgeRing>) {
    val it = minEdgeRings.iterator()
    while (it.hasNext()) {
      val er = it.next()
      if (er.isHole()) {
        er.setShell(shell)
      }
    }
  }

  /**
   * For all rings in the input list,
   * determine whether the ring is a shell or a hole
   * and add it to the appropriate list.
   * Due to the way the DirectedEdges were linked,
   * a ring is a shell if it is oriented CW, a hole otherwise.
   */
  private fun sortShellsAndHoles(
    edgeRings: MutableList<EdgeRing>,
    shellList: MutableList<EdgeRing>,
    freeHoleList: MutableList<EdgeRing>
  ) {
    val it = edgeRings.iterator()
    while (it.hasNext()) {
      val er = it.next()
//      er.setInResult();
      if (er.isHole()) {
        freeHoleList.add(er)
      } else {
        shellList.add(er)
      }
    }
  }

  /**
   * This method determines finds a containing shell for all holes
   * which have not yet been assigned to a shell.
   * These "free" holes should
   * all be **properly** contained in their parent shells, so it is safe to use the
   * `findEdgeRingContaining` method.
   *
   * @throws TopologyException if a hole cannot be assigned to a shell
   */
  private fun placeFreeHoles(shellList: MutableList<EdgeRing>, freeHoleList: MutableList<EdgeRing>) {
    val it = freeHoleList.iterator()
    while (it.hasNext()) {
      val hole = it.next()
      // only place this hole if it doesn't yet have a shell
      if (hole.getShell() == null) {
        val shell = findEdgeRingContaining(hole, shellList)
          ?: throw TopologyException("unable to assign hole to a shell", hole.getCoordinate(0))
//        Assert.isTrue(shell != null, "unable to assign hole to a shell");
        hole.setShell(shell)
      }
    }
  }

  private fun computePolygons(shellList: MutableList<EdgeRing>): MutableList<Polygon> {
    val resultPolyList: MutableList<Polygon> = ArrayList()
    // add Polygons for all shells
    val it = shellList.iterator()
    while (it.hasNext()) {
      val er = it.next()
      val poly = er.toPolygon(geometryFactory)
      resultPolyList.add(poly)
    }
    return resultPolyList
  }

  companion object {
    /**
     * Find the innermost enclosing shell EdgeRing containing the argument EdgeRing, if any.
     * The innermost enclosing ring is the *smallest* enclosing ring.
     *
     * @return containing EdgeRing, if there is one
     * or null if no containing EdgeRing is found
     */
    private fun findEdgeRingContaining(testEr: EdgeRing, shellList: MutableList<EdgeRing>): EdgeRing? {
      val testRing = testEr.getLinearRing()!!
      val testEnv = testRing.getEnvelopeInternal()
      var testPt: Coordinate? = testRing.getCoordinateN(0)

      var minShell: EdgeRing? = null
      var minShellEnv: Envelope? = null
      val it = shellList.iterator()
      while (it.hasNext()) {
        val tryShell = it.next()
        val tryShellRing = tryShell.getLinearRing()!!
        val tryShellEnv = tryShellRing.getEnvelopeInternal()
        // the hole envelope cannot equal the shell envelope
        // (also guards against testing rings against themselves)
        if (tryShellEnv == testEnv) continue
        // hole must be contained in shell
        if (!tryShellEnv.contains(testEnv)) continue

        testPt = CoordinateArrays.ptNotInList(testRing.getCoordinates(), tryShellRing.getCoordinates())
        var isContained = false
        if (PointLocation.isInRing(testPt!!, tryShellRing.getCoordinates()))
          isContained = true

        // check if this new containing ring is smaller than the current minimum ring
        if (isContained) {
          if (minShell == null || minShellEnv!!.contains(tryShellEnv)) {
            minShell = tryShell
            minShellEnv = minShell.getLinearRing()!!.getEnvelopeInternal()
          }
        }
      }
      return minShell
    }
  }
}
