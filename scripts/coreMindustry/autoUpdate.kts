package coreMindustry

import arc.util.Log
import arc.util.Time
import kotlinx.coroutines.*
import java.io.*
import java.net.URL
import java.nio.file.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import kotlin.io.path.*

name = "服务器管理: GitHub 自动更新器"

// ==================== 配置区域 ====================

class C(
    /** GitHub 仓库地址，格式: owner/repo */
    var repo: String = "ZenXSin/msp",

    /** 分支名称 */
    var branch: String = "master",

    /** GitHub Token (私有仓库需要) */
    var token: String = "",

    /** 本地脚本根目录 */
    val localPath: Path = Paths.get("config/"),

    /** 临时下载目录 */
    val tempPath: Path = Paths.get("config/scripts_update_temp"),

    /** 备份目录 */
    val backupPath: Path = Paths.get("config/scripts_backup"),

    /** 需要排除的文件/文件夹 (正则) */
    val excludePatterns: List<Regex> = listOf(
        "\\.git.*".toRegex(),
        "build".toRegex(),
        "\\.gradle".toRegex(),
        "\\.idea".toRegex(),
        "\\.kts\\.compiled\\..*".toRegex(),
        "buildSrc".toRegex(),
        "gradle".toRegex(),
        "logs".toRegex(),
        "maps".toRegex(),
        "mods".toRegex(),
        "saves".toRegex(),
        "settings_backups".toRegex(),
        "scripts_update_temp".toRegex(),
        "scripts_backup".toRegex(),
        "cache".toRegex(),
        "date".toRegex()
    )
)

val config = C()

// ==================== 数据类定义 ====================

data class GitHubTree(
    val sha: String,
    val url: String,
    val tree: List<GitHubTreeItem>
)

data class GitHubTreeItem(
    val path: String,
    val mode: String,
    val type: String,
    val sha: String,
    val size: Long? = null,
    val url: String? = null
)

data class GitHubCommit(
    val sha: String,
    val commit: CommitDetail,
    val html_url: String
)

data class CommitDetail(
    val message: String,
    val author: CommitAuthor
)

data class CommitAuthor(
    val name: String,
    val date: String
)

data class FileChange(
    val path: String,
    val type: ChangeType,
    val remoteSize: Long?,
    val localSize: Long?,
    val remoteSha: String?
) {
    enum class ChangeType { ADDED, MODIFIED, DELETED, UNCHANGED }
}

data class TreeNode(
    val name: String,
    val path: String,
    val type: NodeType,
    val changeType: FileChange.ChangeType? = null,
    val size: Long? = null,
    val children: MutableList<TreeNode> = mutableListOf()
) {
    enum class NodeType { FILE, DIRECTORY }
}

// 存储待确认的更新
var pendingChanges: List<FileChange>? = null
var pendingCommit: GitHubCommit? = null

// ==================== 手动JSON解析器 ====================

object JsonParser {

    fun parseTree(json: String): GitHubTree {
        val map = parseObject(json)
        return GitHubTree(
            sha = getString(map, "sha"),
            url = getString(map, "url"),
            tree = (map["tree"] as? List<*>)?.mapNotNull {
                it as? Map<*, *>
            }?.map { parseTreeItem(it) } ?: emptyList()
        )
    }

    fun parseTreeItem(map: Map<*, *>): GitHubTreeItem {
        return GitHubTreeItem(
            path = getString(map, "path"),
            mode = getString(map, "mode"),
            type = getString(map, "type"),
            sha = getString(map, "sha"),
            size = (map["size"] as? Number)?.toLong(),
            url = map["url"] as? String
        )
    }

    fun parseCommit(json: String): GitHubCommit {
        val map = parseObject(json)
        val commitMap = map["commit"] as? Map<*, *> ?: emptyMap<Any?, Any?>()
        val authorMap = commitMap["author"] as? Map<*, *> ?: emptyMap<Any?, Any?>()

        return GitHubCommit(
            sha = getString(map, "sha"),
            commit = CommitDetail(
                message = getString(commitMap, "message"),
                author = CommitAuthor(
                    name = getString(authorMap, "name"),
                    date = getString(authorMap, "date")
                )
            ),
            html_url = getString(map, "html_url")
        )
    }

    private fun getString(map: Map<*, *>, key: String): String {
        return (map[key] as? String) ?: ""
    }

    private fun parseObject(json: String): Map<String, Any?> {
        val result = mutableMapOf<String, Any?>()
        var i = skipWhitespace(json, 0)

        if (i >= json.length || json[i] != '{') {
            return result
        }
        i++

        while (i < json.length) {
            i = skipWhitespace(json, i)
            if (i >= json.length) break

            if (json[i] == '}') {
                i++
                break
            }

            if (json[i] != '"') {
                while (i < json.length && json[i] != ',' && json[i] != '}') i++
                if (i < json.length && json[i] == ',') i++
                continue
            }

            val keyResult = parseString(json, i)
            val key = keyResult.first
            i = keyResult.second

            i = skipWhitespace(json, i)
            if (i < json.length && json[i] == ':') i++
            i = skipWhitespace(json, i)

            val valueResult = parseValue(json, i)
            result[key] = valueResult.first
            i = valueResult.second

            i = skipWhitespace(json, i)
            if (i < json.length && json[i] == ',') {
                i++
            }
        }

        return result
    }

    private fun parseValue(json: String, start: Int): Pair<Any?, Int> {
        var i = skipWhitespace(json, start)
        if (i >= json.length) return null to i

        return when (json[i]) {
            '"' -> {
                val result = parseString(json, i)
                result.first to result.second
            }
            '{' -> {
                var depth = 1
                var j = i + 1
                while (j < json.length && depth > 0) {
                    when (json[j]) {
                        '{' -> depth++
                        '}' -> depth--
                        '"' -> {
                            j++
                            while (j < json.length && json[j] != '"') {
                                if (json[j] == '\\' && j + 1 < json.length) j++
                                j++
                            }
                        }
                    }
                    j++
                }
                val objStr = json.substring(i, j)
                parseObject(objStr) to j
            }
            '[' -> {
                val list = mutableListOf<Any?>()
                i++
                while (i < json.length) {
                    i = skipWhitespace(json, i)
                    if (i < json.length && json[i] == ']') {
                        i++
                        break
                    }

                    val valueResult = parseValue(json, i)
                    list.add(valueResult.first)
                    i = valueResult.second

                    i = skipWhitespace(json, i)
                    if (i < json.length && json[i] == ',') {
                        i++
                    } else if (i < json.length && json[i] == ']') {
                        i++
                        break
                    }
                }
                list to i
            }
            't' -> {
                if (i + 4 <= json.length && json.substring(i, i + 4) == "true") {
                    true to (i + 4)
                } else {
                    null to i
                }
            }
            'f' -> {
                if (i + 5 <= json.length && json.substring(i, i + 5) == "false") {
                    false to (i + 5)
                } else {
                    null to i
                }
            }
            'n' -> {
                if (i + 4 <= json.length && json.substring(i, i + 4) == "null") {
                    null to (i + 4)
                } else {
                    null to i
                }
            }
            else -> {
                var j = i
                while (j < json.length && (json[j].isDigit() || json[j] == '-' || json[j] == '.' ||
                            json[j] == 'e' || json[j] == 'E' || json[j] == '+')) {
                    j++
                }
                val numStr = json.substring(i, j)
                val value = when {
                    numStr.contains('.') || numStr.lowercase().contains('e') -> numStr.toDoubleOrNull()
                    else -> numStr.toLongOrNull()
                }
                value to j
            }
        }
    }

    private fun parseString(json: String, start: Int): Pair<String, Int> {
        val sb = StringBuilder()
        var i = start + 1

        while (i < json.length) {
            when (json[i]) {
                '"' -> return sb.toString() to (i + 1)
                '\\' -> {
                    if (i + 1 < json.length) {
                        i++
                        when (json[i]) {
                            '"' -> sb.append('"')
                            '\\' -> sb.append('\\')
                            '/' -> sb.append('/')
                            'b' -> sb.append('\b')
                            'f' -> sb.append('\u000C')
                            'n' -> sb.append('\n')
                            'r' -> sb.append('\r')
                            't' -> sb.append('\t')
                            'u' -> {
                                if (i + 4 < json.length) {
                                    val hex = json.substring(i + 1, i + 5)
                                    sb.append(hex.toInt(16).toChar())
                                    i += 4
                                }
                            }
                            else -> sb.append(json[i])
                        }
                    }
                }
                else -> sb.append(json[i])
            }
            i++
        }

        return sb.toString() to i
    }

    private fun skipWhitespace(json: String, start: Int): Int {
        var i = start
        while (i < json.length && json[i].isWhitespace()) i++
        return i
    }
}

// ==================== 核心功能 ====================

suspend fun fetchRemoteTree(): Result<GitHubTree> = withContext(Dispatchers.IO) {
    runCatching {
        val url = "https://api.github.com/repos/${config.repo}/git/trees/${config.branch}?recursive=1"
        val connection = URL(url).openConnection() as java.net.HttpURLConnection
        connection.requestMethod = "GET"
        connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
        connection.setRequestProperty("User-Agent", "Mindustry-Server-Updater")

        if (config.token.isNotEmpty()) {
            connection.setRequestProperty("Authorization", "token ${config.token}")
        }

        connection.connectTimeout = 10000
        connection.readTimeout = 30000

        val response = connection.inputStream.bufferedReader().use { it.readText() }
        JsonParser.parseTree(response)
    }
}

suspend fun fetchLatestCommit(): Result<GitHubCommit> = withContext(Dispatchers.IO) {
    runCatching {
        val url = "https://api.github.com/repos/${config.repo}/commits/${config.branch}"
        val connection = URL(url).openConnection() as java.net.HttpURLConnection
        connection.requestMethod = "GET"
        connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
        connection.setRequestProperty("User-Agent", "Mindustry-Server-Updater")

        if (config.token.isNotEmpty()) {
            connection.setRequestProperty("Authorization", "token ${config.token}")
        }

        val response = connection.inputStream.bufferedReader().use { it.readText() }
        JsonParser.parseCommit(response)
    }
}

fun scanLocalFiles(): Map<String, Long> {
    val files = mutableMapOf<String, Long>()
    if (!config.localPath.exists()) return files

    Files.walk(config.localPath).use { stream ->
        stream.filter { it.isRegularFile() }
            .filter { path ->
                val relative = config.localPath.relativize(path).toString().replace("\\", "/")

                if (relative.startsWith("scripts_update_temp/") ||
                    relative.startsWith("scripts_backup/") ||
                    relative.contains("/scripts_update_temp/") ||
                    relative.contains("/scripts_backup/")) return@filter false

                config.excludePatterns.none { pattern ->
                    pattern.matches(relative) ||
                            relative.split("/").any { segment -> pattern.matches(segment) }
                }
            }
            .forEach { path ->
                val relative = config.localPath.relativize(path).toString().replace("\\", "/")
                files[relative] = path.fileSize()
            }
    }
    return files
}

fun compareFiles(remoteTree: GitHubTree, localFiles: Map<String, Long>): List<FileChange> {
    val remoteFiles = remoteTree.tree.filter { it.type == "blob" }
    val changes = mutableListOf<FileChange>()
    val processed = mutableSetOf<String>()

    remoteFiles.forEach { remote ->
        val shouldExclude = config.excludePatterns.any { pattern ->
            pattern.matches(remote.path) ||
                    remote.path.split("/").any { segment -> pattern.matches(segment) }
        }
        if (shouldExclude) return@forEach

        val localSize = localFiles[remote.path]
        val type = when {
            localSize == null -> FileChange.ChangeType.ADDED
            localSize != remote.size -> FileChange.ChangeType.MODIFIED
            else -> FileChange.ChangeType.UNCHANGED
        }

        if (type != FileChange.ChangeType.UNCHANGED) {
            changes.add(FileChange(
                path = remote.path,
                type = type,
                remoteSize = remote.size,
                localSize = localSize,
                remoteSha = remote.sha
            ))
        }
        processed.add(remote.path)
    }

    localFiles.keys.forEach { localPath ->
        val shouldExclude = config.excludePatterns.any { pattern ->
            pattern.matches(localPath) ||
                    localPath.split("/").any { segment -> pattern.matches(segment) }
        }
        if (localPath !in processed && !shouldExclude) {
            changes.add(FileChange(
                path = localPath,
                type = FileChange.ChangeType.DELETED,
                remoteSize = null,
                localSize = localFiles[localPath],
                remoteSha = null
            ))
        }
    }

    return changes.sortedBy { it.path }
}

fun buildTreeStructure(changes: List<FileChange>): TreeNode {
    val root = TreeNode("root", "", TreeNode.NodeType.DIRECTORY)

    val allPaths = mutableSetOf<String>()
    changes.forEach { change ->
        var path = change.path
        allPaths.add(path)
        while (path.contains("/")) {
            path = path.substringBeforeLast("/")
            allPaths.add(path)
        }
    }

    val nodeMap = mutableMapOf<String, TreeNode>()

    allPaths.filter { !it.contains(".") || changes.none { c -> c.path == it && c.type != FileChange.ChangeType.DELETED } }
        .forEach { path ->
            val name = if (path.contains("/")) path.substringAfterLast("/") else path
            val node = TreeNode(name, path, TreeNode.NodeType.DIRECTORY)
            nodeMap[path] = node
        }

    changes.forEach { change ->
        if (change.type != FileChange.ChangeType.DELETED || change.path.contains(".")) {
            val name = change.path.substringAfterLast("/")
            val node = TreeNode(
                name = name,
                path = change.path,
                type = TreeNode.NodeType.FILE,
                changeType = change.type,
                size = change.remoteSize ?: change.localSize
            )
            nodeMap[change.path] = node
        }
    }

    nodeMap.values.forEach { node ->
        if (node.path.contains("/")) {
            val parentPath = node.path.substringBeforeLast("/")
            val parent = nodeMap[parentPath] ?: root
            parent.children.add(node)
        } else if (node != root) {
            root.children.add(node)
        }
    }

    fun sortChildren(node: TreeNode) {
        node.children.sortWith(compareBy({ it.type == TreeNode.NodeType.FILE }, { it.name }))
        node.children.forEach { sortChildren(it) }
    }
    sortChildren(root)

    return root
}

fun renderTree(node: TreeNode, prefix: String = "", isLast: Boolean = true): String {
    val sb = StringBuilder()

    if (node.name != "root") {
        val connector = if (isLast) "└── " else "├── "
        val color = when (node.changeType) {
            FileChange.ChangeType.ADDED -> "[green]"
            FileChange.ChangeType.MODIFIED -> "[yellow]"
            FileChange.ChangeType.DELETED -> "[red]"
            else -> "[white]"
        }
        val icon = when (node.type) {
            TreeNode.NodeType.DIRECTORY -> "📁"
            TreeNode.NodeType.FILE -> when (node.changeType) {
                FileChange.ChangeType.ADDED -> "[green]+"
                FileChange.ChangeType.MODIFIED -> "[yellow]~"
                FileChange.ChangeType.DELETED -> "[red]-"
                else -> "  "
            }
        }

        val sizeStr = if (node.type == TreeNode.NodeType.FILE && node.size != null) {
            " ${node.size.formatBytes()}"
        } else ""

        sb.appendLine("$prefix$connector$color$icon ${node.name}[]$sizeStr")
    }

    val newPrefix = prefix + if (isLast) "    " else "│   "

    node.children.forEachIndexed { index, child ->
        val childIsLast = index == node.children.size - 1
        sb.append(renderTree(child, newPrefix, childIsLast))
    }

    return sb.toString()
}

fun calculateStats(node: TreeNode): Triple<Int, Int, Int> {
    var added = 0
    var modified = 0
    var deleted = 0

    fun traverse(n: TreeNode) {
        when (n.changeType) {
            FileChange.ChangeType.ADDED -> added++
            FileChange.ChangeType.MODIFIED -> modified++
            FileChange.ChangeType.DELETED -> deleted++
            else -> {}
        }
        n.children.forEach { traverse(it) }
    }

    traverse(node)
    return Triple(added, modified, deleted)
}

suspend fun downloadRepository(targetPath: Path): Result<Boolean> = withContext(Dispatchers.IO) {
    runCatching {
        if (targetPath.exists()) {
            targetPath.toFile().deleteRecursively()
        }
        targetPath.createDirectories()

        val zipUrl = "https://github.com/${config.repo}/archive/refs/heads/${config.branch}.zip"
        val zipFile = targetPath.resolve("repo.zip")

        URL(zipUrl).openStream().use { input ->
            Files.copy(input, zipFile, StandardCopyOption.REPLACE_EXISTING)
        }

        ZipInputStream(Files.newInputStream(zipFile)).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            val rootFolder = entry?.name?.split("/")?.firstOrNull() ?: "${config.repo.replace("/", "-")}-${config.branch}"

            while (entry != null) {
                if (!entry.isDirectory) {
                    val relativePath = entry.name.removePrefix("$rootFolder/")
                    if (relativePath != entry.name) {
                        val outputPath = targetPath.resolve(relativePath)
                        outputPath.parent?.createDirectories()
                        Files.copy(zis, outputPath, StandardCopyOption.REPLACE_EXISTING)
                    }
                }
                entry = zis.nextEntry
            }
        }

        zipFile.deleteIfExists()
        true
    }
}

suspend fun createBackup(): Result<Path> = withContext(Dispatchers.IO) {
    runCatching {
        val timestamp = Time.millis()
        val backupDir = config.backupPath.resolve("backup_$timestamp")
        backupDir.createDirectories()

        if (config.localPath.exists()) {
            Files.walk(config.localPath).use { stream ->
                stream.filter { it.isRegularFile() }
                    .forEach { source ->
                        val relative = config.localPath.relativize(source)
                        val target = backupDir.resolve(relative)
                        target.parent?.createDirectories()
                        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING)
                    }
            }
        }

        backupDir
    }
}

suspend fun applyUpdate(changes: List<FileChange>): Result<String> = withContext(Dispatchers.IO) {
    runCatching {
        val backupPath = createBackup().getOrThrow()
        downloadRepository(config.tempPath).getOrThrow()

        var added = 0
        var modified = 0
        var deleted = 0

        changes.forEach { change ->
            when (change.type) {
                FileChange.ChangeType.ADDED -> {
                    val source = config.tempPath.resolve(change.path)
                    val target = config.localPath.resolve(change.path)
                    target.parent?.createDirectories()
                    Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING)
                    added++
                }
                FileChange.ChangeType.MODIFIED -> {
                    val source = config.tempPath.resolve(change.path)
                    val target = config.localPath.resolve(change.path)
                    Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING)
                    modified++
                }
                FileChange.ChangeType.DELETED -> {
                    val target = config.localPath.resolve(change.path)
                    target.deleteIfExists()
                    deleted++
                }
                else -> {}
            }
        }

        config.tempPath.toFile().deleteRecursively()

        "更新完成！新增: $added, 修改: $modified, 删除: $deleted | 备份位置: $backupPath"
    }
}

fun Long.formatBytes(): String {
    return when {
        this >= 1024 * 1024 -> String.format("%.2f MB", this / (1024.0 * 1024.0))
        this >= 1024 -> String.format("%.2f KB", this / 1024.0)
        else -> "$this B"
    }
}

// ==================== 控制台命令 ====================

command("github-update", "检查 GitHub 仓库更新") {
    permission = "admin.github.update"

    body {
        launch(Dispatchers.IO) {
            Log.info("[GitHubUpdater] 正在检查更新...")

            val commitResult = fetchLatestCommit()
            if (commitResult.isFailure) {
                Log.err("[GitHubUpdater] 获取提交信息失败: ${commitResult.exceptionOrNull()?.message}")
                return@launch
            }

            val commit = commitResult.getOrThrow()
            Log.info("[GitHubUpdater] 远程最新提交: ${commit.sha.take(7)} by ${commit.commit.author.name}")
            Log.info("[GitHubUpdater] 提交信息: ${commit.commit.message}")

            val treeResult = fetchRemoteTree()
            if (treeResult.isFailure) {
                Log.err("[GitHubUpdater] 获取文件树失败: ${treeResult.exceptionOrNull()?.message}")
                return@launch
            }

            val localFiles = scanLocalFiles()
            val changes = compareFiles(treeResult.getOrThrow(), localFiles)

            if (changes.isEmpty()) {
                Log.info("[GitHubUpdater] 本地文件已是最新，无需更新")
                pendingChanges = null
                pendingCommit = null
                return@launch
            }

            // 存储待确认的变更
            pendingChanges = changes
            pendingCommit = commit

            val tree = buildTreeStructure(changes)
            val (added, modified, deleted) = calculateStats(tree)

            Log.info("")
            Log.info("══════════ 项目结构对比 ══════════")
            Log.info("新增: $added | 修改: $modified | 删除: $deleted")
            Log.info("")

            val treeStr = renderTree(tree)
            treeStr.lines().forEach { line ->
                if (line.isNotBlank()) Log.info(line)
            }

            Log.info("")
            Log.info("══════════════════════════════════")
            Log.info("输入 github-update-confirm 确认更新")
            Log.info("输入 github-update-cancel 取消更新")
        }
    }
}

command("github-update-confirm", "确认执行 GitHub 更新") {
    
    permission = "admin.github.update"

    body {
        val changes = pendingChanges
        if (changes == null) {
            Log.warn("[GitHubUpdater] 没有待处理的更新，请先执行 github-update")
            return@body
        }

        launch(Dispatchers.IO) {
            Log.info("[GitHubUpdater] 正在执行更新...")

            val result = applyUpdate(changes)

            result.fold(
                onSuccess = { msg ->
                    Log.info("[GitHubUpdater] $msg")
                    pendingChanges = null
                    pendingCommit = null
                },
                onFailure = { e ->
                    Log.err("[GitHubUpdater] 更新失败: ${e.message}")
                }
            )
        }
    }
}

command("github-update-cancel", "取消 GitHub 更新") {
    
    permission = "admin.github.update"

    body {
        if (pendingChanges != null) {
            pendingChanges = null
            pendingCommit = null
            Log.info("[GitHubUpdater] 已取消更新")
        } else {
            Log.warn("[GitHubUpdater] 没有正在进行的更新")
        }
    }
}

command("github-update-config", "查看更新器配置") {
    
    permission = "admin.github.update.config"

    body {
        Log.info("=== GitHub 更新器配置 ===")
        Log.info("仓库: ${config.repo}")
        Log.info("分支: ${config.branch}")
        Log.info("Token: ${if (config.token.isEmpty()) "未设置" else "已设置"}")
        Log.info("本地路径: ${config.localPath}")
        Log.info("备份路径: ${config.backupPath}")
        Log.info("排除规则: ${config.excludePatterns.size} 项")
    }
}

// ==================== 生命周期 ====================

onEnable {
    Log.info("[GitHubUpdater] 更新器已启用")
    Log.info("[GitHubUpdater] 配置仓库: ${config.repo}:${config.branch}")
    config.localPath.createDirectories()
    config.backupPath.createDirectories()
}

onDisable {
    if (config.tempPath.exists()) {
        config.tempPath.toFile().deleteRecursively()
    }
    pendingChanges = null
    pendingCommit = null
    Log.info("[GitHubUpdater] 更新器已禁用")
}