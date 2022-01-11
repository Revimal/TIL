# Spring Boot를 활용한 Ping 서버 #1

## 구현 및 로컬 실행 환경

_2022_02_07_

### 개요

* Spring Boot는 Spring 프레임워크[^1]를 쉽게 시작할 수 있게끔 도와주는 도구.
* 이를 활용해서, MVC 기반의 기본적인 REST API 서버를 제작하며 학습 진행.



### 요구사항 명세

이번에는 아래 3개의 기능을 가진, REST API 서버를 로컬에서 띄워보는 걸 목표로 한다.

* `/api/ping`으로 GET을 날리면 `pong!`을 반환해주는 API.
* `/api/echo`에 `?input=hello`와 같이 파라미터를 함께 넘겨주면 `hello`를 그대로 반환해주는 API.
* `/api/echo/3?input=hello`와 같은 식으로 GET을 날리면 `hellohellohello`와 같이 반환해주는 API.



### 요구사항 분석

* Request를 받고 Respose를 돌려 줄, 단일 컨트롤러만 구현해도 충분할 것이다.
  * 데이터의 영속적인 저장이 필요 없기 때문에, DB 구성이나 그에 상응하는 모델 구현이 필요 없다.
  * `PingController`와 `EchoController`로 나누어도 좋지만, 로직이 그리 많지는 않아 필수적이지는 않다.



### 구현

_Spring Boot 2.6.2 / Kotlin 1.6_

##### PingController

```kotlin
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
```

`@RestController`: 기존 `@Controller` 어노테이션에서 `@ResponseBody`가 추가된 것.

* 기존 `@Controller`는 View를 반환해 렌더링 하지만, `@RestController`는 데이터를 그대로 반환해 사용자 전달.

`@GetMapping`: 기존 `@RequestMapping` 어노테이션에서 HTTP Method를 `GET`으로 지정한 것.

* `@RequestMapping`: URL를 컨트롤러의 메서드와 매핑할 때 사용하며, `value`로 URL / `method`로 요청 형식 지정.
* `value = [...]`로 구현했는데, 이는 `/api/echo/{count}`와 `/api/echo`를 모두 `echo`에서 처리한다는 의미.

`@PathVariable`: URL에서 `{}` 표기된 부분의 값을 가져오며, 기본적으로는 뒤 이어오는 변수명과 동일한 값을 갖고 옴.

* `@PathVariable(value="count")`와 같이 URL에서 가져올 값을 직접 지정할 수 있으며, 이 경우 변수명은 자유.
* `required = false`로 `{count}`가 포함되어 있지 않은 URL이 들어올 수 있다는 것을 표시.

`.count: Integer?`: 요청에 `{count}`가 없을 경우, `count` 변수가 `null`이 될 수 있기 때문에 `Nullable` 명시적 표기.

* 그에 따라 아래쪽 `(count as? Int ?: 1)`에서 타입 캐스팅이 불가능 한 경우 1로 변환되게끔 처리.

`@RequestParam`: URL에서 `?input="value"`와 같이 들어오는 파라미터를 갖고 옴.



##### PingControllerTest

```kotlin
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
```

`@AutoConfigureMockMvc`: `@WebMvcTest`와 비슷하게 사용할 수 있으나, 어플리케이션 전체 구성을 로드할 때 사용.

* `@AutoConfigureMockMvc`는 전체 어플리케이션을 로드하는 `@SpringBootTest`와 함께 사용한다.
* 그에 반해 `@WebMvcTest`는 단독으로 사용하며, `@Controller`와 그 관련 빈만 스캔하여 로드한다.
* `@AutoConfigureMockMvc`는 전체 어플리케이션 컨텍스트를 불러오기에 느리다는 단점이 있고.
* `@WebMvcTest`는 서비스나 다른 계층의 빈에 대한 테스트를 진행하려면 직접 로드해야 한다는 단점이 있다.
* 요약하면, 전체 구성 테스트 시에는 `@AutoConfigureMockMvc`를, 컨트롤러만 테스트 시에는 `@WebMvcTest`를 쓴다.

`@SpringBootTest(webEnvironment = ...)`: SpringBoot 내장 톰켓을 사용할 것인지 여부와 상세 설정 결정.

* `WebEnvironment.Mock`: 웹 환경을 모킹하여, 톰켓 서버를 실행하지 않고 테스트.
* `WebEnvironment.RANDOM_PORT`: `WebServerApplicationContext`를 로드하고, 톰켓을 랜덤 포트로 실행.
* `WebEnvironment.DEFINED_PORT`: `RANDOM_PORT`와 동일하나, `application.properties`의 포트로 실행.
* `WebEnvironment.NONE`: 웹 관련 테스트를 할 수 없으며, 메서드에 대한 직접 호출 테스트만 할 수 있음.

`@Autowired`: IoC 컨테이너에 등록된 빈 중에 필요한 것을 자동으로 찾아 의존성 주입을 수행한다.

* 코틀린에서는 `lateinit`과 같이 쓰지 않으면 의존성 주입 이전에 `Non-null`타입인데 초기화 안했다고 오류가 난다.
* Setter에 `@Autowired`를 걸어 의존성을 찾거나, 클래스의 생성자에 파라미터로 직접 의존성을 걸어주는 방식도 가능.

`@Test`: 각 테스트 케이스를 구성

* `mockMvc`를 가지고 `perform`, `andExpect`, `andDo` 를 사용해 테스트 진행.

* `MockMvcRequestBuilders.get()`: GET method를 가진 요청을 생성 (POST / PUT / DELETE 등도 존재).

* `MockMvcResultMatchers`: Response로 받은 결과를 비교하여 성공 실패를 결정.

---

[^1]: Spring 프레임워크 자체에 대한 일반적인 개념 설명은 다른 TIL에서 진행할 예정

---

##### Reference

1. ["[Spring-boot] 시작하기 전 알아야 할 것들" by "개발인생"](https://hello-bryan.tistory.com/319)
2. ["@Controller와 @RestController 차이" by "망나니개발자"](https://mangkyu.tistory.com/49)
3. ["Spring, @Controller @RestController 차이" by "devham76"](https://devham76.github.io/spring/Spring-controllerRestController/)
4. ["Spring에서 @RequestParam과 @PathVariable" by "mhlab"](https://elfinlas.github.io/2018/02/18/spring-parameter/)
5. ["[Kotlin] 코틀린 null 처리 - ? ?. ?: !!, let, lateinit, 제너릭, 플랫폼 타입" by "뚜덜이~"](https://tourspace.tistory.com/114)
6. ["스프링부트 테스트코드 작성시 @WebMvcTest 와 @AutoConfigureMockMvc 의 차이점" by "Meaning"](https://we1cometomeanings.tistory.com/65)
7. ["Difference between using MockMvc with SpringBootTest and Using WebMvcTest" by "StackOverflow"](https://stackoverflow.com/questions/39865596/difference-between-using-mockmvc-with-springboottest-and-using-webmvctest)
8. ["MVC 테스트 어노테이션 알아보기" by "devebucks"](https://pinokio0702.tistory.com/143)