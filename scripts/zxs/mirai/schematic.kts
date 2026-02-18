package zxs.mirai

import mindustry.game.Schematics
import net.mamoe.mirai.contact.recallMessage
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.globalEventChannel
import net.mamoe.mirai.event.subscribeMessages
import net.mamoe.mirai.message.data.ForwardMessage
import net.mamoe.mirai.message.data.MessageSource.Key.recall
import net.mamoe.mirai.message.data.buildForwardMessage
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import java.io.File
import net.mamoe.mirai.message.data.PlainText

name = "schematic"

globalEventChannel().subscribeMessages {
    startsWith("bXNja"){
        if (this is GroupMessageEvent) {
            this.message.recall()
        }
        val base = "bXNja$it"
        val schematic = Schematics.readBase64(base)
        val msg = """
        |蓝图名称: ${schematic.name()}
        |蓝图描述: ${schematic.description()}
        |蓝图大小: ${schematic.width}x${schematic.height}
        |电力使用: 产出${schematic.powerProduction()} 消耗${schematic.powerConsumption()} 共${schematic.powerProduction() - schematic.powerConsumption()}
        |建造所需: 
        |${schematic.requirements()}
        |核心蓝图: ${if(schematic.hasCore()){"是"}else{"否"}}
        """.trimMargin()
        val f: ForwardMessage  =  buildForwardMessage {
            add(809109491L, "RA2.EXE", PlainText(msg))
            add(809109491L, "RA2.EXE", PlainText(it))
   }
        subject.sendMessage(f)
    }
}