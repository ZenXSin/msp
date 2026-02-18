@file:Depends("coreLibrary/DBApi")
@file:Depends("plugins/user/level/Level")
@file:Depends("coreMindustry/menu")
@file:Depends("coreMindustry")
@file:Depends("wayzer")
@file:Depends("mirai/miraiCommandApi")

package user

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import user.level.Level.Levels
import MiraiCommandApi.T.newMiraiCommand

object UserQQ : Table("UserQQ") {
    val id = integer("id").autoIncrement()
    val uuid = varchar("uuid", length = 1024)
    val qq = varchar("qq", length = 1024)
    val jy = varchar("经验", length = 1024)
    val zhiWei = varchar("职位", length = 1024)
    val gongXianZhi = varchar("贡献点", length = 1024)
}

class PlayerGameData(
    val id:Int,
    val uuid:String,
    val qq:String,
    val jy:Int,
    val zhiWei:String,
    val gongXianZhi:String,
)

object UserTools {
    fun addQQ(uuid: String, qq: Long): Int {
        transaction {
            //addLogger(StdOutSqlLogger)
            UserQQ.insert {
                it[UserQQ.uuid] = uuid
                it[UserQQ.qq] = qq.toString()
                it[jy] = "0"
                it[zhiWei] = "新兵"
                it[gongXianZhi] = "0"
            }
        }
        return UserUUIDTools.getIDBYUUID(uuid)
    }
    fun getPlayerGameDataBYID(id:Int):PlayerGameData? {
        var ret:PlayerGameData? = null
        transaction {
            //addLogger(StdOutSqlLogger)
            UserQQ.selectAll().toList().map {
                if (it[UserQQ.id] == id) {
                    ret = PlayerGameData(it[UserQQ.id],it[UserQQ.uuid],it[UserQQ.qq],it[UserQQ.jy].toInt(),it[UserQQ.zhiWei],it[UserQQ.gongXianZhi])
                }
            }
        }
        return ret
    }
    fun addJyById(id:Int,addJy:Int): PlayerGameData? {
        var ret:PlayerGameData? = null
        transaction {
            addLogger(StdOutSqlLogger)
            UserQQ.selectAll().toList().map { t ->
                if (t[UserQQ.id] == id) {
                    UserQQ.insert {
                        it[UserQQ.id] = id
                        it[jy] = (it[jy].toInt() + addJy).toString()
                    }

                    updateLevelSql()
                    ret = PlayerGameData(t[UserQQ.id],t[UserQQ.uuid],t[UserQQ.qq],t[UserQQ.jy].toInt(),t[UserQQ.zhiWei],t[UserQQ.gongXianZhi])
                    }
            }
        }
        return ret
    }

    private fun updateLevelSql() {
        transaction {
            addLogger(StdOutSqlLogger)
            UserQQ.selectAll().toList().map { t ->
                UserQQ.insert {
                    it[id] = t[id]
                    it[zhiWei] = Levels.getLevel(t[jy].toInt()).name
                }
            }
        }
    }

}

object UserUUIDTools {
    fun removeQQByUUID(uuid: String) {
        transaction {
            //addLogger(StdOutSqlLogger)
            UserQQ.deleteWhere { UserQQ.uuid.eq(uuid) }
        }
    }
    fun findQQBYUUID(uuid: String): Long {
        var inReturn = 0L
        try {
            transaction {
                //addLogger(StdOutSqlLogger)
                UserQQ.selectAll().toList().map {
                    if (it[UserQQ.uuid] == uuid) {
                        inReturn = it[UserQQ.qq].toLong()
                    }
                }
            }
        } catch (e:Exception) {
            println("\u001B[31m findQQBYUUID: $e \u001B[0m")
        }
        return inReturn
    }
    fun getIDBYUUID(uuid:String): Int {
        var inReturn = 0
        try {
            transaction {
                //addLogger(StdOutSqlLogger)
                UserQQ.selectAll().toList().map {
                    if (it[UserQQ.uuid] == uuid) {
                        inReturn = it[UserQQ.id]
                    }
                }
            }
        } catch (e:Exception) {
            println("\u001B[31m findQQBYUUID: $e \u001B[0m")
        }
        return inReturn
    }
}

object UserQQTools {
    fun removeQQByQQ(qq: Long) {
        transaction {
            //addLogger(StdOutSqlLogger)
            UserQQ.deleteWhere { UserQQ.qq.eq(qq.toString()) }
        }
    }

    fun findUUIDBYQQ(qq: Long): String {
        var inReturn = ""
        try {
            transaction {
                //addLogger(StdOutSqlLogger)
                UserQQ.selectAll().toList().map {
                    if (it[UserQQ.qq] == qq.toString()) {
                        inReturn = it[UserQQ.uuid]
                    }
                }
            }
        } catch (e:Exception) {
            println("\u001B[31m findUUIDBYQQ: $e \u001B[0m")
        }
        return inReturn
    }

    fun getIDBYQQ(qq:Long): Int {
        var inReturn = 0
        try {
            transaction {
                UserQQ.selectAll().toList().map { t ->
                    if (t[UserQQ.qq] == qq.toString()) {
                        inReturn = t[UserQQ.id]
                    }
                }
            }
        } catch (e:Exception) {
            println("\u001B[31m findQQBYUUID: $e \u001B[0m")
        }
        return inReturn
    }
}


command("upqqsql", "重置用户数据库") {
    RequirePermission("scriptAgent.admin")
    body {
        transaction {
            //addLogger(StdOutSqlLogger)
            SchemaUtils.drop(UserQQ)
            SchemaUtils.create(UserQQ)
        }
    }
}

onEnable {

}

newMiraiCommand("tst","tst",precise = false) {
    val jy = message.content.split(" ")[0].toInt()
    //UserTools.addJyById(UserQQTools.getIDBYQQ(sender.id),jy)
    subject.sendMessage(UserQQTools.getIDBYQQ(sender.id).toString())
}