package zxs.user

import coreLibrary.lib.PermissionApi
import coreMindustry.lib.command
import mindustry.gen.Player
import org.jetbrains.exposed.sql.transactions.transaction
import wayzer.lib.dao.PlayerData

name = "解绑"

command("unbind", "解除绑定") {
    usage = "[uid]"
    permission = "zxs.game.unbind"
    body {
        val data = PlayerData[getuuid(arg.first())]
        withContext(Dispatchers.IO) {
            transaction { data.unbind() }
        }
        try {
            getplayer(getuuid(arg.first())).kick("[green]解绑成功，请重新进服", 0)
        } catch (_:Exception) {}
    }
}

fun getuuid(uid: String): String {
    return depends("wayzer/user/shortID")?.import<(String) -> String?>("getUUIDbyShort")?.invoke(uid) ?: return ""
}

suspend fun getplayer(uuid: String): Player {
    val profile = withContext(Dispatchers.IO) {
        transaction { PlayerData.findByIdWithTransaction(uuid) }?.player!!
    }
    return profile
}

PermissionApi.registerDefault("zxs.game.unbind", group = "@admin")