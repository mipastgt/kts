/*
 * Copyright (c) 2016 Vivid Solutions, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.locationtech.jts.noding.snapround

import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geom.PrecisionModel
import org.locationtech.jts.geom.util.LinearComponentExtracter
import org.locationtech.jts.noding.NodedSegmentString
import org.locationtech.jts.noding.Noder
import org.locationtech.jts.noding.NodingValidator
import org.locationtech.jts.noding.SegmentString

/**
 * Nodes the linework in a list of [Geometry]s using Snap-Rounding
 * to a given [PrecisionModel].
 *
 *
 * Input coordinates do not need to be rounded to the
 * precision model.
 * All output coordinates are rounded to the precision model.
 *
 *
 * This class does **not** dissolve the output linework,
 * so there may be duplicate linestrings in the output.
 * Subsequent processing (e.g. polygonization) may require
 * the linework to be unique.  Using `UnaryUnion` is one way
 * to do this (although this is an inefficient approach).
 */
class GeometryNoder
/**
 * Creates a new noder which snap-rounds to a grid specified
 * by the given [PrecisionModel].
 *
 * @param pm the precision model for the grid to snap-round to
 */
  (private val pm: PrecisionModel) {
  private lateinit var geomFact: GeometryFactory
  private var isValidityChecked = false

  /**
   * Sets whether noding validity is checked after noding is performed.
   *
   * @param isValidityChecked
   */
  fun setValidate(isValidityChecked: Boolean) {
    this.isValidityChecked = isValidityChecked
  }

  /**
   * Nodes the linework of a set of Geometrys using SnapRounding.
   *
   * @param geoms a Collection of Geometrys of any type
   * @return a List of LineStrings representing the noded linework of the input
   */
  fun node(geoms: Collection<*>): MutableList<LineString> {
    // get geometry factory
    val geom0 = geoms.iterator().next() as Geometry
    geomFact = geom0.getFactory()

    val segStrings = toSegmentStrings(extractLines(geoms))
    val sr: Noder = SnapRoundingNoder(pm)
    sr.computeNodes(segStrings)
    val nodedLines = sr.getNodedSubstrings()!!

    //TODO: improve this to check for full snap-rounded correctness
    if (isValidityChecked) {
      val nv = NodingValidator(nodedLines)
      nv.checkValid()
    }

    return toLineStrings(nodedLines)
  }

  private fun toLineStrings(segStrings: Collection<*>): MutableList<LineString> {
    val lines = ArrayList<LineString>()
    for (obj in segStrings) {
      val ss = obj as SegmentString
      // skip collapsed lines
      if (ss.size() < 2)
        continue
      lines.add(geomFact.createLineString(ss.getCoordinates()))
    }
    return lines
  }

  private fun extractLines(geoms: Collection<*>): MutableList<Any?> {
    val lines = ArrayList<Any?>()
    val lce = LinearComponentExtracter(lines)
    for (obj in geoms) {
      val geom = obj as Geometry
      geom.apply(lce)
    }
    return lines
  }

  private fun toSegmentStrings(lines: Collection<*>): MutableList<NodedSegmentString> {
    val segStrings = ArrayList<NodedSegmentString>()
    for (obj in lines) {
      val line = obj as LineString
      segStrings.add(NodedSegmentString(line.getCoordinates(), null))
    }
    return segStrings
  }
}
