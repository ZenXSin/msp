@file:Depends("zxs/libs/mail")

package zxs.user

import coreMindustry.UtilTextInput
import coreMindustry.lib.game
import coreMindustry.lib.listen
import coreMindustry.lib.registerActionFilter
import mindustry.Vars
import mindustry.game.EventType
import mindustry.gen.Call
import mindustry.gen.Player
import mindustry.net.Administration
import zxs.libs.Mail
import org.jetbrains.exposed.sql.transactions.transaction
import wayzer.lib.dao.PlayerData
import wayzer.lib.dao.PlayerProfile
import wayzer.user.ext.ProfileBind

val input = contextScript<UtilTextInput>()
val bind = contextScript<ProfileBind>()
val mail = contextScript<Mail>()

name = "白名单"

listen<EventType.PlayerJoin> {
    if (PlayerData[it.player.uuid()].profile?.qq === null) {
        launch(Dispatchers.game) {
            val qq = input.textInput(it.player, "请输入QQ号进行绑定（绑定码会发往您QQ号对应的邮箱）")!!.toLong()
            val token = bind.generate(qq)
            mail.sm("${qq}@qq.com", "您的绑定码", token.toString())
            val tok = input.textInput(it.player, "请输入获取到的绑定码")?.toInt()
            if (tok == token) {
                PlayerData[it.player!!.uuid()].apply {
                    transaction {
                        bind(player!!, PlayerProfile.findOrCreate(qq).apply {
                            this.onJoin(player!!)
                        })
                    }
                }
            } else {
                it.player.kick("[red]请输入正确的绑定码",0)
            }
            if (PlayerData[it.player.uuid()].profile?.qq === null) {
                it.player.kick("[red]请先绑定", 0)
            }
        }
    }
}

fun Player.isOk(): Boolean {
    val ok = PlayerData.findById(uuid())?.profile != null
    if (!ok) Call.infoMessage(
        con, "[red]本服务器已开启白名单验证\n请绑定账号后再进行操作"
    )
    return ok
}

registerActionFilter {
    it.player.isOk()
}
onEnable {
    val f = Administration.ChatFilter { p, m ->
        if (!p.isOk()) {
            p.sendMessage("[red]本服务器已开启白名单验证\n请绑定账号后再进行操作")
            null
        } else m
    }
    Vars.netServer.admins.chatFilters.add(f)
}
/*val code: MutableMap<Int, String> = mutableMapOf()
val waitbind: MutableMap<Long, String> = mutableMapOf()

listen<EventType.PlayerJoin> {
    launch {
        waitbind.forEach { (t, u) ->
            if (it.player.uuid() == u) {
                PlayerData[u].apply {
                    transaction {
                        bind(player!!, PlayerProfile.findOrCreate(t).apply {
                            this.onJoin(player!!)
                        })
                    }
                }
                waitbind.remove(t)
            }
        }
        delay(1000)
        if (PlayerData[it.player!!.uuid()].profile?.qq == null)
            it.player.kick(
                "[red]您未绑定QQ，请在QQ群中发送“/绑定 ${getcode(it.player.uuid())}”绑定账号后再进入服务器(机器人不会回复)",
                0
            )
    }
}

fun getcode(uuid: String): Int {
    val token = Random.nextInt(100000, 999999)
    code += Pair(token, uuid)
    return token
}

globalEventChannel().subscribeGroupMessages {
    contains("/绑定", true) {
        val token = it.replace("/绑定 ", "").toInt()
        code.forEach { (t, u) ->
            if (t == token) {
                waitbind += Pair(sender.id, u)
                code.remove(t)
                return@forEach
            }
        }
    }
}*/
