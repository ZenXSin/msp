@file:Depends("mirai")
@file:Depends("zxs/mirai/canSend")
@file:Depends("zxs/game/cards/gacha")
@file:Depends("zxs/game/cards/cardm")

package zxs.mirai

import net.mamoe.mirai.event.globalEventChannel
import net.mamoe.mirai.event.subscribeFriendMessages
import net.mamoe.mirai.event.subscribeMessages
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import wayzer.lib.dao.PlayerProfile
import wayzer.user.UserService
import zxs.game.cards.Gacha
import zxs.game.cards.Cardm
import java.time.LocalDate
import java.time.temporal.ChronoUnit

val userService = contextScript<UserService>()
val cansend = contextScript<CanSend>()
val gacha = contextScript<Gacha>()
val cardm = contextScript<Cardm>()

name = "签到"

object Users : Table("Users") {
    val qq = varchar("qq", length = 1024)
    val ok = varchar("ok", length = 1024)
    val time = varchar("time", length = 1024)
    val day = varchar("day", length = 1024)
}

fun set(qq: Long, ok: Boolean = false, time: Int = 0, day: Int = 0) {
    transaction {
        Users.deleteWhere { Users.qq.eq(qq.toString()) }
        Users.insert {
            it[Users.qq] = qq.toString()
            it[Users.ok] = ok.toString()
            it[Users.time] = time.toString()
            it[Users.day] = day.toString()
        }
    }
}

fun find(qq: Long) {
    var ret = false
    transaction {
        Users.selectAll().map { row ->
            if (row[Users.qq] == qq.toString())
                ret = true
        }
    }
    if (!ret) {
        transaction {
            Users.insert {
                it[Users.qq] = qq.toString()
                it[ok] = "true"
                it[time] = "0"
                it[day] = (day() - 1).toString()
            }
        }
    }
}

fun getok(qq: Long): Boolean? {
    var ret: Boolean? = null
    transaction {
        Users.selectAll().map { row ->
            if (row[Users.qq] == qq.toString())
                ret = row[Users.ok].toBoolean()
        }
    }
    return ret
}

fun gettime(qq: Long): Int? {
    var ret: Int? = null
    transaction {
        Users.selectAll().map { row ->
            if (row[Users.qq] == qq.toString())
                ret = row[Users.time].toInt()
        }
    }
    return ret
}

fun getday(qq: Long): Int? {
    var ret: Int? = null
    transaction {
        Users.selectAll().map { row ->
            if (row[Users.qq] == qq.toString())
                ret = row[Users.day].toInt()
        }
    }
    return ret
}

globalEventChannel().subscribeMessages {
    case("签到") {
        if (cansend.canSend()) {
            val qq = sender.id
            val profile = PlayerProfile.findByQQ(qq)
            if (profile == null) {
                subject.sendMessage("请先绑定游戏")
                return@case
            }
            find(qq)
            val day = getday(qq)!!
            if (day < day()) {
                var time = gettime(qq)!!
                val jy = 10 + 15 * time
                if (time < 30 && day == day() - 1) {
                    time++
                } else if (day != day() - 1) {
                    time = 0
                }
                set(qq, false, time, day())
                val card = gacha.gacha2()
                profile.players.map {
                    cardm.addCard(it.uuid(),card.id)
                }
                subject.sendMessage("----签到成功----\n@${qq}\n连续${time}天签到\n获得经验：${jy}\n获得卡牌：${card.name}\n卡牌介绍：\n${card.descripts}")
                profile.let { userService.updateExp(it, jy) }
            } else {
                subject.sendMessage("您已签到")
            }
        }
    }
}

globalEventChannel().subscribeFriendMessages {
    case("u") {
        transaction {
            SchemaUtils.drop(Users)
            SchemaUtils.create(Users)
        }
    }
}

fun day(): Int {
    val startDate = LocalDate.of(2024, 8, 1)
    val currentDate = LocalDate.now()
    return ChronoUnit.DAYS.between(startDate, currentDate).toInt()
}