@file:Depends("zxs/libs/getplayer")

package zxs.game

import coreLibrary.lib.PermissionApi
import coreLibrary.lib.with
import coreMindustry.lib.CommandType
import coreMindustry.lib.command
import coreMindustry.lib.player
import mindustry.Vars.netServer
import mindustry.gen.Player
import mindustry.net.Administration
import org.jetbrains.exposed.sql.transactions.transaction
import wayzer.VoteService
import wayzer.VoteService.addSubVote
import wayzer.lib.dao.PlayerData
import zxs.libs.Getplayer

val user: MutableList<String> = mutableListOf()
val getplayer = contextScript<Getplayer>()

name = "投票禁言"

addSubVote("投票禁言", "quiet", "禁言") {
    try {
        val p = getplayer.getPlayer(player!!)!!
        if (user.contains(p.uuid())) returnReply("[red]此玩家已被禁言".with())
        VoteService.start(player!!, "禁言 ${p.name}".with()) {
            add(p.uuid())
        }
    } catch (e: Exception) {
        returnReply("[red]执行禁言出错，请将报错截图并询问管理\n\n{e}".with("e" to e.toString()))
    }
}
addSubVote("投票解除禁言", "unQuiet", "解除禁言") {
    try {
        val p = getplayer.getPlayer(player!!)!!
        if (!user.contains(p.uuid())) returnReply("[red]此玩家未被禁言".with())
        VoteService.start(player!!, "解除禁言 ${p.name}".with()) {
            user.remove(p.uuid())
        }
    } catch (e: Exception) {
        returnReply("[red]执行禁言出错，请将报错截图并询问管理\n\n{e}".with("e" to e.toString()))
    }
}
suspend fun getplayer(uuid: String): Player {
    val profile = withContext(Dispatchers.IO) {
        transaction { PlayerData.findByIdWithTransaction(uuid) }?.player!!
    }
    return profile
}

onEnable {
    val f = Administration.ChatFilter { p, m ->
        if (user.contains(p.uuid())) {
            p.sendMessage("[yellow]你已被禁言")
            null
        } else m
    }
    netServer.admins.chatFilters.add(f)
}
command("quiet","禁言玩家") {
    permission = "zxs.game.quiet"
    body {
        add(getplayer.getPlayer(player!!)!!.uuid())
    }
}
command("unQuiet","解除玩家禁言") {
    permission = "zxs.game.unQuiet"
    body {
        user.remove(getplayer.getPlayer(player!!)!!.uuid())
    }
}
PermissionApi.registerDefault("zxs.game.quiet", group = "@admin")
PermissionApi.registerDefault("zxs.game.unQuiet", group = "@admin")
fun add(u: String) {
    launch {
        user.add(u)
        delay(600000)
        user.remove(u)
    }
}