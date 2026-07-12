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
 * A source of characters for [WktStreamTokenizer]. Common-Kotlin abstraction over the input the
 * WKT reader tokenizes: [StringWktCharStream] (the common `read(String)` path) and, on the JVM, a
 * `java.io.Reader`-backed stream (the streaming `read(Reader)` path).
 *
 * This is a public seam so streaming readers in other modules (e.g. `jts-io-files`) can parse a
 * sequence of geometries from a shared character source via [WKTReader.read].
 */
interface WktCharStream {
    /** Returns the next character code, or -1 at end of input. */
    fun read(): Int
}

/** A [WktCharStream] over an in-memory [String]. */
internal class StringWktCharStream(private val text: String) : WktCharStream {
    private var pos = 0
    override fun read(): Int = if (pos < text.length) text[pos++].code else -1
}

/**
 * A minimal tokenizer reproducing the exact behaviour the WKT reader configured on
 * `java.io.StreamTokenizer`: `resetSyntax` with word characters `[A-Za-z0-9.+-]` (and code points
 * >= 160), whitespace `0..' '`, and `#` as a line-comment character. Numbers are **not** parsed by
 * the tokenizer — they are returned as words and parsed by the reader — matching the original
 * configuration. `'('`, `')'` and `','` are ordinary single-character tokens (`ttype` == the char
 * code). EOL is not significant (treated as whitespace) but still advances the line counter.
 */
internal class WktStreamTokenizer(private val source: WktCharStream) {

    /** After [nextToken], the type of the token: [TT_WORD], [TT_EOF], or an ordinary char code. */
    var ttype: Int = TT_NOTHING
        private set

    /** The string value, set when [ttype] is [TT_WORD]. */
    var sval: String? = null
        private set

    private var lineNumber = 1
    private var peekc = NEED_CHAR
    private var pushedBack = false

    /** The current line number (1-based), for error messages. */
    fun lineno(): Int = lineNumber

    /**
     * Causes the next [nextToken] call to return the current token again (without re-reading).
     */
    fun pushBack() {
        if (ttype != TT_NOTHING) {
            pushedBack = true
        }
    }

    /**
     * Reads the next token, sets [ttype] (and [sval] for words), and returns [ttype].
     */
    fun nextToken(): Int {
        if (pushedBack) {
            pushedBack = false
            return ttype
        }
        sval = null

        var c = peekc
        peekc = NEED_CHAR
        if (c == NEED_CHAR) {
            c = source.read()
        }

        // skip whitespace and comments, counting lines (EOL is not significant)
        while (true) {
            if (c < 0) {
                ttype = TT_EOF
                return ttype
            }
            if (c == '\r'.code) {
                lineNumber++
                c = source.read()
                if (c == '\n'.code) {
                    c = source.read()
                }
                continue
            }
            if (c == '\n'.code) {
                lineNumber++
                c = source.read()
                continue
            }
            if (c <= ' '.code) {
                c = source.read()
                continue
            }
            if (c == '#'.code) {
                // comment: skip to end of line; the terminating EOL/EOF is handled next iteration
                do {
                    c = source.read()
                } while (c != '\n'.code && c != '\r'.code && c >= 0)
                continue
            }
            break
        }

        if (isWordChar(c)) {
            val sb = StringBuilder()
            do {
                sb.append(c.toChar())
                c = source.read()
            } while (c >= 0 && isWordChar(c))
            peekc = c // push back the terminating non-word char (or EOF)
            sval = sb.toString()
            ttype = TT_WORD
            return ttype
        }

        // ordinary single-character token
        ttype = c
        return ttype
    }

    private fun isWordChar(c: Int): Boolean {
        return c in 'a'.code..'z'.code ||
            c in 'A'.code..'Z'.code ||
            c in '0'.code..'9'.code ||
            c == '-'.code || c == '+'.code || c == '.'.code ||
            c in 160..255 ||
            c >= 256
    }

    companion object {
        const val TT_EOF = -1
        const val TT_EOL = -4 // not produced (EOL not significant); kept for error-message parity
        const val TT_NUMBER = -2 // not produced (numbers are words); kept for error-message parity
        const val TT_WORD = -3
        private const val TT_NOTHING = -5
        private const val NEED_CHAR = Int.MAX_VALUE
    }
}
