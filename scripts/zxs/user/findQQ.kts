@file:Depends("coreMindustry")
@file:Depends("wayzer/user/userService")
@file:Depends("zxs/libs/getplayer")

package zxs.user

import coreLibrary.lib.PermissionApi
import coreLibrary.lib.with
import coreMindustry.lib.command
import wayzer.lib.dao.PlayerData
import zxs.libs.Getplayer

name = "查找QQ"

val getplayer = contextScript<Getplayer>()

fun getuuid(uid: String): String {
    return depends("wayzer/user/shortID")?.import<(String) -> String?>("getUUIDbyShort")?.invoke(uid) ?: return ""
}

command("fq","通过uid查询qq") {
    this.type = CommandType.Client
    permission = "zxs.game.fq"
    body {
        returnReply(PlayerData[getplayer.getPlayer(player!!)!!.uuid()].profile?.qq.toString().with())
    }
}
command("findqq","通过uid查询qq") {
    this.type = CommandType.Server
    usage = "[uid]"
    permission = "zxs.game.findqq"
    body {
        returnReply(PlayerData[getuuid(arg.first())].profile?.qq.toString().with())
    }
}
PermissionApi.registerDefault("zxs.game.fq", group = "@admin")