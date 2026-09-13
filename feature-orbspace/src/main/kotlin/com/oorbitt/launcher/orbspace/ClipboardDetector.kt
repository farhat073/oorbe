package com.oorbitt.launcher.orbspace

import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import java.util.ArrayDeque

/**
 * Detected clipboard actions. Each variant carries the parsed payload
 * so the caller can launch the appropriate intent without re-parsing.
 */
sealed class ClipboardAction {
    data class OpenUrl(val url: String) : ClipboardAction()
    data class CallNumber(val number: String) : ClipboardAction()
    data class SendEmail(val email: String) : ClipboardAction()
    data class OpenMap(val address: String) : ClipboardAction()
    data class MathExpression(val expression: String, val result: String) : ClipboardAction()
    data class PlainText(val text: String) : ClipboardAction()
}

/**
 * Analyzes clipboard content and returns the most appropriate [ClipboardAction].
 *
 * On Android 10 (API 29)+ clipboard reads are restricted to the foreground
 * activity / default IME. Call [detect] only from a visible context.
 */
class ClipboardDetector(private val context: Context) {

    // ── Regex patterns ──────────────────────────────────────────────────

    private val urlPattern = Regex(
        """https?://[^\s<>"{}|\\^`\[\]]+""",
        RegexOption.IGNORE_CASE
    )

    private val emailPattern = Regex(
        """[A-Za-z0-9._%+\-]+@[A-Za-z0-9.\-]+\.[A-Za-z]{2,}"""
    )

    /** Matches international numbers with optional country code,
     *  spaces, dashes, dots, parentheses — at least 10 digit characters. */
    private val phonePattern = Regex(
        """(?:\+?\d{1,3}[\s.\-]?)?(?:\(?\d{2,4}\)?[\s.\-]?)?\d[\d\s.\-]{7,}\d"""
    )

    /** Heuristic: line contains a street number AND a road keyword. */
    private val addressKeywords = Regex(
        """\b(?:street|st|avenue|ave|road|rd|boulevard|blvd|drive|dr|lane|ln|court|ct|place|pl|way|highway|hwy|circle|cir|terrace|parkway|pkwy)\b""",
        RegexOption.IGNORE_CASE
    )
    private val streetNumber = Regex("""\b\d{1,6}\b""")

    /** Only digits, whitespace, and arithmetic operators (+, -, *, /, ^, (, ), .) */
    private val mathPattern = Regex(
        """^[\d\s+\-*/^().]+$"""
    )

    // ── Public API ──────────────────────────────────────────────────────

    /**
     * Reads the primary clip and returns a [ClipboardAction], or `null`
     * when the clipboard is empty or inaccessible.
     */
    fun detect(): ClipboardAction? {
        val text = readClipboardText() ?: return null
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return null

        return when {
            urlPattern.containsMatchIn(trimmed) -> {
                val url = urlPattern.find(trimmed)!!.value
                ClipboardAction.OpenUrl(url)
            }

            emailPattern.containsMatchIn(trimmed) -> {
                val email = emailPattern.find(trimmed)!!.value
                ClipboardAction.SendEmail(email)
            }

            looksLikePhoneNumber(trimmed) -> {
                val digits = trimmed.replace(Regex("[^\\d+]"), "")
                ClipboardAction.CallNumber(digits)
            }

            looksLikeAddress(trimmed) -> {
                ClipboardAction.OpenMap(trimmed)
            }

            looksLikeMath(trimmed) -> {
                val result = evaluateMath(trimmed)
                if (result != null) {
                    ClipboardAction.MathExpression(trimmed, result)
                } else {
                    ClipboardAction.PlainText(trimmed)
                }
            }

            else -> ClipboardAction.PlainText(trimmed)
        }
    }

    // ── Helpers ─────────────────────────────────────────────────────────

    private fun readClipboardText(): String? {
        return try {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                ?: return null

            // On Android 10+ the system returns null / empty when not in foreground.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (cm.primaryClipDescription == null) return null
            }

            val clip = cm.primaryClip ?: return null
            if (clip.itemCount == 0) return null
            clip.getItemAt(0).coerceToText(context)?.toString()
        } catch (_: SecurityException) {
            // May be thrown on some OEMs when app is not foreground.
            null
        }
    }

    private fun looksLikePhoneNumber(text: String): Boolean {
        if (!phonePattern.containsMatchIn(text)) return false
        // Require at least 10 digit characters in the entire string
        val digitCount = text.count { it.isDigit() }
        return digitCount >= 10
    }

    private fun looksLikeAddress(text: String): Boolean {
        return addressKeywords.containsMatchIn(text) && streetNumber.containsMatchIn(text)
    }

    private fun looksLikeMath(text: String): Boolean {
        if (!mathPattern.matches(text)) return false
        // Must contain at least one operator to be considered a math expression
        return text.any { it in "+-*/^" }
    }

    // ── Shunting-yard math evaluator ────────────────────────────────────

    /**
     * Evaluates a simple arithmetic expression containing +, -, *, /, ^, (, ).
     * Returns the result as a formatted string, or `null` on parse errors.
     */
    private fun evaluateMath(expr: String): String? {
        return try {
            val tokens = tokenize(expr)
            val rpn = toRPN(tokens)
            val value = evaluateRPN(rpn)
            if (value == value.toLong().toDouble()) {
                value.toLong().toString()
            } else {
                "%.6g".format(value)
            }
        } catch (_: Exception) {
            null
        }
    }

    private sealed class Token {
        data class Num(val value: Double) : Token()
        data class Op(val op: Char) : Token()
        data object LParen : Token()
        data object RParen : Token()
    }

    private fun tokenize(expr: String): List<Token> {
        val tokens = mutableListOf<Token>()
        var i = 0
        val s = expr.replace(" ", "")
        while (i < s.length) {
            val c = s[i]
            when {
                c.isDigit() || c == '.' -> {
                    val start = i
                    while (i < s.length && (s[i].isDigit() || s[i] == '.')) i++
                    tokens.add(Token.Num(s.substring(start, i).toDouble()))
                }
                c == '(' -> { tokens.add(Token.LParen); i++ }
                c == ')' -> { tokens.add(Token.RParen); i++ }
                c in "+-*/^" -> {
                    // Handle unary minus/plus: if first token or after '(' or operator
                    if (c == '-' && (tokens.isEmpty() || tokens.last() is Token.LParen || tokens.last() is Token.Op)) {
                        // Parse the number with the negative sign
                        i++
                        val start = i
                        while (i < s.length && (s[i].isDigit() || s[i] == '.')) i++
                        if (i > start) {
                            tokens.add(Token.Num(-s.substring(start, i).toDouble()))
                        } else {
                            // No number follows the minus — treat as operator
                            tokens.add(Token.Op(c))
                        }
                    } else if (c == '+' && (tokens.isEmpty() || tokens.last() is Token.LParen || tokens.last() is Token.Op)) {
                        i++ // skip unary plus
                    } else {
                        tokens.add(Token.Op(c))
                        i++
                    }
                }
                else -> i++ // skip whitespace / unknown
            }
        }
        return tokens
    }

    private fun precedence(op: Char): Int = when (op) {
        '+', '-' -> 1
        '*', '/' -> 2
        '^' -> 3
        else -> 0
    }

    private fun isRightAssociative(op: Char) = op == '^'

    private fun toRPN(tokens: List<Token>): List<Token> {
        val output = mutableListOf<Token>()
        val ops = ArrayDeque<Token>()
        for (token in tokens) {
            when (token) {
                is Token.Num -> output.add(token)
                is Token.Op -> {
                    while (ops.isNotEmpty()) {
                        val top = ops.peek()
                        if (top is Token.Op &&
                            (precedence(top.op) > precedence(token.op) ||
                                    (precedence(top.op) == precedence(token.op) && !isRightAssociative(token.op)))
                        ) {
                            output.add(ops.pop())
                        } else break
                    }
                    ops.push(token)
                }
                is Token.LParen -> ops.push(token)
                is Token.RParen -> {
                    while (ops.isNotEmpty() && ops.peek() !is Token.LParen) {
                        output.add(ops.pop())
                    }
                    if (ops.isNotEmpty()) ops.pop() // discard '('
                }
            }
        }
        while (ops.isNotEmpty()) output.add(ops.pop())
        return output
    }

    private fun evaluateRPN(tokens: List<Token>): Double {
        val stack = ArrayDeque<Double>()
        for (token in tokens) {
            when (token) {
                is Token.Num -> stack.push(token.value)
                is Token.Op -> {
                    val b = stack.pop()
                    val a = stack.pop()
                    stack.push(
                        when (token.op) {
                            '+' -> a + b
                            '-' -> a - b
                            '*' -> a * b
                            '/' -> if (b != 0.0) a / b else Double.NaN
                            '^' -> Math.pow(a, b)
                            else -> throw IllegalArgumentException("Unknown op ${token.op}")
                        }
                    )
                }
                else -> {} // parens should have been removed
            }
        }
        return stack.pop()
    }
}
