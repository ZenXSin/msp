import net.mamoe.mirai.event.globalEventChannel
import net.mamoe.mirai.event.subscribeMessages

open class ListenType(val name: String)
class Group : ListenType("群聊")
class Friend : ListenType("私信")
class AllListen : ListenType("全部")

class MiraiCommand(
    val listen: String, val description: String, val listenType: ListenType
)

object ListenTypes {
    val group = Group()
    val friend = Friend()
    val AllListen = AllListen()
}

object T {
    val miraiCommands: MutableList<MiraiCommand> = mutableListOf()

    inline fun Script.newMiraiCommand(
        listen: String,
        name: String = listen,
        description: String = name,
        listenType: ListenType = ListenTypes.AllListen,
        precise: Boolean = true,
        crossinline go: suspend MessageEvent.() -> Unit,

        ) {
        onEnable {
            miraiCommands.add(MiraiCommand(listen, description, listenType))
        }
        launch {
            if (precise) {
                println(listen)
                when (listenType) {
                    ListenTypes.group -> {
                        globalEventChannel().subscribeGroupMessages {
                            listen {
                                this.apply {
                                    go(this@listen)
                                }
                            }
                        }
                    }

                    ListenTypes.friend -> {
                        globalEventChannel().subscribeFriendMessages {
                            listen {
                                this.apply {
                                    go(this@listen)
                                }
                            }
                        }
                    }

                    ListenTypes.AllListen -> {
                        globalEventChannel().subscribeMessages {
                            listen {
                                this.apply {
                                    go(this@listen)
                                }
                            }
                        }
                    }
                }
            } else {
                when (listenType) {
                    ListenTypes.group -> {
                        globalEventChannel().subscribeGroupMessages {
                            contains(listen, true) {
                                this.apply {
                                    go(this@contains)
                                }
                            }
                        }
                    }

                    ListenTypes.friend -> {
                        globalEventChannel().subscribeFriendMessages {
                            contains(listen, true) {
                                this.apply {
                                    go(this@contains)
                                }
                            }
                        }
                    }

                    ListenTypes.AllListen -> {
                        globalEventChannel().subscribeMessages {
                            contains(listen, true) {
                                this.apply {
                                    go(this@contains)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}