package io.github.hayatoyagi.prvisualizer.github

class GitHubAuthExpiredException(message: String) : RuntimeException(message)
class GitHubApiException(val statusCode: Int, message: String) : RuntimeException(message)
