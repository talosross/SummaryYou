package me.nanova.summaryexpressive.model

sealed class SummaryException(message: String) : Exception(message) {
    data object NoInternetException : SummaryException("Exception: no internet")
    data object InvalidLinkException : SummaryException("Exception: invalid link")
    data object NoTranscriptException : SummaryException("Exception: no transcript")
    data object NoContentException : SummaryException("Exception: no content")
    data object TooShortException : SummaryException("Exception: too short")
    data object PaywallException : SummaryException("Exception: paywall detected")
    data object TooLongException : SummaryException("Exception: too long")
    data object IncorrectKeyException : SummaryException("Exception: incorrect key")
    data object RateLimitException : SummaryException("Exception: rate limit")
    data object NoKeyException : SummaryException("Exception: no key")
    class UnknownException(message: String) : SummaryException(message)
}