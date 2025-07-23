package me.nanova.summaryexpressive.model

data class SummaryResult(
    val title: String?,
    val author: String?,
    val summary: String?,
    val errorMessage: String? = null
) {
    val isError: Boolean
        get() = errorMessage != null

    /**
     * TODO
     * Secondary constructor for backward compatibility with view models that still pass `isError`.
     * If `isError` is true, it assumes the `summary` parameter contains the error message.
     */
    constructor(
        title: String?,
        author: String?,
        summaryOrError: String?,
        isError: Boolean
    ) : this(
        title = title,
        author = author,
        summary = if (isError) null else summaryOrError,
        errorMessage = if (isError) summaryOrError else null
    )
}
