@file:Depends("mirai/miraiCommandApi")
@file:Depends("plugins/user/userSql")
@file:Depends("plugins/user/level/Level")

import user.level.Level.Levels
import MiraiCommandApi.T.newMiraiCommand
import user.UserSql.*

newMiraiCommand("info", "个人信息") {
    val p = UserTools.getPlayerGameDataBYID(UserQQTools.getIDBYQQ(sender.id))
    if (p === null) {
        subject.sendMessage("请先绑定")
        return@newMiraiCommand
    }

    val msg = "ID：${p.id}\n职位：${p.zhiWei}(${p.jy}/${Levels.getNextLevelJy(p.jy)})\n贡献点：${p.gongXianZhi}"

    subject.sendMessage(msg)
}

command("inf", "个人信息") {
    body {
        val p = UserTools.getPlayerGameDataBYID(UserUUIDTools.getIDBYUUID(player!!.uuid()!!)!!)!!
            val msg = "ID：${p.id}\n职位：${p.zhiWei}(${p.jy}/${Levels.getNextLevelJy(p.jy)})\n贡献点：${p.gongXianZhi}"
        player.sendMessage(msg)
    }
}