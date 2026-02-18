@file:Depends("zxs/libs/mail")

package zxs.server

import arc.Core
import coreLibrary.lib.util.loop
import coreMindustry.lib.game
import mindustry.Vars
import mindustry.gen.Groups
import zxs.libs.Mail
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import net.mamoe.mirai.Bot
import java.sql.Connection
import mirai.Send

name = "服务器监控"

val mirai = contextScript<Send>()
val mail = contextScript<Mail>()
var sql: Connection? = null

onEnable {
    transaction {
        SchemaUtils.create(dyqq)
    }
    launch(Dispatchers.game) {
        loop(Dispatchers.game) {
            delay(60000)
            val tps = Core.graphics.framesPerSecond.coerceAtMost(255)
            if (tps < 10) {
                var mes = ""
                Groups.player.forEach {
                    mes += it.name + "   " + it.uuid() + "\n"
                }
                get().map { q ->
                    launch {
                        mail.sm(
                            "${q[dyqq.qq]}@qq.com",
                            "服务器tps异常，当前tps：${tps}",
                            "当前地图：${Vars.state.map.name()}在线玩家：\n $mes"
                        )
                    }
                            mirai.sendFriend("服务器tps异常，当前tps：${tps}\n当前地图：${Vars.state.map.name()}\n在线玩家：\n" + mes,q[dyqq.qq].toLong())
                }
            }
        }
    }
}

object dyqq : Table("dyqq") {
    val qq = varchar("qq", length = 1024)
}

fun add(qq: Long) {
    transaction {
        dyqq.insert {
            it[dyqq.qq] = qq.toString()
        }
    }
    mirai.sendFriend("感谢您的订阅,服务器相关的状态消息将在此呈现",qq)
    launch {
        mail.sm(
            "${qq}@qq.com",
            "感谢您的订阅",
            "服务器相关的状态消息将在此呈现"
        )
    }
}

fun remove(qq: Long) {
    transaction {
        dyqq.deleteWhere { dyqq.qq.eq(qq.toString()) }
    }
    mirai.sendFriend("随时欢迎您的到来",qq)
    launch {
        mail.sm(
            "${qq}@qq.com",
            "随时欢迎您的到来",
            "随时欢迎您的到来"
        )
    }
}

fun get(): Query {
    return transaction { dyqq.selectAll() }
}

fun getd(): dyqq {
    return transaction { dyqq }
}