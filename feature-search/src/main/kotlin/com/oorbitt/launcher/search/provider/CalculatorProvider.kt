package com.oorbitt.launcher.search.provider

import com.oorbitt.launcher.model.SearchResult
import com.oorbitt.launcher.model.SearchResultAction
import com.oorbitt.launcher.search.SearchProvider
import java.util.Locale
import java.util.regex.Pattern

class CalculatorProvider : SearchProvider {
    override val priority: Int = 90 // High priority

    override suspend fun search(query: String): List<SearchResult> {
        val trimmed = query.trim()
        if (trimmed.length < 3) return emptyList()

        try {
            val result = evaluateExpression(trimmed)
            if (result != null) {
                return listOf(
                    SearchResult(
                        id = "calc:$trimmed",
                        title = result,
                        subtitle = "Calculator: $trimmed",
                        action = SearchResultAction.ShowAnswer(result),
                        relevanceScore = 0.95f, // high score so it floats to the top
                        providerName = "Calculator"
                    )
                )
            }
        } catch (_: Exception) { }

        return emptyList()
    }

    private fun evaluateExpression(expr: String): String? {
        var clean = expr.lowercase(Locale.ROOT)
            .replace(" ", "")
            .replace("x", "*")
            .replace("÷", "/")
        
        // Handle "X% of Y" -> (X/100) * Y
        val percentOfPattern = Pattern.compile("(\\d+(\\.\\d+)?)\\%of(\\d+(\\.\\d+)?)")
        val matcher = percentOfPattern.matcher(clean)
        if (matcher.find()) {
            val x = matcher.group(1).toDoubleOrNull() ?: return null
            val y = matcher.group(3).toDoubleOrNull() ?: return null
            val ans = (x / 100.0) * y
            return formatResult(ans)
        }

        // Check if expression contains only math characters
        if (!clean.matches(Regex("[0-9+\\-*/().%]+"))) {
            return null
        }

        // Basic shunting-yard or double evaluation
        val evaluator = SimpleMathEvaluator(clean)
        val ans = evaluator.eval()
        return formatResult(ans)
    }

    private fun formatResult(value: Double): String {
        return if (value % 1.0 == 0.0) {
            value.toLong().toString()
        } else {
            String.format(Locale.ROOT, "%.4f", value).trimEnd('0').trimEnd('.')
        }
    }

    private class SimpleMathEvaluator(val str: String) {
        var pos = -1
        var ch = 0

        fun nextChar() {
            ch = if (++pos < str.length) str[pos].code else -1
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
            if (pos < str.length) throw RuntimeException("Unexpected: " + ch.toChar())
            return x
        }

        // Grammar:
        // expression = term | expression `+` term | expression `-` term
        // term = factor | term `*` factor | term `/` factor
        // factor = `+` factor | `-` factor | `(` expression `)` | number
        //        | factor `%`
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
                else return x
            }
        }

        fun parseFactor(): Double {
            if (eat('+'.code)) return parseFactor() // unary plus
            if (eat('-'.code)) return -parseFactor() // unary minus

            var x: Double
            val startPos = this.pos
            if (eat('('.code)) { // parentheses
                x = parseExpression()
                eat(')'.code)
            } else if (ch >= '0'.code && ch <= '9'.code || ch == '.'.code) { // numbers
                while (ch >= '0'.code && ch <= '9'.code || ch == '.'.code) nextChar()
                x = str.substring(startPos, this.pos).toDouble()
            } else {
                throw RuntimeException("Unexpected: " + ch.toChar())
            }

            // Handle suffix percent (e.g. 50% = 0.5)
            if (eat('%'.code)) {
                x /= 100.0
            }

            return x
        }

        fun eval(): Double {
            return parse()
        }
    }
}
