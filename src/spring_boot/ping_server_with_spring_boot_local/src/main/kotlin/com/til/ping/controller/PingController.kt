package com.til.ping.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.*

@RestController
class PingController {
    @GetMapping("/api/ping")
    fun ping(): String {
        return "pong!"
    }

    @GetMapping(value = ["/api/echo/{count}", "/api/echo"])
    fun echo(@PathVariable(required = false) count: Integer?, @RequestParam("input") input: String): String {
        var strBuilder: StringBuilder = StringBuilder()
        for (i in 0 until (count as? Int ?: 1)) strBuilder.append(input)
        return strBuilder.toString()
    }
}