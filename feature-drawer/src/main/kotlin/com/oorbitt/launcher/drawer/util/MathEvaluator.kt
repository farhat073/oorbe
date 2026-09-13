package com.oorbitt.launcher.drawer.util

import kotlin.math.pow

object MathEvaluator {
    fun evaluate(str: String): Double? {
        val clean = str.replace(" ", "").replace("x", "*").replace("X", "*")
        if (clean.isBlank()) return null
        
        // Quick check if this is likely a math expression.
        // It must contain at least some digits/numbers and at least one math operator or parenthesis,
        // and shouldn't contain letters unless they form pi or e constants.
        val hasDigits = clean.any { it.isDigit() }
        val hasOperators = clean.any { it in "+-*/%^()" }
        val hasLetters = clean.any { it.isLetter() && it.lowercaseChar() !in listOf('p', 'i', 'e') }
        if (!hasDigits || !hasOperators || hasLetters) {
            return null
        }
        
        return try {
            object : Any() {
                var pos = -1
                var ch = 0

                fun nextChar() {
                    ch = if (++pos < clean.length) clean[pos].code else -1
                }

                fun eat(charToEat: Int): Boolean {
                    while (ch == ' '.code) nextChar()
                    if (ch == charToEat) {
                        nextChar()
                        return true
                    }
                    return false
                }

                fun parse(): Double {
                    nextChar()
                    val x = parseExpression()
                    if (pos < clean.length) throw RuntimeException("Unexpected: " + ch.toChar())
                    return x
                }

                fun parseExpression(): Double {
                    var x = parseTerm()
                    while (true) {
                        if (eat('+'.code)) x += parseTerm() // addition
                        else if (eat('-'.code)) x -= parseTerm() // subtraction
                        else return x
                    }
                }

                fun parseTerm(): Double {
                    var x = parseFactor()
                    while (true) {
                        if (eat('*'.code)) x *= parseFactor() // multiplication
                        else if (eat('/'.code)) x /= parseFactor() // division
                        else if (eat('%'.code)) x %= parseFactor() // modulo
                        else return x
                    }
                }

                fun parseFactor(): Double {
                    if (eat('+'.code)) return parseFactor() // unary plus
                    if (eat('-'.code)) return -parseFactor() // unary minus

                    var x: Double
                    val startPos = pos
                    if (eat('('.code)) { // parentheses
                        x = parseExpression()
                        eat(')'.code)
                    } else if ((ch >= '0'.code && ch <= '9'.code) || ch == '.'.code) { // numbers
                        while ((ch >= '0'.code && ch <= '9'.code) || ch == '.'.code) nextChar()
                        x = clean.substring(startPos, pos).toDouble()
                    } else if (eat('p'.code) && eat('i'.code)) {
                        x = Math.PI
                    } else if (eat('e'.code)) {
                        x = Math.E
                    } else {
                        throw RuntimeException("Unexpected: " + ch.toChar())
                    }

                    if (eat('^'.code)) x = x.pow(parseFactor()) // exponentiation

                    return x
                }
            }.parse()
        } catch (e: Exception) {
            null
        }
    }
}
