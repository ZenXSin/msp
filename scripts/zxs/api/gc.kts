package zxs.api

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

name = "机器人api提供"

onEnable {
    launch {
        embeddedServer(io.ktor.server.jetty.Jetty, port = 6511) {
            routing {
                get("/enable") {
                    call.respondText("true")
                }
                get("/gc") {
                    val runtime = Runtime.getRuntime()
                    val usedMemoryBefore = runtime.totalMemory() - runtime.freeMemory()
                    System.gc() // 触发垃圾回收
                    val usedMemoryAfter = runtime.totalMemory() - runtime.freeMemory()
                    val msg =
                        "清理完毕，清理前:${usedMemoryBefore / 1024 / 1024} MB，清理后:${usedMemoryAfter / 1024 / 1024} MB"
                    call.respondText(msg)
                }
            }
        }.start(wait = true)
    }
}
