@file:Depends("coreMindustry/menu")
@file:Depends("coreLibrary/DBApi")
@file:Depends("coreMindustry/utilTextInput")
@file:Depends("coreMindustry")
@file:Depends("wayzer")
@file:Depends("plugins/user/userSql")
@file:Depends("mirai/miraiCommandApi")

package user

import coreMindustry.lib.listen
import mindustry.game.EventType
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import user.UserSql.*
import kotlin.random.Random
import MiraiCommandApi.T.newMiraiCommand


//绑定
val waitBind: MutableMap<Int, String> = mutableMapOf()

listen<EventType.PlayerJoin> {
    if ((UserUUIDTools.findQQBYUUID(it.player.uuid()) == 0L)) {
        it.player.kick(
            "[red]您未绑定QQ,请在QQ群中发送\n“/绑定 ${getCode(it.player.uuid())}”绑定账号",
            0
        )
    }

}

fun getCode(uuid: String): Int {
    val token = Random.nextInt(100000, 999999)
    if (waitBind.keys.contains(token)) getCode(uuid)
    waitBind.values.remove(uuid)
    waitBind[token] = uuid
    return token
}

command("ttt", "") {
    body {
        //println(waitBind)
        transaction {
            println(UserQQ.selectAll().toList())
        }
    }
}

newMiraiCommand("bind","绑定", precise = false) {
    val cod = message.content.split(" ")
    if (cod.size <= 1){
        subject.sendMessage("绑定失败：请指定绑定码")
        return@newMiraiCommand
    }
    val code = cod[1].toInt()
    if (!waitBind.keys.contains(code)) {
        subject.sendMessage("绑定失败：不存在的绑定码")
        return@newMiraiCommand
    }
    var id = 0
    transaction {
        id = UserTools.addQQ(waitBind[code]!!, sender.id)
    }
    waitBind.remove(code)
    subject.sendMessage("绑定成功,您的ID为：$id")
}

