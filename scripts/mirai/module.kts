//by cong wayzer

@file:Depends("coreMindustry")
@file:Import("-Xjvm-default=all-compatibility", compileArg = true)
@file:Import("mirai.lib.*", defaultImport = true)
@file:Import("net.mamoe:mirai-core-jvm:2.15.0-M1", mavenDepends = true)
@file:Import("net.mamoe.mirai.event.*", defaultImport = true)
@file:Import("net.mamoe.mirai.event.events.*", defaultImport = true)
@file:Import("net.mamoe.mirai.message.*", defaultImport = true)
@file:Import("net.mamoe.mirai.message.data.*", defaultImport = true)
@file:Import("net.mamoe.mirai.contact.*", defaultImport = true)


import coreLibrary.lib.event.RequestPermissionEvent
import coreLibrary.lib.util.withContextClassloader
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import net.mamoe.mirai.BotFactory
import net.mamoe.mirai.utils.BotConfiguration
import net.mamoe.mirai.utils.MiraiLoggerPlatformBase
import net.mamoe.mirai.utils.StandardCharImageLoginSolver
import java.util.logging.Level

import net.mamoe.mirai.Bot
import net.mamoe.mirai.auth.QRCodeLoginListener
import net.mamoe.mirai.network.CustomLoginFailedException
import net.mamoe.mirai.utils.MiraiLogger
import net.mamoe.mirai.utils.info
import java.awt.Dimension
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.io.File
import javax.sound.midi.SysexMessage
import javax.swing.ImageIcon
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JWindow
import java.util.logging.Logger
import net.mamoe.mirai.event.events.NewFriendRequestEvent



import net.mamoe.mirai.utils.*
import java.awt.Desktop

import net.mamoe.mirai.auth.BotAuthorization

// 配置项
val enable = true//是否启动机器人(开启前先设置账号密码)
val logVerbose  = false//"向控制台输出mirai完整日记"
val qq = 2504013368L// "机器人qq号"

val channel = Channel<String>(onBufferOverflow = BufferOverflow.DROP_LATEST)

// 自定义日志实现
inner class MyLoggerImpl(override val identity: String, private val botLog: Boolean) : MiraiLoggerPlatformBase() {
    private val logger = Logger.getLogger(identity)

    private fun log(level: Level, message: String?, e: Throwable?) {
        if (logVerbose || botLog || level == Level.WARNING || level == Level.SEVERE) {
            // 生成不包含时间戳的日志消息
            val formattedMessage = "${level.name}: $message"
            logger.log(level, formattedMessage, e)
        }
    }

    override fun verbose0(message: String?, e: Throwable?) {
        logger.log(Level.INFO,message)
  }
    override fun debug0(message: String?, e: Throwable?) {
        logger.log(Level.INFO,message)
    }

    override fun info0(message: String?, e: Throwable?) {
        logger.log(Level.INFO,message)
    }

    override fun warning0(message: String?, e: Throwable?) {
        logger.log(Level.INFO,message)
    }

    override fun error0(message: String?, e: Throwable?) {
        logger.log(Level.INFO,message)
    }
}


withContextClassloader { globalEventChannel() }//init

// 启用插件时执行
onEnable {
    if (!enable) {
        println("机器人未开启,请先修改配置文件")
        return@onEnable
    }
    val auth = BotAuthorization.byQRCode()
    val bot = withContextClassloader {
        BotFactory.newBot(qq, auth) {
            workingDir = Config.dataDir.resolve("mirai").apply { mkdirs() }
            cacheDir = Config.cacheDir.resolve("mirai_cache").relativeTo(workingDir)
            fileBasedDeviceInfo()
            parentCoroutineContext = coroutineContext
            setupQRCodeLoginSolver()
            botLoggerSupplier = { MyLoggerImpl("Bot ${it.id}", true) }
            networkLoggerSupplier = { MyLoggerImpl("Net ${it.id}", false) }
            heartbeatStrategy = BotConfiguration.HeartbeatStrategy.REGISTER
        }
    }
    bot.configuration.protocol = BotConfiguration.MiraiProtocol.ANDROID_WATCH
    launch { bot.login() }
}

// 控制命令
Commands.controlCommand.let {
    it += CommandInfo(this, "mirai", "重定向输入到mirai") {
        usage = "[args...]"
        permission = "mirai.input"
        body {
            channel.trySend(arg.joinToString(" "))
        }
    }
}

// 监听权限请求事件
listenTo<RequestPermissionEvent> {
    val subject = subject
    if (subject is User) group += "qq${subject.id}"
}
// 扫码登录解决器
class QRLoginSolver(
    private val parentSolver: LoginSolver
): LoginSolver() {
    val enable = Desktop.isDesktopSupported()
    override suspend fun onSolvePicCaptcha(bot: Bot, data: ByteArray): String? {
        return parentSolver.onSolvePicCaptcha(bot, data)
    }

    override suspend fun onSolveSliderCaptcha(bot: Bot, url: String): String? {
        return parentSolver.onSolveSliderCaptcha(bot, url)
    }

    override val isSliderCaptchaSupported: Boolean
        get() = parentSolver.isSliderCaptchaSupported

    override fun createQRCodeLoginListener(bot: Bot): QRCodeLoginListener {
        if (enable) {
            return SwingQRLoginListener()
        }
        return parentSolver.createQRCodeLoginListener(bot)
    }

    override suspend fun onSolveDeviceVerification(
        bot: Bot,
        requests: DeviceVerificationRequests
    ): DeviceVerificationResult {
        return parentSolver.onSolveDeviceVerification(bot, requests)
    }

    override suspend fun onSolveUnsafeDeviceLoginVerify(bot: Bot, url: String): String? {
        return parentSolver.onSolveUnsafeDeviceLoginVerify(bot, url)
    }
}

// 设置扫码登录解决器
fun BotConfiguration.setupQRCodeLoginSolver() {
    loginSolver = QRLoginSolver(loginSolver ?: StandardCharImageLoginSolver())
}

// Swing扫码登录监听器
class SwingQRLoginListener : QRCodeLoginListener {
    val logger = MiraiLogger.Factory.create(this::class, "QRLoginSolver")
    private var window: JFrame
    private var image: JLabel
    private var tempBot: Bot? = null
    private var tmpFile: File? = null
    override val qrCodeMargin: Int get() = 4
    override val qrCodeSize: Int get() = 6
    init {
        window = JFrame("扫码登录").apply {
            JFrame.setDefaultLookAndFeelDecorated(true)
            defaultCloseOperation = JFrame.DISPOSE_ON_CLOSE
            setLocationRelativeTo(null)
            if (isAlwaysOnTopSupported) isAlwaysOnTop = true
            addWindowListener(object : WindowAdapter() {
                override fun windowClosed(e: WindowEvent?) {
                    tempBot?.close(UserCancelledLoginException())?.also {
                        logger.info("用户主动取消登录")
                    }
                }
            })
        }
        image = JLabel().apply {
            verticalAlignment = JLabel.CENTER
            horizontalAlignment = JLabel.CENTER
        }
        window.add(image)
    }
    private fun updateQRCode(id: Long, img: ByteArray) {
        val icon = ImageIcon(img)
        val hasIcon = image.icon != null
        image.icon = icon
        image.setBounds(0, 0, icon.iconWidth, icon.iconHeight)
        window.title = "Bot($id) 扫码登陆 (关闭窗口取消登录)"
        window.size = Dimension((icon.iconWidth * 1.8).toInt(), (icon.iconHeight * 1.8).toInt())
        if (!hasIcon) window.setLocationRelativeTo(null)
        window.isVisible = true
    }
    override fun onFetchQRCode(bot: Bot, data: ByteArray) {
        tempBot = bot
        updateQRCode(bot.id, data)
        try {
            val tempFile: File
            if (tmpFile == null) {
                tempFile = File.createTempFile(
                    "mirai-qrcode-${bot.id}-${System.currentTimeMillis() / 1000L}",
                    ".png"
                ).apply { deleteOnExit() }
                
                tempFile.createNewFile()

                tmpFile = tempFile
            } else {
                tempFile = tmpFile!!
            }

            tempFile.writeBytes(data)
            logger.info { "将会在弹出窗口显示二维码图片，请在相似网络环境下使用手机QQ扫码登录。若看不清图片，请查看文件 ${tempFile.absolutePath}" }
        } catch (e: Exception) {
            logger.warning("无法写出二维码图片.", e)
        }
    }

    override fun onStateChanged(bot: Bot, state: QRCodeLoginListener.State) {
        tempBot = bot
        logger.info {
            buildString {
                when (state) {
                    QRCodeLoginListener.State.WAITING_FOR_SCAN -> append("等待扫描二维码中")
                    QRCodeLoginListener.State.WAITING_FOR_CONFIRM -> append("扫描完成，请在手机 QQ 确认登录")
                    QRCodeLoginListener.State.CANCELLED -> append("已取消登录，将会重新获取二维码")
                    QRCodeLoginListener.State.TIMEOUT -> append("扫描超时，将会重新获取二维码")
                    QRCodeLoginListener.State.CONFIRMED -> append("已确认登录")
                    else -> append("default state")
                }
            }
        }
        if (state == QRCodeLoginListener.State.CONFIRMED) {
            kotlin.runCatching { tmpFile?.delete() }.onFailure { logger.warning(it) }
            tempBot = null
            window.isVisible = false
            window.dispose()
        }
    }
}
// 用户取消登录异常
class UserCancelledLoginException : CustomLoginFailedException(true) {
    override val message: String
        get() = "用户主动取消登录"
}