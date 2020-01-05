package ru.wtrn.cfddns

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class CfDdnsApplication

fun main(args: Array<String>) {
	runApplication<CfDdnsApplication>(*args)
}
