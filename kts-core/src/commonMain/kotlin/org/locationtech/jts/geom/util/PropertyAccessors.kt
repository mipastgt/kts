/*
 * Kotlin property-style accessors for the JTS geom.util API.
 *
 * This is an additive ergonomics layer, not part of the faithful Java port: the ported types keep
 * upstream JTS's explicit Java getters (`getDeterminant()`, `getInverse()`, …). These extension
 * properties restore the idiom Kotlin callers get for free when using the *Java* JTS artifacts —
 * where the Kotlin compiler synthesizes `xform.determinant` from `getDeterminant()` — so downstream
 * Kotlin code reads the same against this port. They are pure delegates to the underlying getters;
 * behaviour is unchanged, and the Java API is untouched.
 *
 * See `org.locationtech.jts.geom.PropertyAccessors` for the accessors over the core geometry types.
 *
 * Usage: import the members, e.g. `import org.locationtech.jts.geom.util.*`.
 *
 * Notes / deliberate omissions:
 *  - `inverse` maps to `getInverse()`, which throws `NoninvertibleTransformationException` for a
 *    singular (determinant == 0) transformation — matching the Java getter it delegates to.
 *  - No accessors for `isDone()` / `isGeometryChanged()`: those are `CoordinateSequenceFilter`
 *    protocol callbacks invoked by the geometry-filter machinery, not part of this type's
 *    user-facing surface.
 */
package org.locationtech.jts.geom.util

// ---- AffineTransformation --------------------------------------------------------------------------

val AffineTransformation.matrixEntries: DoubleArray get() = getMatrixEntries()
val AffineTransformation.determinant: Double get() = getDeterminant()
val AffineTransformation.inverse: AffineTransformation get() = getInverse()
val AffineTransformation.isIdentity: Boolean get() = isIdentity()
