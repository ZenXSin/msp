@file:Depends("coreMindustry/menu")
import coreMindustry.*;
import mindustry.gen.Player
val list: MutableMap<String, Pair<MutableMap<Int, Int>, MutableList<Boolean>>> = mutableMapOf()

launch { kotlin.concurrent.fixedRateTimer(period = 86400000) {
        list.replaceAll { _, v -> v.first to mutableListOf(false) }
    }; Thread.sleep(Long.MAX_VALUE) }
command("sign", "sign") { body { a(player!!) } }
fun g(i: Int) = mutableMapOf(50 to "white", 55 to "white", 60 to "yellow", 70 to "green", 75 to "green", 20 to "orange", 80 to "red", 1 to "black")[i]
suspend fun a(player: Player) {
    MenuV2(player) {
        if (list[player.name] == null) list[player!!.name] = mutableMapOf(9 to 0) to mutableListOf(false)
        if (list[player.name]!!.second[0]) option(" [red]今日已签到 ") {} else {
            if (list[player.name]!!.first.size == 8) list[player!!.name] = mutableMapOf(9 to 0) to mutableListOf(false)
            columnPreRow = 3
            (0..5).forEach { i ->
                option(
                    if (!list[player.name]!!.first.contains(i)) " [green]点击签到 " else if (list[player.name]!!.second[0]) "明日再来" else " [${
                        g(list[player.name]!!.first[i]!!)}]${list[player.name]!!.first[i]} "
                ) {
                    if (!list[player.name]!!.first.contains(i)) {
                        //   list[player.name]!!.second[0] = true
                        val jy = listOf(1 to 0.008, 20 to 0.07, 50 to 0.25, 55 to 0.25, 60 to 0.2, 70 to 0.1, 75 to 0.102, 80 to 0.02).run {
                            var s = 0.0;
                            val r = Math.random();
                            first { s += it.second; s > r }.first }
                        player.sendMessage("[${g(jy)}]签到成功,获得经验+$jy")
                        list[player.name]!!.first += mutableMapOf(i to jy) } else { a(player) } } }
            option((if (list[player.name]!!.first.size !== 7) "[gray]" else "[green]") + "打卡${list[player.name]!!.first.size - 1}/6天") {
                if (list[player.name]!!.first.size == 7) {
                    player.sendMessage("[green]签到成功,获得经验+100")
                    list[player.name]!!.first += mutableMapOf(8 to 0)
                } } } }.send().awaitWithTimeout() }

//测试用命令
command("t1", "") {//下一天
    body {
        list.replaceAll { _, v -> v.first to mutableListOf(false) }
    }
}

command("t2", "") {
    body {
        val p = listOf(1 to 0.008, 20 to 0.07, 50 to 0.25, 55 to 0.25, 60 to 0.2, 70 to 0.1, 75 to 0.102, 80 to 0.02)
        val c = mapOf(
            1 to "black",
            80 to "red",
            20 to "orange",
            70 to "green",
            75 to "green",
            60 to "yellow",
            50 to "white",
            55 to "white"
        )
        val a = IntArray(81)
        repeat(100) {
            var s = 0.0;
            val r = kotlin.random.Random.nextDouble(); a[p.first { s += it.second; s > r }.first]++
        }
        a.withIndex().filter { it.value > 0 }.sortedByDescending { it.value }
            .forEach { player.sendMessage("${it.index}经验(${c[it.index]}): ${it.value}次") }
    }
}