package zxs.game

import coreMindustry.lib.CommandType
import coreMindustry.lib.command
import coreMindustry.lib.type
import coreMindustry.lib.player
import mindustry.gen.Call

name = "自杀"

command("killme", "自杀") {
    this.type = CommandType.Client
    body {
        Call.sendMessage("玩家：${player!!.name}自杀了")
        player!!.unit().killed()
    }
}