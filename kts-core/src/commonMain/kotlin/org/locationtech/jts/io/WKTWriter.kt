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

import kotlin.jvm.JvmStatic

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.CoordinateSequence
import org.locationtech.jts.geom.CoordinateSequenceFilter
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryCollection
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geom.LinearRing
import org.locationtech.jts.geom.MultiLineString
import org.locationtech.jts.geom.MultiPoint
import org.locationtech.jts.geom.MultiPolygon
import org.locationtech.jts.geom.Point
import org.locationtech.jts.geom.Polygon
import org.locationtech.jts.geom.PrecisionModel
import org.locationtech.jts.util.Assert

/**
 * Writes the Well-Known Text representation of a [Geometry].
 * The Well-Known Text format is defined in the
 * OGC [*Simple Features Specification for SQL*](http://www.opengis.org/techno/specs.htm).
 * See `WKTReader` (in the jts-io-wkt module) for a formal specification of the format syntax.
 *
 * The `WKTWriter` outputs coordinates rounded to the precision
 * model. Only the maximum number of decimal places
 * necessary to represent the ordinates to the required precision will be
 * output.
 *
 * The SFS WKT spec does not define a special tag for [LinearRing]s.
 * Under the spec, rings are output as `LINESTRING`s.
 * In order to allow precisely specifying constructed geometries,
 * JTS also supports a non-standard `LINEARRING` tag which is used
 * to output LinearRings.
 *
 * Note: the `Writer` overloads of [write]/[writeFormatted] are the JVM (Phase-1) streaming
 * surface; the multiplatform-common API uses the `String`-returning forms (backed internally
 * by a [StringBuilder]).
 *
 */
class WKTWriter(private val outputDimension: Int) {

  /**
   * A filter implementation to test if a coordinate sequence actually has
   * meaningful values for an ordinate bit-pattern
   */
  private class CheckOrdinatesFilter(private val checkOrdinateFlags: MutableSet<Ordinate>) :
    CoordinateSequenceFilter {

    private val outputOrdinates: MutableSet<Ordinate> = mutableSetOf(Ordinate.X, Ordinate.Y)

    override fun filter(seq: CoordinateSequence, i: Int) {
      if (checkOrdinateFlags.contains(Ordinate.Z) && !outputOrdinates.contains(Ordinate.Z)) {
        if (!seq.getZ(i).isNaN()) outputOrdinates.add(Ordinate.Z)
      }
      if (checkOrdinateFlags.contains(Ordinate.M) && !outputOrdinates.contains(Ordinate.M)) {
        if (!seq.getM(i).isNaN()) outputOrdinates.add(Ordinate.M)
      }
    }

    override fun isGeometryChanged(): Boolean = false

    override fun isDone(): Boolean = outputOrdinates == checkOrdinateFlags

    /**
     * Gets the evaluated ordinate bit-pattern
     *
     * @return A bit-pattern of ordinates with valid values masked by `checkOrdinateFlags`.
     */
    fun getOutputOrdinates(): MutableSet<Ordinate> = outputOrdinates
  }

  var outputOrdinates: MutableSet<Ordinate> = mutableSetOf(Ordinate.X, Ordinate.Y)
    /**
     * Sets the [Ordinate] that are to be written. Possible members are `X`, `Y`, `Z`, `M`.
     * Values of `X` and `Y` are always assumed and not particularly checked for.
     */
    set(value) {
      field.remove(Ordinate.Z)
      field.remove(Ordinate.M)

      if (outputDimension == 3) {
        if (value.contains(Ordinate.Z)) field.add(Ordinate.Z)
        else if (value.contains(Ordinate.M)) field.add(Ordinate.M)
      }
      if (outputDimension == 4) {
        if (value.contains(Ordinate.Z)) field.add(Ordinate.Z)
        if (value.contains(Ordinate.M)) field.add(Ordinate.M)
      }
    }

  private var precisionModel: PrecisionModel? = null
  private var ordinateFormat: OrdinateFormat? = null
  private var formatted = false
  private var coordsPerLine = -1
  private var indentTabStr: String = ""

  init {
    setTab(INDENT)

    if (outputDimension < 2 || outputDimension > 4)
      throw IllegalArgumentException("Invalid output dimension (must be 2 to 4)")

    if (outputDimension > 2) outputOrdinates.add(Ordinate.Z)
    if (outputDimension > 3) outputOrdinates.add(Ordinate.M)
  }

  /**
   * Creates a new WKTWriter with default settings
   */
  constructor() : this(OUTPUT_DIMENSION)

  /**
   * Sets whether the output will be formatted.
   *
   * @param isFormatted true if the output is to be formatted
   */
  fun setFormatted(isFormatted: Boolean) {
    this.formatted = isFormatted
  }

  /**
   * Sets the maximum number of coordinates per line
   * written in formatted output.
   * If the provided coordinate number is <= 0,
   * coordinates will be written all on one line.
   *
   * @param coordsPerLine the number of coordinates per line to output.
   */
  fun setMaxCoordinatesPerLine(coordsPerLine: Int) {
    this.coordsPerLine = coordsPerLine
  }

  /**
   * Sets the tab size to use for indenting.
   *
   * @param size the number of spaces to use as the tab string
   * @throws IllegalArgumentException if the size is non-positive
   */
  fun setTab(size: Int) {
    if (size <= 0)
      throw IllegalArgumentException("Tab count must be positive")
    this.indentTabStr = stringOfChar(' ', size)
  }

  /**
   * Sets a [PrecisionModel] that should be used on the ordinates written.
   *
   * If none/`null` is assigned, the precision model of the [Geometry.getFactory] is used.
   *
   * Note: The precision model is applied to all ordinate values, not just x and y.
   *
   * @param precisionModel the precision model
   */
  fun setPrecisionModel(precisionModel: PrecisionModel) {
    this.precisionModel = precisionModel
    this.ordinateFormat = OrdinateFormat.create(precisionModel.getMaximumSignificantDigits())
  }

  /**
   *  Converts a `Geometry` to its Well-known Text representation.
   *
   * @param  geometry  a `Geometry` to process
   * @return           a <Geometry Tagged Text> string (see the OpenGIS Simple
   *      Features Specification)
   */
  fun write(geometry: Geometry): String {
    return writeToString(geometry, false)
  }

  /**
   *  Converts a `Geometry` to its Well-known Text representation, appending it to `out`.
   *
   * @param  geometry  a `Geometry` to process
   * @param  out       the destination to append to (e.g. a `java.io.Writer` or `StringBuilder`)
   */
  fun write(geometry: Geometry, out: Appendable) {
    // write the geometry
    out.append(writeToString(geometry, formatted))
  }

  /**
   *  Same as `write`, but with newlines and spaces to make the
   *  well-known text more readable.
   *
   * @param  geometry  a `Geometry` to process
   * @return           a <Geometry Tagged Text> string (see the OpenGIS Simple
   *      Features Specification), with newlines and spaces
   */
  fun writeFormatted(geometry: Geometry): String {
    return writeToString(geometry, true)
  }

  /**
   *  Same as `write`, but with newlines and spaces to make the
   *  well-known text more readable.
   *
   * @param  geometry  a `Geometry` to process
   * @param  out       the destination to append to (e.g. a `java.io.Writer` or `StringBuilder`)
   */
  fun writeFormatted(geometry: Geometry, out: Appendable) {
    out.append(writeToString(geometry, true))
  }

  /**
   *  Converts a `Geometry` to its Well-known Text representation
   *  and returns it as a string.
   *
   * @param  geometry       the `Geometry` to process
   * @param  useFormatting  flag indicating that the output should be formatted
   */
  private fun writeToString(geometry: Geometry, useFormatting: Boolean): String {
    val sb = StringBuilder()
    val formatter = getFormatter(geometry)
    // append the WKT
    appendGeometryTaggedText(geometry, useFormatting, sb, formatter)
    return sb.toString()
  }

  private fun getFormatter(geometry: Geometry): OrdinateFormat {
    // if present use the cached formatter
    ordinateFormat?.let { return it }

    // no precision model was specified, so use the geometry's
    val pm = geometry.getPrecisionModel()
    return createFormatter(pm)
  }

  /**
   *  Converts a `Geometry` to <Geometry Tagged Text> format,
   *  then appends it to the buffer.
   */
  private fun appendGeometryTaggedText(
    geometry: Geometry, useFormatting: Boolean, sb: StringBuilder, formatter: OrdinateFormat
  ) {
    // evaluate the ordinates actually present in the geometry
    val cof = CheckOrdinatesFilter(this.outputOrdinates)
    geometry.apply(cof)

    // Append the WKT
    appendGeometryTaggedText(geometry, cof.getOutputOrdinates(), useFormatting, 0, sb, formatter)
  }

  /**
   *  Converts a `Geometry` to <Geometry Tagged Text> format,
   *  then appends it to the buffer.
   */
  private fun appendGeometryTaggedText(
    geometry: Geometry, outputOrdinates: MutableSet<Ordinate>, useFormatting: Boolean,
    level: Int, sb: StringBuilder, formatter: OrdinateFormat
  ) {
    indent(useFormatting, level, sb)

    when (geometry) {
      is Point -> appendPointTaggedText(geometry, outputOrdinates, useFormatting, level, sb, formatter)
      is LinearRing -> appendLinearRingTaggedText(geometry, outputOrdinates, useFormatting, level, sb, formatter)
      is LineString -> appendLineStringTaggedText(geometry, outputOrdinates, useFormatting, level, sb, formatter)
      is Polygon -> appendPolygonTaggedText(geometry, outputOrdinates, useFormatting, level, sb, formatter)
      is MultiPoint -> appendMultiPointTaggedText(geometry, outputOrdinates, useFormatting, level, sb, formatter)
      is MultiLineString -> appendMultiLineStringTaggedText(geometry, outputOrdinates, useFormatting, level, sb, formatter)
      is MultiPolygon -> appendMultiPolygonTaggedText(geometry, outputOrdinates, useFormatting, level, sb, formatter)
      is GeometryCollection -> appendGeometryCollectionTaggedText(geometry, outputOrdinates, useFormatting, level, sb, formatter)
      else -> Assert.shouldNeverReachHere("Unsupported Geometry implementation:" + geometry::class.simpleName)
    }
  }

  /**
   *  Converts a `Coordinate` to <Point Tagged Text> format,
   *  then appends it to the buffer.
   */
  private fun appendPointTaggedText(
    point: Point, outputOrdinates: MutableSet<Ordinate>, useFormatting: Boolean,
    level: Int, sb: StringBuilder, formatter: OrdinateFormat
  ) {
    sb.append(WKTConstants.POINT)
    sb.append(" ")
    appendOrdinateText(outputOrdinates, sb)
    appendSequenceText(point.getCoordinateSequence(), outputOrdinates, useFormatting, level, false, sb, formatter)
  }

  /**
   *  Converts a `LineString` to <LineString Tagged Text>
   *  format, then appends it to the buffer.
   */
  private fun appendLineStringTaggedText(
    lineString: LineString, outputOrdinates: MutableSet<Ordinate>, useFormatting: Boolean,
    level: Int, sb: StringBuilder, formatter: OrdinateFormat
  ) {
    sb.append(WKTConstants.LINESTRING)
    sb.append(" ")
    appendOrdinateText(outputOrdinates, sb)
    appendSequenceText(lineString.getCoordinateSequence(), outputOrdinates, useFormatting, level, false, sb, formatter)
  }

  /**
   *  Converts a `LinearRing` to <LinearRing Tagged Text>
   *  format, then appends it to the buffer.
   */
  private fun appendLinearRingTaggedText(
    linearRing: LinearRing, outputOrdinates: MutableSet<Ordinate>, useFormatting: Boolean,
    level: Int, sb: StringBuilder, formatter: OrdinateFormat
  ) {
    sb.append(WKTConstants.LINEARRING)
    sb.append(" ")
    appendOrdinateText(outputOrdinates, sb)
    appendSequenceText(linearRing.getCoordinateSequence(), outputOrdinates, useFormatting, level, false, sb, formatter)
  }

  /**
   *  Converts a `Polygon` to <Polygon Tagged Text> format,
   *  then appends it to the buffer.
   */
  private fun appendPolygonTaggedText(
    polygon: Polygon, outputOrdinates: MutableSet<Ordinate>, useFormatting: Boolean,
    level: Int, sb: StringBuilder, formatter: OrdinateFormat
  ) {
    sb.append(WKTConstants.POLYGON)
    sb.append(" ")
    appendOrdinateText(outputOrdinates, sb)
    appendPolygonText(polygon, outputOrdinates, useFormatting, level, false, sb, formatter)
  }

  /**
   *  Converts a `MultiPoint` to <MultiPoint Tagged Text>
   *  format, then appends it to the buffer.
   */
  private fun appendMultiPointTaggedText(
    multipoint: MultiPoint, outputOrdinates: MutableSet<Ordinate>, useFormatting: Boolean,
    level: Int, sb: StringBuilder, formatter: OrdinateFormat
  ) {
    sb.append(WKTConstants.MULTIPOINT)
    sb.append(" ")
    appendOrdinateText(outputOrdinates, sb)
    appendMultiPointText(multipoint, outputOrdinates, useFormatting, level, sb, formatter)
  }

  /**
   *  Converts a `MultiLineString` to <MultiLineString Tagged
   *  Text> format, then appends it to the buffer.
   */
  private fun appendMultiLineStringTaggedText(
    multiLineString: MultiLineString, outputOrdinates: MutableSet<Ordinate>, useFormatting: Boolean,
    level: Int, sb: StringBuilder, formatter: OrdinateFormat
  ) {
    sb.append(WKTConstants.MULTILINESTRING)
    sb.append(" ")
    appendOrdinateText(outputOrdinates, sb)
    appendMultiLineStringText(multiLineString, outputOrdinates, useFormatting, level, sb, formatter)
  }

  /**
   *  Converts a `MultiPolygon` to <MultiPolygon Tagged Text>
   *  format, then appends it to the buffer.
   */
  private fun appendMultiPolygonTaggedText(
    multiPolygon: MultiPolygon, outputOrdinates: MutableSet<Ordinate>, useFormatting: Boolean,
    level: Int, sb: StringBuilder, formatter: OrdinateFormat
  ) {
    sb.append(WKTConstants.MULTIPOLYGON)
    sb.append(" ")
    appendOrdinateText(outputOrdinates, sb)
    appendMultiPolygonText(multiPolygon, outputOrdinates, useFormatting, level, sb, formatter)
  }

  /**
   *  Converts a `GeometryCollection` to <GeometryCollection
   *  Tagged Text> format, then appends it to the buffer.
   */
  private fun appendGeometryCollectionTaggedText(
    geometryCollection: GeometryCollection, outputOrdinates: MutableSet<Ordinate>, useFormatting: Boolean,
    level: Int, sb: StringBuilder, formatter: OrdinateFormat
  ) {
    sb.append(WKTConstants.GEOMETRYCOLLECTION)
    sb.append(" ")
    appendOrdinateText(outputOrdinates, sb)
    appendGeometryCollectionText(geometryCollection, outputOrdinates, useFormatting, level, sb, formatter)
  }

  /**
   * Appends the i'th coordinate from the sequence to the buffer.
   *
   * If the `seq` has coordinates that are `Double.NaN`, these are not written, even though
   * [outputDimension] suggests this.
   */
  private fun appendCoordinate(
    seq: CoordinateSequence, outputOrdinates: MutableSet<Ordinate>, i: Int,
    sb: StringBuilder, formatter: OrdinateFormat
  ) {
    sb.append(writeNumber(seq.getX(i), formatter) + " " + writeNumber(seq.getY(i), formatter))

    if (outputOrdinates.contains(Ordinate.Z)) {
      sb.append(" ")
      sb.append(writeNumber(seq.getZ(i), formatter))
    }

    if (outputOrdinates.contains(Ordinate.M)) {
      sb.append(" ")
      sb.append(writeNumber(seq.getM(i), formatter))
    }
  }

  /**
   * Appends additional ordinate information. This function may append 'Z', 'M' or 'ZM'
   * depending on the ordinates present in `outputOrdinates`.
   */
  private fun appendOrdinateText(outputOrdinates: MutableSet<Ordinate>, sb: StringBuilder) {
    if (outputOrdinates.contains(Ordinate.Z))
      sb.append(WKTConstants.Z)
    if (outputOrdinates.contains(Ordinate.M))
      sb.append(WKTConstants.M)
  }

  /**
   *  Appends all members of a `CoordinateSequence` to the buffer. Each `Coordinate` is separated from
   *  another using a comma, the ordinates of a `Coordinate` are separated by a space.
   */
  private fun appendSequenceText(
    seq: CoordinateSequence, outputOrdinates: MutableSet<Ordinate>, useFormatting: Boolean,
    level: Int, indentFirst: Boolean, sb: StringBuilder, formatter: OrdinateFormat
  ) {
    if (seq.size() == 0) {
      sb.append(WKTConstants.EMPTY)
    } else {
      if (indentFirst) indent(useFormatting, level, sb)
      sb.append("(")
      for (i in 0 until seq.size()) {
        if (i > 0) {
          sb.append(", ")
          if (coordsPerLine > 0 && i % coordsPerLine == 0) {
            indent(useFormatting, level + 1, sb)
          }
        }
        appendCoordinate(seq, outputOrdinates, i, sb, formatter)
      }
      sb.append(")")
    }
  }

  /**
   *  Converts a `Polygon` to <Polygon Text> format, then
   *  appends it to the buffer.
   */
  private fun appendPolygonText(
    polygon: Polygon, outputOrdinates: MutableSet<Ordinate>, useFormatting: Boolean,
    level: Int, indentFirst: Boolean, sb: StringBuilder, formatter: OrdinateFormat
  ) {
    if (polygon.isEmpty()) {
      sb.append(WKTConstants.EMPTY)
    } else {
      if (indentFirst) indent(useFormatting, level, sb)
      sb.append("(")
      appendSequenceText(polygon.getExteriorRing().getCoordinateSequence(), outputOrdinates,
        useFormatting, level, false, sb, formatter)
      for (i in 0 until polygon.getNumInteriorRing()) {
        sb.append(", ")
        appendSequenceText(polygon.getInteriorRingN(i).getCoordinateSequence(), outputOrdinates,
          useFormatting, level + 1, true, sb, formatter)
      }
      sb.append(")")
    }
  }

  /**
   *  Converts a `MultiPoint` to <MultiPoint Text> format, then
   *  appends it to the buffer.
   */
  private fun appendMultiPointText(
    multiPoint: MultiPoint, outputOrdinates: MutableSet<Ordinate>, useFormatting: Boolean,
    level: Int, sb: StringBuilder, formatter: OrdinateFormat
  ) {
    if (multiPoint.getNumGeometries() == 0) {
      sb.append(WKTConstants.EMPTY)
    } else {
      sb.append("(")
      for (i in 0 until multiPoint.getNumGeometries()) {
        if (i > 0) {
          sb.append(", ")
          indentCoords(useFormatting, i, level + 1, sb)
        }
        appendSequenceText((multiPoint.getGeometryN(i) as Point).getCoordinateSequence(),
          outputOrdinates, useFormatting, level, false, sb, formatter)
      }
      sb.append(")")
    }
  }

  /**
   *  Converts a `MultiLineString` to <MultiLineString Text>
   *  format, then appends it to the buffer.
   */
  private fun appendMultiLineStringText(
    multiLineString: MultiLineString, outputOrdinates: MutableSet<Ordinate>,
    useFormatting: Boolean, level: Int, sb: StringBuilder, formatter: OrdinateFormat
  ) {
    if (multiLineString.getNumGeometries() == 0) {
      sb.append(WKTConstants.EMPTY)
    } else {
      var level2 = level
      var doIndent = false
      sb.append("(")
      for (i in 0 until multiLineString.getNumGeometries()) {
        if (i > 0) {
          sb.append(", ")
          level2 = level + 1
          doIndent = true
        }
        appendSequenceText((multiLineString.getGeometryN(i) as LineString).getCoordinateSequence(),
          outputOrdinates, useFormatting, level2, doIndent, sb, formatter)
      }
      sb.append(")")
    }
  }

  /**
   *  Converts a `MultiPolygon` to <MultiPolygon Text> format,
   *  then appends it to the buffer.
   */
  private fun appendMultiPolygonText(
    multiPolygon: MultiPolygon, outputOrdinates: MutableSet<Ordinate>, useFormatting: Boolean,
    level: Int, sb: StringBuilder, formatter: OrdinateFormat
  ) {
    if (multiPolygon.getNumGeometries() == 0) {
      sb.append(WKTConstants.EMPTY)
    } else {
      var level2 = level
      var doIndent = false
      sb.append("(")
      for (i in 0 until multiPolygon.getNumGeometries()) {
        if (i > 0) {
          sb.append(", ")
          level2 = level + 1
          doIndent = true
        }
        appendPolygonText(multiPolygon.getGeometryN(i) as Polygon, outputOrdinates,
          useFormatting, level2, doIndent, sb, formatter)
      }
      sb.append(")")
    }
  }

  /**
   *  Converts a `GeometryCollection` to <GeometryCollectionText>
   *  format, then appends it to the buffer.
   */
  private fun appendGeometryCollectionText(
    geometryCollection: GeometryCollection, outputOrdinates: MutableSet<Ordinate>, useFormatting: Boolean,
    level: Int, sb: StringBuilder, formatter: OrdinateFormat
  ) {
    if (geometryCollection.getNumGeometries() == 0) {
      sb.append(WKTConstants.EMPTY)
    } else {
      var level2 = level
      sb.append("(")
      for (i in 0 until geometryCollection.getNumGeometries()) {
        if (i > 0) {
          sb.append(", ")
          level2 = level + 1
        }
        appendGeometryTaggedText(geometryCollection.getGeometryN(i), outputOrdinates,
          useFormatting, level2, sb, formatter)
      }
      sb.append(")")
    }
  }

  private fun indentCoords(useFormatting: Boolean, coordIndex: Int, level: Int, sb: StringBuilder) {
    if (coordsPerLine <= 0 || coordIndex % coordsPerLine != 0)
      return
    indent(useFormatting, level, sb)
  }

  private fun indent(useFormatting: Boolean, level: Int, sb: StringBuilder) {
    if (!useFormatting || level <= 0)
      return
    sb.append("\n")
    for (i in 0 until level) {
      sb.append(indentTabStr)
    }
  }

  companion object {
    /**
     * Generates the WKT for a `POINT`
     * specified by a [Coordinate].
     *
     * @param p0 the point coordinate
     *
     * @return the WKT
     */
    @JvmStatic
    fun toPoint(p0: Coordinate): String {
      return WKTConstants.POINT + " ( " + format(p0) + " )"
    }

    /**
     * Generates the WKT for a `LINESTRING`
     * specified by a [CoordinateSequence].
     *
     * @param seq the sequence to write
     *
     * @return the WKT string
     */
    @JvmStatic
    fun toLineString(seq: CoordinateSequence): String {
      val buf = StringBuilder()
      buf.append(WKTConstants.LINESTRING)
      buf.append(" ")
      if (seq.size() == 0)
        buf.append(WKTConstants.EMPTY)
      else {
        buf.append("(")
        for (i in 0 until seq.size()) {
          if (i > 0)
            buf.append(", ")
          buf.append(format(seq.getX(i), seq.getY(i)))
        }
        buf.append(")")
      }
      return buf.toString()
    }

    /**
     * Generates the WKT for a `LINESTRING`
     * specified by a [CoordinateSequence].
     *
     * @param coord the sequence to write
     *
     * @return the WKT string
     */
    @JvmStatic
    fun toLineString(coord: Array<Coordinate>): String {
      val buf = StringBuilder()
      buf.append(WKTConstants.LINESTRING)
      buf.append(" ")
      if (coord.isEmpty())
        buf.append(WKTConstants.EMPTY)
      else {
        buf.append("(")
        for (i in coord.indices) {
          if (i > 0)
            buf.append(", ")
          buf.append(format(coord[i]))
        }
        buf.append(")")
      }
      return buf.toString()
    }

    /**
     * Generates the WKT for a `LINESTRING`
     * specified by two [Coordinate]s.
     *
     * @param p0 the first coordinate
     * @param p1 the second coordinate
     *
     * @return the WKT
     */
    @JvmStatic
    fun toLineString(p0: Coordinate, p1: Coordinate): String {
      return WKTConstants.LINESTRING + " ( " + format(p0) + ", " + format(p1) + " )"
    }

    @JvmStatic
    fun format(p: Coordinate): String {
      return format(p.x, p.y)
    }

    private fun format(x: Double, y: Double): String {
      return OrdinateFormat.DEFAULT.format(x) + " " + OrdinateFormat.DEFAULT.format(y)
    }

    private const val INDENT = 2
    private const val OUTPUT_DIMENSION = 2

    /**
     *  Creates the `OrdinateFormat` used to write `double`s
     *  with a sufficient number of decimal places.
     *
     * @param  precisionModel  the `PrecisionModel` used to determine
     *      the number of decimal places to write.
     * @return                 an `OrdinateFormat` that writes `double`s without scientific notation.
     */
    private fun createFormatter(precisionModel: PrecisionModel): OrdinateFormat {
      return OrdinateFormat.create(precisionModel.getMaximumSignificantDigits())
    }

    /**
     *  Returns a `String` of repeated characters.
     *
     * @param  ch     the character to repeat
     * @param  count  the number of times to repeat the character
     * @return        a `String` of characters
     */
    private fun stringOfChar(ch: Char, count: Int): String {
      val buf = StringBuilder(count)
      for (i in 0 until count) {
        buf.append(ch)
      }
      return buf.toString()
    }

    /**
     *  Converts a `double` to a `String`, not in scientific notation.
     */
    private fun writeNumber(d: Double, formatter: OrdinateFormat): String {
      return formatter.format(d)
    }
  }
}
