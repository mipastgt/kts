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

import org.locationtech.jts.index.chain.MonotoneChain
import org.locationtech.jts.index.chain.MonotoneChainOverlapAction
import org.locationtech.jts.noding.SegmentIntersector
import org.locationtech.jts.noding.SegmentString

internal class EdgeSegmentOverlapAction(private val si: SegmentIntersector) : MonotoneChainOverlapAction() {

    override fun overlap(mc1: MonotoneChain, start1: Int, mc2: MonotoneChain, start2: Int) {
        val ss1 = mc1.getContext() as SegmentString
        val ss2 = mc2.getContext() as SegmentString
        si.processIntersections(ss1, start1, ss2, start2)
    }
}
