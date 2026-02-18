package zxs.libs

import mindustry.gen.Player
import coreMindustry.MenuV2
import mindustry.gen.Groups
import coreMindustry.renderPaged

suspend fun getPlayer(player: Player): Player? {
    var ret: Player? = null
    MenuV2(player) {
        title = "选择玩家"
        msg = "请选择玩家"
        columnPreRow = 2
        renderPaged(Groups.player.toList()) {
                option(it!!.name) {
                    ret = it
            }
        }
    }.send().awaitWithTimeout()
    return ret
}