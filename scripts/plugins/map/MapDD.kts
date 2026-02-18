package map

import mindustry.Vars
import mindustry.type.Item
import mindustry.world.Block



command("t","") {
    body {
        var gd:MutableList<Float> = mutableListOf()
        state.map.rules().spawns.forEach {
            gd.add(it.type.health)
        }
        returnReply(("出怪强度: " + gd.size / state.map.rules().spawns.size).with())
    }
}

fun t(item: Item) {
    // return (content.blocks().map { it.requirements.map { i -> (i.item == item) } }.size / content.blocks().size) * (item.hardness / 2f)
}