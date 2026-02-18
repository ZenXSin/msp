@file:Depends("zxs/server/monitor")
@file:Depends("zxs/game/cards/cardm")
@file:Depends("zxs/game/wake/wakeToPlayer")

package zxs.main

import coreMindustry.MenuV2
import zxs.game.wake.WakeToPlayer
import coreMindustry.lib.CommandType
import coreMindustry.lib.command
import coreMindustry.lib.player
import coreMindustry.lib.type
import mindustry.gen.Player
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SqlExpressionBuilder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import zxs.server.Monitor
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import zxs.game.cards.Cardm
import wayzer.lib.dao.PlayerData

name = "菜单"

val monitor = contextScript<Monitor>()
val card = contextScript<Cardm>()

val wtp = contextScript<WakeToPlayer>()

suspend fun menu(player: Player) {
    val playerData = PlayerData[player.uuid()]
    if (playerData.profile?.qq != null) {
        MenuV2(player) {
            columnPreRow = 2
            if (playerData.profile?.qq != null) {
                title = "菜单"
                transaction {
                    option(if (monitor.getd().select { monitor.getd().qq.eq(playerData.profile!!.qq.toString()) }
                            .any()) "解除订阅服务器" else "订阅服务器") {
                        transaction {
                            if (monitor.getd().select { monitor.getd().qq.eq(playerData.profile!!.qq.toString()) }
                                    .any()) {
                                transaction {
                                    monitor.remove(playerData.profile?.qq!!)
                                }
                            } else {
                                transaction {
                                    monitor.add(playerData.profile?.qq!!)
                                }
                            }
                        }
                        menu(player)
                    }
                }
                option("卡牌") {
                    card.menuCard(player) {
                        launch {
                            menu(player)
                        }
                    }
                }
                option("拖尾管理") {
                    wtp.menu(player) {
                        launch {
                            menu(player)
                        }
                    }
                }
                option("返回") {}
            } else {
                msg = "请先绑定"
                option("返回") {}
            }
        }.send().awaitWithTimeout()
    }
}

command("menu", "菜单") {
    this.type = CommandType.Client
    body {
        menu(player!!)
    }
}

