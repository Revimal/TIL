package com.til.ping.controller

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers

@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PingControllerTest() {
    private val alphabet: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
    private val randomAlphabets: (Int) -> String = {count -> List(count){alphabet.random()}.joinToString("")}

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun pingTest() {
        val uri: String = "/api/ping"
        mockMvc.perform(MockMvcRequestBuilders.get(uri))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.content().string("pong!"))
            .andDo(MockMvcResultHandlers.print())
    }

    @Test
    fun echoTest() {
        val uri: String = "/api/echo"
        val param: String = "input"
        val input: String = randomAlphabets(32)

        mockMvc.perform(MockMvcRequestBuilders.get(uri).param(param, input))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.content().string(input))
            .andDo(MockMvcResultHandlers.print())
    }

    @Test
    fun echoWithCountTest() {
        val uri: String = "/api/echo/{count}"
        val param: String = "input"
        val count: Int = (0..10).random()
        val input: String = randomAlphabets(count)
        val validator: String = input.repeat(count)

        mockMvc.perform(MockMvcRequestBuilders.get(uri, count).param(param, input))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.content().string(validator))
            .andDo(MockMvcResultHandlers.print())
    }
}
