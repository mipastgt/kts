/*
 * Copyright (c) 2023 Martin Davis.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.locationtech.jts.operation.relateng

import kotlin.jvm.JvmStatic

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Dimension

/**
 * Converts the node sections at a polygon node where
 * a shell and one or more holes touch, or two or more holes touch.
 *
 * @author Martin Davis
 * @see RelateNode
 */
object PolygonNodeConverter {

    /**
     * Converts a list of sections of valid polygon rings
     * to have "self-touching" structure.
     * There are the same number of output sections as input ones.
     *
     * @param polySections the original sections
     * @return the converted sections
     */
    @JvmStatic
    fun convert(polySections: MutableList<NodeSection>): List<NodeSection> {
        polySections.sortWith(NodeSection.EdgeAngleComparator())

        //TODO: move uniquing up to caller
        val sections = extractUnique(polySections)
        if (sections.size == 1)
            return sections

        //-- find shell section index
        val shellIndex = findShell(sections)
        if (shellIndex < 0) {
            return convertHoles(sections)
        }
        //-- at least one shell is present.  Handle multiple ones if present
        val convertedSections = ArrayList<NodeSection>()
        var nextShellIndex = shellIndex
        do {
            nextShellIndex = convertShellAndHoles(sections, nextShellIndex, convertedSections)
        } while (nextShellIndex != shellIndex)

        return convertedSections
    }

    private fun convertShellAndHoles(
        sections: List<NodeSection>,
        shellIndex: Int,
        convertedSections: MutableList<NodeSection>
    ): Int {
        val shellSection = sections[shellIndex]
        var inVertex = shellSection.getVertex(0)
        var i = next(sections, shellIndex)
        while (!sections[i].isShell()) {
            val holeSection = sections[i]
            // Assert: holeSection.isShell() = false
            val outVertex = holeSection.getVertex(1)
            val ns = createSection(shellSection, inVertex, outVertex)
            convertedSections.add(ns)

            inVertex = holeSection.getVertex(0)
            i = next(sections, i)
        }
        //-- create final section for corner from last hole to shell
        val outVertex = shellSection.getVertex(1)
        val ns = createSection(shellSection, inVertex, outVertex)
        convertedSections.add(ns)
        return i
    }

    private fun convertHoles(sections: List<NodeSection>): List<NodeSection> {
        val convertedSections = ArrayList<NodeSection>()
        val copySection = sections[0]
        for (i in sections.indices) {
            val inext = next(sections, i)
            val inVertex = sections[i].getVertex(0)
            val outVertex = sections[inext].getVertex(1)
            val ns = createSection(copySection, inVertex, outVertex)
            convertedSections.add(ns)
        }
        return convertedSections
    }

    private fun createSection(ns: NodeSection, v0: Coordinate?, v1: Coordinate?): NodeSection {
        return NodeSection(
            ns.isA(),
            Dimension.A, ns.id(), 0, ns.getPolygonal(),
            ns.isNodeAtVertex(),
            v0, ns.nodePt(), v1
        )
    }

    private fun extractUnique(sections: List<NodeSection>): List<NodeSection> {
        val uniqueSections = ArrayList<NodeSection>()
        var lastUnique = sections[0]
        uniqueSections.add(lastUnique)
        for (ns in sections) {
            if (0 != lastUnique.compareTo(ns)) {
                uniqueSections.add(ns)
                lastUnique = ns
            }
        }
        return uniqueSections
    }

    private fun next(ns: List<NodeSection>, i: Int): Int {
        var next = i + 1
        if (next >= ns.size)
            next = 0
        return next
    }

    private fun findShell(polySections: List<NodeSection>): Int {
        for (i in polySections.indices) {
            if (polySections[i].isShell())
                return i
        }
        return -1
    }
}
