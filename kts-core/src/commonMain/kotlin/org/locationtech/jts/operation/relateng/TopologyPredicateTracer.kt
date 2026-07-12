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

import org.locationtech.jts.geom.Envelope
import org.locationtech.jts.geom.Location

/**
 * Traces the evaluation of a [TopologyPredicate].
 *
 * @author mdavis
 *
 */
class TopologyPredicateTracer private constructor() {

    companion object {
        /**
         * Creates a new predicate tracing the evaluation of a given predicate.
         *
         * @param pred the predicate to trace
         * @return the traceable predicate
         */
        @JvmStatic
        fun trace(pred: TopologyPredicate): TopologyPredicate {
            return PredicateTracer(pred)
        }
    }

    private class PredicateTracer(private val pred: TopologyPredicate) : TopologyPredicate {

        override fun name(): String = pred.name()

        override fun requireSelfNoding(): Boolean {
            return pred.requireSelfNoding()
        }

        override fun requireInteraction(): Boolean {
            return pred.requireInteraction()
        }

        override fun requireCovers(isSourceA: Boolean): Boolean {
            return pred.requireCovers(isSourceA)
        }

        override fun requireExteriorCheck(isSourceA: Boolean): Boolean {
            return pred.requireExteriorCheck(isSourceA)
        }

        override fun init(dimA: Int, dimB: Int) {
            pred.init(dimA, dimB)
            checkValue("dimensions")
        }

        override fun init(envA: Envelope, envB: Envelope) {
            pred.init(envA, envB)
            checkValue("envelopes")
        }

        override fun updateDimension(locA: Int, locB: Int, dimension: Int) {
            val desc = "A:" + Location.toLocationSymbol(locA) +
                "/B:" + Location.toLocationSymbol(locB) +
                " -> " + dimension
            var ind = ""
            val isChanged = isDimChanged(locA, locB, dimension)
            if (isChanged) {
                ind = " <<< "
            }
            println(desc + ind)
            pred.updateDimension(locA, locB, dimension)
            if (isChanged) {
                checkValue("IM entry")
            }
        }

        private fun isDimChanged(locA: Int, locB: Int, dimension: Int): Boolean {
            if (pred is IMPredicate) {
                return pred.isDimChanged(locA, locB, dimension)
            }
            return false
        }

        private fun checkValue(source: String) {
            if (pred.isKnown()) {
                println(
                    name() + " = " + pred.value() +
                        " based on " + source
                )
            }
        }

        override fun finish() {
            pred.finish()
        }

        override fun isKnown(): Boolean {
            return pred.isKnown()
        }

        override fun value(): Boolean {
            return pred.value()
        }

        override fun toString(): String {
            return pred.toString()
        }
    }
}
