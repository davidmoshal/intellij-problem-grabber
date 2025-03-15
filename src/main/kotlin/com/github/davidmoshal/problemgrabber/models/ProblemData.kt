package com.github.davidmoshal.problemgrabber.models

/**
 * Data class representing a detected problem in the code
 */
data class ProblemData(
    val message: String,
    val description: String,
    val type: String,
    val filePath: String,
    val line: Int,
    val column: Int,
    val severity: String,
    val fix: String?,
    val code: String,
    val surroundingCode: String
)
