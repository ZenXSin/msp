package user.level

class Level(val name:String,val jy:Int) {
    init {
        Levels.levels.add(this)
    }
}

object Levels {
    val levels: MutableList<Level> = mutableListOf()
    val ZhengZhaoBin = Level("征召兵",0)
    val LieBin = Level("列兵",1000)
    val ZhongShi = Level("中士",3000)
    val ZhiHuiGuan = Level("指挥官", 5000)

    fun getLevel(jy: Int): Level {
        for (level in levels.reversed()) {
            if (jy >= level.jy) return level
        }
        return levels.first()
    }

    fun getNextLevelJy(jy: Int): Int {
        val current = getLevel(jy)           // 当前等级
        val nextIndex = levels.indexOf(current) + 1
        return if (nextIndex < levels.size) {
            levels[nextIndex].jy
        } else {
            0                                // 已到顶
        }
    }
}