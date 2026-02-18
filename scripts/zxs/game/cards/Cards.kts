@file:Depends("zxs/game/cards/CardType")

package zxs.game.cards

import arc.graphics.Color
import kotlinx.coroutines.*
import mindustry.content.Fx
import mindustry.content.UnitTypes
import mindustry.gen.Call
import mindustry.gen.Groups
import zxs.game.cards.CardType.Card
import zxs.game.cards.CardType.CardTypes
import kotlin.math.sqrt

class Cards {
     val spawnmono = Card(1,1, CardTypes.unit, "召唤苦力", "召唤3个mono为你打工") { p ->
        repeat(5) {
            UnitTypes.mono.spawn(p.team(), p.x(), p.y())
        }
    }
     val treatment = Card(1,2, CardTypes.unit, "召唤治疗点", "在玩家身边召唤一个治疗点，治疗附近单位持续5秒") { p ->
         runBlocking {
             repeat(5) {
                 Call.effect(Fx.launch, p.x, p.y, 100f, Color.green)
                 Groups.unit.map {
                     if (it.team() == p.team() && sqrt((it.x() - p.x()) * (it.x() - p.x()) + (it.y() - p.y()) * (it.y() - p.y()).toDouble()).toInt() <= 15 * 8) {
                         if (it.health + 500 < it.maxHealth) {
                             it.health(it.health + 500)
                         } else {
                             it.health(it.maxHealth)
                         }
                     }
                 }
                 delay(1000)
             }
         }
    }
    fun load(): List<Card> {
        return listOf(spawnmono,treatment)
    }
    fun loadM(): Map<Int, Card> {
        return mapOf(1 to spawnmono,2 to treatment)
    }
}