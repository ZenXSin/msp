package zxs.libs

import coreLibrary.lib.config
import java.util.*
import javax.mail.*
import javax.mail.internet.InternetAddress

name = "邮件发送"

val host by config.key("smtp.qq.com", "smtp邮箱地址")
val port by config.key("587", "smtp邮箱端口")
val mail by config.key("2504013368@qq.com", "smtp邮箱号")
val token by config.key("exnwntxwdjkudhgc", "smtp邮箱密码")

fun sm(to: String, subject: String, body: String) {
    val properties = Properties()
    properties["mail.smtp.host"] = host
    properties["mail.smtp.port"] = port
    properties["mail.smtp.auth"] = "true"
    properties["mail.smtp.starttls.enable"] = "true"

    val session = Session.getInstance(properties, object : Authenticator() {
        override fun getPasswordAuthentication(): PasswordAuthentication {
            return PasswordAuthentication(mail, token)
        }
    })

    val message = javax.mail.internet.MimeMessage(session)
    message.setFrom(InternetAddress(mail))
    message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to))
    message.subject = subject
    message.setText(body)

    Transport.send(message)
}