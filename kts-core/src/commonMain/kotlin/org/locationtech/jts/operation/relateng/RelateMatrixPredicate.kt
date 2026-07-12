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

import org.locationtech.jts.geom.IntersectionMatrix

/**
 * Evaluates the full relate [IntersectionMatrix].
 * @author mdavis
 *
 */
internal class RelateMatrixPredicate : IMPredicate() {

    override fun name(): String {
        return "relateMatrix"
    }

    override fun requireInteraction(): Boolean {
        //-- ensure entire matrix is computed
        return false
    }

    override fun isDetermined(): Boolean {
        //-- ensure entire matrix is computed
        return false
    }

    override fun valueIM(): Boolean {
        //-- indicates full matrix is being evaluated
        return false
    }

    /**
     * Gets the current state of the IM matrix (which may only be partially complete).
     *
     * @return the IM matrix
     */
    fun getIM(): IntersectionMatrix {
        return intMatrix
    }
}
