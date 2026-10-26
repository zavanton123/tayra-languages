package com.tayra.languages.backend

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/** Accounts, subscriptions, cloud backups and sync for the Tayra Languages apps. */
@SpringBootApplication
class TayraBackendApplication

fun main(args: Array<String>) {
    runApplication<TayraBackendApplication>(*args)
}
