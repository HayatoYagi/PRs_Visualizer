package io.github.hayatoyagi.prvisualizer.github

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class GitHubUser(
    @SerialName("login")
    val login: String = "",
)

@Serializable
internal data class GitHubRepository(
    @SerialName("full_name")
    val fullName: String = "",
    @SerialName("default_branch")
    val defaultBranch: String = "",
)

@Serializable
internal data class GitHubPullRequest(
    @SerialName("node_id")
    val nodeId: String = "",
    @SerialName("number")
    val number: Int = 0,
    @SerialName("title")
    val title: String = "",
    @SerialName("user")
    val user: GitHubUser? = null,
    @SerialName("draft")
    val draft: Boolean = false,
    @SerialName("html_url")
    val htmlUrl: String = "",
)

@Serializable
internal data class GitHubPullRequestFile(
    @SerialName("filename")
    val filename: String = "",
    @SerialName("additions")
    val additions: Int = 0,
    @SerialName("deletions")
    val deletions: Int = 0,
    @SerialName("status")
    val status: String = "",
)

@Serializable
internal data class GitHubCommitAuthor(
    @SerialName("name")
    val name: String = "",
    @SerialName("date")
    val date: String = "",
)

@Serializable
internal data class GitHubCommitDetails(
    @SerialName("message")
    val message: String = "",
    @SerialName("author")
    val author: GitHubCommitAuthor? = null,
    @SerialName("committer")
    val committer: GitHubCommitAuthor? = null,
)

@Serializable
internal data class GitHubCommit(
    @SerialName("sha")
    val sha: String = "",
    @SerialName("commit")
    val commit: GitHubCommitDetails? = null,
    @SerialName("html_url")
    val htmlUrl: String = "",
)

@Serializable
internal data class GitHubTreeNode(
    @SerialName("type")
    val type: String = "",
    @SerialName("path")
    val path: String = "",
    @SerialName("size")
    val size: Int = 0,
)

@Serializable
internal data class GitHubTree(
    @SerialName("tree")
    val tree: List<GitHubTreeNode> = emptyList(),
)
