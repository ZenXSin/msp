@file:Depends("mirai")
@file:Depends("zxs/mirai/canSend")

package zxs.mirai

import coreLibrary.lib.with
import net.mamoe.mirai.event.globalEventChannel
import net.mamoe.mirai.event.subscribeMessages
import wayzer.lib.dao.PlayerProfile
import zxs.mirai.CanSend

name = "个人信息"
val cansend = contextScript<CanSend>()

globalEventChannel().subscribeMessages {
    case("个人信息") {
        if (cansend.canSend()) {
            val profile = PlayerProfile.findByQQ(sender.id)
            if (profile != null) {
                subject.sendMessage(
                    """    | 当前绑定账号: {profile.id}
    | 总在线时间: {profile.onlineTime:分钟}
    | 当前等级: {profile.level}
    | 当前经验(下一级所需经验): {profile.totalExp}({profile.nextLevel})
    | 注册时间: {profile.registerTime:YYYY-MM-dd}""".with("profile" to profile).toString()
                )
            } else {
                subject.sendMessage("查询失败，可能是未绑QQ")
            }
        }
    }
}
