/*
 * Copyright (c) 2024 Martin Davis.
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

/**
 * String constants for DE-9IM matrix patterns for topological relationships.
 * These can be used with [RelateNG.evaluate] and [RelateNG.relate].
 *
 * @author Martin Davis
 *
 */
class IntersectionMatrixPattern private constructor() {

    companion object {
        /**
         * A DE-9IM pattern to detect whether two polygonal geometries are adjacent along
         * an edge, but do not overlap.
         */
        const val ADJACENT = "F***1****"

        /**
         * A DE-9IM pattern to detect a geometry which properly contains another
         * geometry (i.e. which lies entirely in the interior of the first geometry).
         */
        const val CONTAINS_PROPERLY = "T**FF*FF*"

        /**
         * A DE-9IM pattern to detect if two geometries intersect in their interiors.
         */
        const val INTERIOR_INTERSECTS = "T********"
    }
}
