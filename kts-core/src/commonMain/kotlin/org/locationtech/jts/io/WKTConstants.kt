/*
 * Copyright (c) 2020 Martin Davis.
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
 * Constants used in the WKT (Well-Known Text) format.
 *
 * @author Martin Davis
 */
object WKTConstants {

  const val GEOMETRYCOLLECTION = "GEOMETRYCOLLECTION"
  const val LINEARRING = "LINEARRING"
  const val LINESTRING = "LINESTRING"
  const val MULTIPOLYGON = "MULTIPOLYGON"
  const val MULTILINESTRING = "MULTILINESTRING"
  const val MULTIPOINT = "MULTIPOINT"
  const val POINT = "POINT"
  const val POLYGON = "POLYGON"

  const val EMPTY = "EMPTY"

  const val M = "M"
  const val Z = "Z"
  const val ZM = "ZM"
}
