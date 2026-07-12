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

package org.locationtech.jts.util

import kotlin.jvm.JvmStatic

import org.locationtech.jts.io.OrdinateFormat

/**
 * Utility methods for working with {@link String}s.
 *
 * @author Martin Davis
 *
 */
class StringUtil {
  companion object {
    /**
     * Mimics the the Java SE {@link String#split(String)} method.
     *
     * @param s the string to split.
     * @param separator the separator to use.
     * @return the array of split strings.
     */
    @JvmStatic
    fun split(s: String, separator: String): Array<String> {
      val separatorlen = separator.length
      val tokenList = ArrayList<Any?>()
      var tmpString = "" + s
      var pos = tmpString.indexOf(separator)
      while (pos >= 0) {
        val token = tmpString.substring(0, pos)
        tokenList.add(token)
        tmpString = tmpString.substring(pos + separatorlen)
        pos = tmpString.indexOf(separator)
      }
      if (tmpString.length > 0)
        tokenList.add(tmpString)
      val res = arrayOfNulls<String>(tokenList.size)
      for (i in res.indices) {
        res[i] = tokenList.get(i) as String
      }
      @Suppress("UNCHECKED_CAST")
      return res as Array<String>
    }

    const val NEWLINE: String = "\n"

    /**
     *  Returns an throwable's stack trace
     */
    @JvmStatic
    fun getStackTrace(t: Throwable): String {
      return t.stackTraceToString()
    }

    @JvmStatic
    fun getStackTrace(t: Throwable, depth: Int): String {
      var stackTrace = ""
      val lines = getStackTrace(t).split("\n")
      for (i in 0 until depth) {
        val line: String? = if (i < lines.size) lines[i] else null
        stackTrace += line + NEWLINE
      }
      return stackTrace
    }

    /**
     * Returns a string representation of the given number,
     * using a format compatible with WKT.
     *
     * @param d a number
     * @return a string
     *
     * @deprecated use {@link OrdinateFormat}
     */
    @JvmStatic
    fun toString(d: Double): String {
      return OrdinateFormat.DEFAULT.format(d)
    }

    @JvmStatic
    fun spaces(n: Int): String {
      return chars(' ', n)
    }

    @JvmStatic
    fun chars(c: Char, n: Int): String {
      val ch = CharArray(n)
      for (i in 0 until n) {
        ch[i] = c
      }
      return ch.concatToString()
    }
  }
}
