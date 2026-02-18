package zxs.game.wake

import coreMindustry.lib.CommandType
import coreMindustry.lib.command
import mindustry.content.Fx
import mindustry.entities.Effect
import mindustry.gen.Player
import mindustry.gen.Call
import arc.graphics.Color
import mindustry.graphics.Layer
//停用
val wakes = mutableMapOf(
    0 to Wake("none","none",10f,Color.red,0f, listOf("none")),
    1 to Wake("测试拖尾","测试拖尾",10f,Color.red,0f, listOf("spawn","crawlDust","unitShieldBreak","dooropen")),
    999 to Wake("999","999",10f,Color.red,0f, listOf("spawn")),
    
         0 to Wake("无","拖尾",10f,Color.red,0f, listOf("none")),
     1 to Wake("火焰","拖尾",5f,Color.valueOf("84fff2"),0f, listOf("burning")),
     2 to Wake("测试2","拖尾",5f,Color.valueOf("84fff2"),0f, listOf("pointHit")),

     4 to Wake("测试4","拖尾",5f,Color.valueOf("84fff2"),0f, listOf("pointShockwave")),//5

     7 to Wake("测试7","拖尾",5f,Color.valueOf("84fff2"),0f, listOf("commandSend")),//3
     8 to Wake("测试8","拖尾",5f,Color.valueOf("84fff2"),0f, listOf("placeBlock")),//8
     9 to Wake("测试9","拖尾",5f,Color.valueOf("84fff2"),0f, listOf("upgradeCoreBloom")),//8
    10 to Wake("测试0","拖尾",5f,Color.valueOf("84fff2"),0f, listOf("coreLaunchConstruct","burning")),//10
    11 to Wake("测试11","拖尾",5f,Color.valueOf("84fff2"),0f, listOf("tapBlock")),//4.5
    12 to Wake("测试12","拖尾",5f,Color.valueOf("84fff2"),0f, listOf("breakBlock")),//3 红
    13 to Wake("测试13","拖尾",5f,Color.valueOf("84fff2"),0f, listOf("select")),//6

    14 to Wake("方1","拖尾",8f,Color.red,0f, listOf("smoke")),//黑烟 1
    15 to Wake("方3","拖尾",8f,Color.red,0f, listOf("rocketSmoke")),//超小黑烟 1

    16 to Wake("方5","拖尾",8f,Color.red,0f, listOf("magmasmoke")),//大黑烟 2
    17 to Wake("方7","拖尾",8f,Color.red,0f, listOf("breakProp")),//红色溅水 3
)

val map by lazy {
    Fx::class.java.fields.filter { it.type == Effect::class.java }
        .associate { it.name to (it.get(null) as Effect) }
}

class Wake(val name: String, val description: String, val time: Float, val color: Color, val range:Float, val effect: List<String>) {
    fun update(player: Player) {
        effect.forEach { i ->
            val effect = map[i]
            effect?.layer = Layer.floor + 0.1f
            Call.effect(effect, player.x, player.y,range,color)
        }
    }
}

fun wake(name: String,description: String,time: Float,color: Color,range:Float,effect: List<String>):Wake {
    return Wake(name, description, time, color, range, effect)
}
command("getEffects", "") {
    body {
        map.forEach { (n, _) ->
            println(n)
        }
    }
}
