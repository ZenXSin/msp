package zxs.game.cards

import mindustry.gen.Player

object CardTypes {
    val unit = CardType("Unit")
    val build = CardType("Build")
    val auxiliary = CardType("Auxiliary")
    val other = CardType("Other")
}

class CardType(val id: String)

class Card(
    val rarity: Int,
    val id: Int,
    val type: CardType,
    val name: String,
    val descripts: String,
    val body: (player: Player) -> Unit
) {
    fun use(player: Player) {
            body(player)
    }

    fun get(): Card {
        return this
    }

    override fun toString(): String {
        return "{id:${id},name:${name}}"
    }
}
