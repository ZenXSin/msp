package zxs.mirai

import arc.util.Strings
import net.mamoe.mirai.Bot
import net.mamoe.mirai.contact.isAdministrator
import net.mamoe.mirai.event.globalEventChannel
import net.mamoe.mirai.event.subscribeMessages
import wayzer.MapManager
import wayzer.MapRegistry

globalEventChannel().subscribeMessages {
    contains("查询地图", true) {
        var desc = ""
        try {
            withTimeoutOrNull(3000) {
                val map = MapRegistry.findById(it.replace("查询地图 ", "").toInt())
                if (map == null) {
                    desc = "查无此图"
                    return@withTimeoutOrNull
                }
                desc = "地图作者: ${Strings.stripColors(map.map.author())}[][]\n" +
                        "地图简介: ${Strings.truncate(Strings.stripColors(map.map.description()), 100, "...")}"
            } ?: { desc = "查询超时" }
        } catch (e:Exception) {
            desc = "查询错误\n${e.message}"
        } finally {
            subject.sendMessage(desc)
        }
    }
    case("换图") {
        Bot.instancesSequence.forEach {
            if (it.getGroup(878232735L)?.members?.get(sender.id)?.isAdministrator() != true) {
                it.getFriend(sender.id)?.sendMessage("您无管理员权限，请勿使用管理员命令")
                return@case
            }
        }
        MapManager.loadMap()
    }
}