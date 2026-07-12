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
package org.locationtech.jts.operation.linemerge

import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryComponentFilter
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.planargraph.GraphComponent
import org.locationtech.jts.planargraph.Node
import org.locationtech.jts.util.Assert

/**
 * Merges a collection of linear components to form maximal-length linestrings.
 *
 * @version 1.7
 */
class LineMerger {
  private val graph = LineMergeGraph()
  private var mergedLineStrings: MutableCollection<LineString>? = null
  private var factory: GeometryFactory? = null

  /**
   * Adds a Geometry to be processed. May be called multiple times.
   * Any dimension of Geometry may be added; the constituent linework will be
   * extracted.
   *
   * @param geometry geometry to be line-merged
   */
  fun add(geometry: Geometry) {
    geometry.apply(object : GeometryComponentFilter {
      override fun filter(component: Geometry) {
        if (component is LineString) {
          add(component)
        }
      }
    })
  }

  /**
   * Adds a collection of Geometries to be processed. May be called multiple times.
   * Any dimension of Geometry may be added; the constituent linework will be
   * extracted.
   *
   * @param geometries the geometries to be line-merged
   */
  fun add(geometries: Collection<*>) {
    mergedLineStrings = null
    val i = geometries.iterator()
    while (i.hasNext()) {
      val geometry = i.next() as Geometry
      add(geometry)
    }
  }

  private fun add(lineString: LineString) {
    if (factory == null) {
      this.factory = lineString.getFactory()
    }
    graph.addEdge(lineString)
  }

  private var edgeStrings: MutableList<EdgeString>? = null

  private fun merge() {
    if (mergedLineStrings != null) {
      return
    }

    // reset marks (this allows incremental processing)
    GraphComponent.setMarked(graph.nodeIterator(), false)
    GraphComponent.setMarked(graph.edgeIterator(), false)

    edgeStrings = ArrayList()
    buildEdgeStringsForObviousStartNodes()
    buildEdgeStringsForIsolatedLoops()
    val mergedLineStrings = ArrayList<LineString>()
    this.mergedLineStrings = mergedLineStrings
    for (edgeString in edgeStrings!!) {
      mergedLineStrings.add(edgeString.toLineString())
    }
  }

  private fun buildEdgeStringsForObviousStartNodes() {
    buildEdgeStringsForNonDegree2Nodes()
  }

  private fun buildEdgeStringsForIsolatedLoops() {
    buildEdgeStringsForUnprocessedNodes()
  }

  private fun buildEdgeStringsForUnprocessedNodes() {
    val i = graph.getNodes().iterator()
    while (i.hasNext()) {
      val node = i.next()
      if (!node.isMarked()) {
        Assert.isTrue(node.getDegree() == 2)
        buildEdgeStringsStartingAt(node)
        node.setMarked(true)
      }
    }
  }

  private fun buildEdgeStringsForNonDegree2Nodes() {
    val i = graph.getNodes().iterator()
    while (i.hasNext()) {
      val node = i.next()
      if (node.getDegree() != 2) {
        buildEdgeStringsStartingAt(node)
        node.setMarked(true)
      }
    }
  }

  private fun buildEdgeStringsStartingAt(node: Node) {
    val i = node.getOutEdges().iterator()
    while (i.hasNext()) {
      val directedEdge = i.next() as LineMergeDirectedEdge
      if (directedEdge.getEdge()!!.isMarked()) {
        continue
      }
      edgeStrings!!.add(buildEdgeStringStartingWith(directedEdge))
    }
  }

  private fun buildEdgeStringStartingWith(start: LineMergeDirectedEdge): EdgeString {
    val edgeString = EdgeString(factory!!)
    var current: LineMergeDirectedEdge? = start
    do {
      val curr = current!!
      edgeString.add(curr)
      curr.getEdge()!!.setMarked(true)
      current = curr.getNext()
    } while (current != null && current !== start)
    return edgeString
  }

  /**
   * Gets the [LineString]s created by the merging process.
   *
   * @return the collection of merged LineStrings
   */
  fun getMergedLineStrings(): MutableCollection<LineString>? {
    merge()
    return mergedLineStrings
  }
}
