@file:Depends("coreMindustry")

package zxs.server

import coreMindustry.lib.CommandType
import coreMindustry.lib.command
import coreMindustry.lib.type
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter

name = "房间管理"

val processBuilder = ProcessBuilder("java", "-jar", "host.jar", "config", "port", "1568")
processBuilder.directory(File("./config/scripts/PVE"))
val process: Process = processBuilder.start()
val writer = OutputStreamWriter(process.outputStream)

onEnable {
    launch {
        Thread {
            BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                reader.forEachLine { line ->
                    println("STDOUT: $line")
                }
            }
        }.start()
        Thread {
            BufferedReader(InputStreamReader(process.errorStream)).use { reader ->
                reader.forEachLine { line ->
                    println("STDERR: $line")
                }
            }
        }.start()

        process.waitFor()
    }
}
onDisable {
    process.destroyForcibly()
    writer.close()
}

command("send","向pve服务器发送指令") {
    this.type = CommandType.Server
    usage = "[Command]"
    body {
        var msg = ""
        arg.forEach { msg += it + " " }
        writer.write("${msg}\n")
        writer.flush()
    }
}

class Server(command:Array<String>,pathname:String,val servername:String) {
    val processBuilder = ProcessBuilder(*command)
    var process: Process? = null
    var writer: OutputStreamWriter? = null
    init {
        processBuilder.directory(File(pathname))
        process = processBuilder.start()
        writer = OutputStreamWriter(process!!.outputStream)
    }
}