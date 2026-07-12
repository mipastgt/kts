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
package org.locationtech.jts.io

/**
 * Constant values used by the WKB format.
 */
object WKBConstants {
    const val wkbXDR = 0
    const val wkbNDR = 1

    const val wkbPoint = 1
    const val wkbLineString = 2
    const val wkbPolygon = 3
    const val wkbMultiPoint = 4
    const val wkbMultiLineString = 5
    const val wkbMultiPolygon = 6
    const val wkbGeometryCollection = 7
}
