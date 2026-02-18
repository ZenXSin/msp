@file:Depends("zxs/game/cards/Cards")
@file:Depends("zxs/libs/getplayer")

package zxs.game.cards

import coreLibrary.lib.PermissionApi
import coreMindustry.MenuV2
import coreMindustry.lib.command
import coreMindustry.lib.game
import coreMindustry.renderPaged
import mindustry.gen.Player
import zxs.game.cards.Cards.Cards
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import zxs.libs.Getplayer

name = "卡牌管理"
val getplayer = contextScript<Getplayer>()

object UserCards : Table("UserCards") {
    val id = integer("id").autoIncrement()
    val uuid = varchar("uuid", length = 1024)
    val cards = integer("hasCards")
}

fun addCard(uuid: String, id: Int) {
    transaction {
        UserCards.insert {
            it[UserCards.uuid] = uuid
            it[cards] = id
        }
    }
}

fun removeCard(uuid: String, id: Int) {
    transaction {
        val fd = UserCards.select {
            (UserCards.uuid.eq(uuid)) and (UserCards.cards.eq(id))
        }.limit(1).firstOrNull()?.get(UserCards.id)
        UserCards.deleteWhere { UserCards.id.eq(fd!!) }
    }
}

fun getCard(uuid: String): List<Int> {
    return transaction {
        UserCards.select { UserCards.uuid.eq(uuid) }
            .map { it[UserCards.cards] }
    }
}

suspend fun allCard(player: Player, ret: (player: Player) -> Unit) {
    val cards = Cards()
    MenuV2(player) {
        columnPreRow = 1
        title = "所有卡牌"
        msg = "点击进入详情页面"
        option("返回") { ret(player) }
        renderPaged(Cards().load()) {
            option(it.name) {
                MenuV2(player) {
                    columnPreRow = 1
                    title = name
                    msg = "${name}\n类型：${it.type.id}\n${it.descripts}"
                    if (player.admin) {
                        option("[red]为玩家添加此卡牌") {
                            addCard(getplayer.getPlayer(player)!!.uuid(),it.id)
                            allCard(player,ret)
                        }
                    }
                    option("[yellow]返回") {
                        ret(player)
                    }
                }.send().awaitWithTimeout()
            }
        }
    }.send().awaitWithTimeout()
}

suspend fun menuCard(player: Player, ret: (player: Player) -> Unit) {
    val cards = Cards().load()
    val cardsM = Cards().loadM()
    MenuV2(player) {
        columnPreRow = 2
        title = "已有卡牌管理"
        msg = "点击进入详情页面"
        option("返回") { ret(player) }
        option("所有卡牌") {
            allCard(player) {
                launch(Dispatchers.game) {
                    menuCard(it) { ret(it) }
                }
            }
        }
        transaction {
            renderPaged(getCard(player.uuid())) {
                val card = cardsM[it]!!
                option(card.name) {
                    MenuV2(player) {
                        title = card.name
                        msg = "${card.name}\n类型：${card.type.id}\n${card.descripts}"
                        option("点击使用") {
                            card.use(player)
                            removeCard(player.uuid(), it)
                        }
                        option("返回") { menuCard(player) { ret(player) } }
                    }.send().awaitWithTimeout()
                }
            }
        }
    }.send().awaitWithTimeout()
}

command("upcardsql", "重置卡牌数据库") {
    permission = "zxs.cards.upcardsql"
    body {
        transaction {
            SchemaUtils.drop(UserCards)
            SchemaUtils.create(UserCards)
        }
    }
}
PermissionApi.registerDefault("zxs.cards.upcardsql", group = "@admin")
fun getuuid(uid: String): String {
    return depends("wayzer/user/shortID")?.import<(String) -> String?>("getUUIDbyShort")?.invoke(uid) ?: return ""
}