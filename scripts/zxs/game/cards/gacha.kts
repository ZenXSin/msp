@file:Depends("zxs/game/cards/Cards")
@file:Depends("zxs/game/cards/CardType")

package zxs.game.cards

import kotlin.random.Random

fun gacha1(): Int {
    val cards = Cards.Cards().load()
    var rarity = (Random.nextInt(0, 1001) / 100f).toInt()
    if (rarity < 1) rarity = 1
    var findCards: Array<CardType.Card> = arrayOf()
    cards.map {
        if (it.rarity <= rarity) {
            findCards += it
        }
    }
    return findCards[Random.nextInt(0,findCards.size)].id
}

fun gacha2(): CardType.Card {
    val cards = Cards.Cards().load()
    var rarity = (Random.nextInt(0, 1001) / 100f).toInt()
    if (rarity < 1) rarity = 1
    var findCards: Array<CardType.Card> = arrayOf()
    cards.map {
        if (it.rarity <= rarity) {
            findCards += it
        }
    }
    return findCards[Random.nextInt(0,findCards.size)]
}
fun gachas1(time:Int): List<Int> {
    val cards = Cards.Cards().load()
    var ret: List<Int> = listOf()
    repeat(time){
    var rarity = (Random.nextInt(0, 1001) / 100f).toInt()
    if (rarity < 1) rarity = 1
    var findCards: Array<CardType.Card> = arrayOf()
    cards.map {
        if (it.rarity <= rarity) {
            findCards += it
        }
    }
    ret += findCards[Random.nextInt(0, findCards.size)].id
}
    return ret
}

fun gachas2(time:Int): List<CardType.Card> {
    val cards = Cards.Cards().load()
    var ret: List<CardType.Card> = listOf()
    repeat(time){
        var rarity = (Random.nextInt(0, 1001) / 100f).toInt()
        if (rarity < 1) rarity = 1
        var findCards: Array<CardType.Card> = arrayOf()
        cards.map {
            if (it.rarity <= rarity) {
                findCards += it
            }
        }
        ret += findCards[Random.nextInt(0, findCards.size)]
    }
    return ret
}