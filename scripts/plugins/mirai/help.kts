@file:Depends("mirai/miraiCommandApi")
@file:Depends("plugins/user/userSql")

package mirai

import MiraiCommandApi.*
import MiraiCommandApi.T.newMiraiCommand

newMiraiCommand("help","帮助") {
    var msg = "📋 可用指令列表\n| 指令 | 介绍 | 范围 |\n"
    val maxName = T.miraiCommands.maxOf { it.listen.length }
    val maxDesc = T.miraiCommands.maxOf { if (it.description=="") 4 else it.description.length }
    for (c in T.miraiCommands) {
        val name  = c.listen.padEnd(maxName)
        val desc  = (if (c.description=="") "none" else c.description).padEnd(maxDesc)
        val type  = c.listenType.name
        msg += "| $name | $desc | $type |\n"
    }
    subject.sendMessage(msg)
}