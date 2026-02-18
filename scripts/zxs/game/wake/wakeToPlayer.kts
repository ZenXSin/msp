@file:Depends("zxs/game/wake/wake")
@file:Depends("wayzer/user/level")
package zxs.game.wake
//停用
import zxs.game.wake.Wake
import coreMindustry.MenuV2
import mindustry.gen.Groups
import mindustry.gen.Player
import wayzer.lib.dao.PlayerProfile
import wayzer.lib.dao.PlayerData
import wayzer.user.Level
import mindustry.game.EventType
import arc.Events
import arc.util.Time
import coreMindustry.renderPaged

var canUpdate = true
val wake = contextScript<Wake>()
val level = contextScript<Level>()
open class PPair<T, U> {
    var first:T? = null
    var second:U? = null
}
var playerWakes: MutableMap<String,PPair<MutableList<Wake.Wake>,Wake.Wake>> = mutableMapOf()
val okPlayerQQ = listOf(488254306L)

onEnable {
    Events.run(EventType.Trigger.update) {
        if (!canUpdate) return@run
        l()
        Groups.player.forEach {
             val profile = PlayerData.findById(it.uuid())?.profile
           // val profile = PlayerData[it!!.uuid()].profile
            wake.wakes.forEach { (k , v) ->
                if ((k <= level.level(profile?.totalExp ?: 0)) || okPlayerQQ.contains(profile?.qq)) {
                    if (playerWakes[it.uuid()] != null && !playerWakes[it.uuid()]!!.first!!.contains(v)) {
                        playerWakes[it.uuid()]!!.first!!.add(v)
                    } else if(playerWakes[it.uuid()] === null) {
                        playerWakes[it.uuid()] = object : PPair<MutableList<Wake.Wake>, Wake.Wake>() {
                            init {
                                first = mutableListOf(v)
                                second = v
                            }
                        }
                    }
                }
           }
       }
    }

    //注册尾迹监听
    wake.wakes.forEach { (k , v) ->
        launch {
            var ok = false
            Events.run(EventType.Trigger.update) {
                if (ok) return@run
                ok = true
                Time.runTask(v.time) {
                    ok = false
                }
                Groups.player.forEach {
                    playerWakes[it.uuid()]?.let { i ->
                        if (i.second!!.name == v.name) v.update(it)
                    }
                }
            }}

   }
}

fun l() {
    canUpdate = false
    Time.runTask(300f) {//玩家卡牌5秒刷新一次
        canUpdate = true
    }
}
fun getWakes() {
    wake.wakes.forEach {
        println(it)
    }
}
 suspend fun menu(player:Player,ret:(player:Player) -> Unit) {
     launch {
        MenuV2(player) {
            columnPreRow = 2
    title = "拖尾管理"
    msg = "点击进入详情页\n当前选择的拖尾：${playerWakes[player.uuid()]!!.second!!.name}\n当前拥有拖尾："
    option("返回") { ret(player) }
    renderPaged(playerWakes[player.uuid()]!!.first!!) { i ->
        println(i)
        option(i.name) {
            MenuV2(player) {
                columnPreRow = 2
                title = i.name
              msg = "${i.name}\n${i.description}"
                option("[yellow]选择") {
                    playerWakes[player.uuid()]!!.second = i
                }
                option("[yellow]返回") {
                    menu(player,ret)
                }
            }.send().awaitWithTimeout()
        }
    }
        }.send().awaitWithTimeout()
    }
}
command("wake","拖尾管理") {
       aliases = listOf("拖尾")
    body {
       try {
           launch {
               menu(player!!) {}}
       } catch (e:Exception) {
           println(e)
       }
    }
}