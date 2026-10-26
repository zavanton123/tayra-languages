package com.tayra.languages.backend.auth

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Component

/** Sends the service's few emails. */
interface Mailer {
    fun send(to: String, subject: String, text: String)
}

/** By SMTP when it is set up; otherwise into the log, for development. */
@Component
class SmtpOrLogMailer(
    private val smtp: ObjectProvider<JavaMailSender>,
    @Value("\${tayra.mail.from}") private val from: String,
) : Mailer {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun send(to: String, subject: String, text: String) {
        val sender = smtp.ifAvailable
        if (sender == null) {
            log.info("Mail to {} (no SMTP set up): {}\n{}", to, subject, text)
            return
        }
        sender.send(SimpleMailMessage().apply {
            setFrom(from)
            setTo(to)
            setSubject(subject)
            setText(text)
        })
    }
}
