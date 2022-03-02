# Spring Boot에서 Kotlin 스타일로 테스트 하기

## 부제: Kotest와 Mockk를 이용한 테스트 코드 작성

_2022/03/02_

### 개요

* 프로젝트를 진행함에 있어서, 테스트는 코드의 무결성을 보장해주는 최소한의 안전 장치이다.
* 특정 기능을 아무리 견고하게 작성했더라도, 코드를 변경하게 되면 개발자는 큰 불안감을 느낀다.
* 이때 근간이 되는 요구 사항을 테스트 코드로 작성해놓았다면, 변경된 기능의 안정성을 신속하고 신뢰성 있게 검증할 수 있다.
* 그럼 더 나아가, 구현에 앞서 테스트 케이스를 작성한다면 더 좋지 않을까?
* 요구 사항에 대한 테스트 케이스를 먼저 작성하고, 그에 맞추어가며 코드를 작성해가는 개념을 TDD[^1]라고 부른다.
* 자, 그러면 Kotlin + Spring Boot 프로젝트 구성에서 TDD를 하기 위한 방법에는 뭐가 있을까?
* 뭐... 여러가지 방법이 있겠지만, 나는 [kotest](https://kotest.io/)와 [mockk](https://mockk.io/)을 조합해서 사용하기로 했다.

### Kotest와 Mockk를 선택한 이유

#### Kotest를 선택한 이유

* Spring Boot를 테스트 할 수 있는 프레임워크에는 여러가지가 있었다.
* 가장 많이 사용되는 JUnit, BDD[^2] 개념을 받아들인 Spock와 그 Kotlin 파생형인 Spek이 대표적.
* 하지만 나는 아래와 같은 이유로 Kotest를 선택했다.
  * 처음부터 Kotlin을 위해 개발된 테스트 프레임워크로, Kotlin DSL[^3]을 완벽하게 지원한다.
  * Spock/Spek과 같이 BDD 개념도 쉽게 적용할 수 있고, 그 외에도 Describe 등의 다양한 테스트 스타일을 지원한다.
  * 공식 예제 코드를 봐도, `@Annotation`을 떡칠하지 않고 **예쁘게** 테스트 코드를 작성 할 수 있다[^4].
  * 프레임워크 생태계나 문서화 또한 활발해서, 사후 지원에 대한 걱정을 사실상 하지 않아도 되었다.
* '이래서 Kotest를 사용한다' 보다는 '모든게 다 되는 Kotest를 사용하지 않을 이유가 없다'에 가까운 느낌이었다.

#### Mockk을 선택한 이유

* 모킹 프레임워크를 고를 때에는 큰 고민이 없었다.
* 이전까지 Java/Kotlin 프로젝트에서 모킹을 할 때에는 [Mockito](https://site.mockito.org/)를 사용했었다.
* 큰 이유는 없었고, 제일 레퍼런스도 많고 주변에서 얘기도 많이 해서 관성에 따라 사용했었다.
* 그런데 테스트 프레임워크를 Kotest로 바꾸고 나니까, '모킹 프레임워크도 더 좋은게 있는거 아니야?'라고 생각이 들었고.
* 마침 그때쯤 해서, 같이 사이드 프로젝트를 진행하던 팀원이 '저는 Mockk 써서 모킹하는데요!'라는 얘기를 해 주었다.
* 찾아보니까, 'Mockito에서 지원하는 모든 기능에 몇몇 추가 기능을 더한, Kotlin을 위한 모킹 프레임워크'라고 나오더라.
  * Coroutine 지원도 되고, Object도 모킹할 수 있고, Private 메소드도 모킹할 수 있고, 생성자까지 모킹할 수 있더라.
  * 그냥 쉽게 말하자면, 'Kotlin에서 언어적으로 지원하는 거, 전부 모킹할 수 있다니까!' 비슷한 느낌?
* 기존 사용자 층도 나쁘지 않고... 깃허브 통계상 최근 사용하는 프로젝트들도 꽤 많이 늘어났더라.
* '한번 써볼까?' 싶었다.

### 테스트 준비

#### 테스트의 큰 틀 잡기

* 테스트에 필요한 프레임워크들도 전부 모았으니, Spring Boot 프로젝트를 어떻게 테스트할지 고민해보았다.
* 일단, 내가 생각하는 [Clean Architecture](../architecture_and_design_pattern/Clean_Architecture_by_Uncle_Bob.md) 분류에 따라 테스트 대상을 나누어 보았다.
  * **Infrastructure:** Database와 그 연결부인 Repository 정도를 분류할 수 있을 것이다.
  * **Adapters:** 아마도, Controller(혹은 Router와 Handler)가 여기에 속할 것이다.
  * **Use Cases:** Service에 해당하는 부분들이 여기에 들어갈 거고.
  * **Entity:** 정말 근본적인 Domain Entity들이 포함되겠지.
* 자, 그러면 각 부분에 맞는 테스트 방법을 고민해보자.
  * **Infrastructure:** 어플리케이션과 독립적인 계층이니, 굳이 테스트를 수행한다면 Redis등을 사용한 통합 테스트?
  * **Adapters:** `WebTestClient`등으로 Request/Response를, Service는 Mockk로 모킹해서 Kotest 테스트 진행.
  * **Use Cases:** Repository를 Mockk로 모킹한 뒤, Kotest 테스트 진행.
  * **Entity:** 이상적인 상황이라면 Data class 형태일테니 테스트가 필요없겠지만, 그게 아니라면 Kotest로 유닛 테스트.

#### 테스트 스타일 결정

* Use Cases에 해당하는 Service 클래스들은 'Given-When-Then' 패턴으로 테스트하기로 결정.

  * `주어진 상황이 있고 (Given)` / `무엇을 할 때에 (When)` / `이런. 결과가 나온다 (Then)`

  * 특정 행동에 따라, 특정 결과가 나올 것이 명확하게 구분되는 Service 로직의 테스트에 적합하다고 생각됐다.

    ```kotlin
    Given("저장된 작업이 1개 있는 상황에서") {
        every { taskRepository.getAllTasks() } returns singleTaskFlux.log()
        When("모든 작업 리스트를 요청하면") {
            val result = StepVerifier.create(service.getAllTasks())
    
            Then("결과 리스트를 조회해야 한다") {
                verify(exactly = 1) {
                    taskRepository.getAllTasks()
                }
            }
            Then("조회된 결과 리스트에 1개의 작업이 존재해야 한다") {
                result
                    .expectSubscription()
                    .expectNext(*singleGetAllTasksDataList.toTypedArray())
                    .expectComplete()
                    .verify()
            }
        }
    ```

* HTTP Request/Response를 직접 처리하는 Controller 클래스는, 'Describe-Context-It' 패턴으로 테스트하기로 결정.

  * `특정 API를 (Describe)` / `사용자가 어떠한 식으로 사용하면 (Context)` / `Controller는 이렇게 반응한다 (It)`

  * 사용자와 API의 상호 관계와, 그 동작 결과에 집중하는 특성은 Controller 로직의 테스트에 적합하다고 생각했다.

    ```kotlin
    describe("getAllTasks를") {
        val performRequest = { webTestClient.get().uri("/task").exchange() }
        context("저장된 데이터가 없는 상황에서 요청한 경우") {
            every { taskService.getAllTasks() } returns emptyGetAllTasksDataFlux.log()
            val response = performRequest()
    
            it("서비스를 통해 데이터를 조회한다") {
                verify(exactly = 1) {
                    taskService.getAllTasks()
                }
            }
            it("요청은 성공한다") {
                response.expectStatus().isOk
            }
            it("반환 형식은 JSON이다") {
                response.expectHeader().contentType(MediaType.APPLICATION_JSON)
            }
            it("JSON은 비어 있다") {
                response.expectBody().json(emptyTaskResponseBody)
            }
        }
    }
    ```

### 테스트 코드 작성

* 대상 프로젝트는 Clean Architecture를 일부 수용한 디렉토리 구조를 가지는, WebFlux 기반의 Monolithic 프로젝트.

* 프로젝트에서 본 TIL과 직접적으로 연관되어 있는 부분들만 정리하자면 아래와 같다.

  * **Task:** Data class로, Task Domain의 가장 기본적인 단위를 이루는 클래스.

    ```kotlin
    data class Task(
        val id: Long?,
        val name: String,
        val status: Status,
        val dueTime: OffsetDateTime?,
        val doneTime: OffsetDateTime?,
        val createdTime: OffsetDateTime?,
        val updatedTime: OffsetDateTime?,
    ) {
        enum class Status(val value: String) {
            TODO("TODO"),
            IN_PROGRESS("IN_PROGRESS"),
            DONE("DONE"),
            WONT_DO("WONT_DO"),
        }
    }
    ```

  * **TaskRepository:** Interface로, Task 데이터베이스와 상호작용을 위한 메소드들을 정의해놓은 인터페이스.

    ```kotlin
    interface TaskRepository {
        /* ... */
        fun getAllTasks(): Flux<Task>
        /* ... */
    }
    ```

  * **TaskService:** `@Service` 어노테이션으로 지정되어 있으며, Task와 관련된 내부 동작들을 구현해놓은 클래스.

    ```kotlin
    @Service
    class TaskService(@Qualifier("MockTaskRepository") private val repository: TaskRepository) {
        /* ... */
        fun getAllTasks(): Flux<GetAllTasksData> {
            return repository.getAllTasks()
                .map { GetAllTasksData.fromTask(it) }
        }
        /* ... */
    }
    ```

    * **GetAllTasksData:** `TaskService`의 `getAllTasks()` 메소드에 대한 DTO[^5].

      ```kotlin
      data class GetAllTasksData private constructor(
          val id: Long,
          val name: String,
      ) {
          companion object {
              fun fromTask(task: Task) = GetAllTasksData(
                  id = task.id!!,
                  name = task.name
              )
          }
      }
      ```

  * **TaskController:** `@RestController` 어노테이션으로 지정되어 있으며, Task와 관련된 HTTP Request/Response를 처리하는 클래스.

    ```kotlin
    @RestController
    @RequestMapping("/task")
    class TaskController(private val service: TaskService) {
        /* ... */
        @GetMapping("")
        fun getAllTasks(): Flux<GetAllTasksResponse> {
            return service.getAllTasks()
                .map { GetAllTasksResponse.fromData(it) }
        }
          /* ... */
    }
    ```

    * **GetAllTasksResponse:** Jackson을 통해 JSON으로 바뀌어 전달될, `TaskController`의 HTTP Response Body.

      ```kotlin
      data class GetAllTasksResponse private constructor(
          val id: Long,
          val name: String
      ) {
          companion object {
              fun fromData(data: GetAllTasksData) = GetAllTasksResponse(
                  id = data.id,
                  name = data.name
              )
          }
      }
      ```

#### 테스트 Helper 클래스 구현

* **TestUtils:** 전체 테스트 케이스에서 공용으로 쓰일 유틸리티 메소드들을 구현해놓은 오브젝트.

  ```kotlin
  object TestUtils {
      private val jsonMapper = jacksonObjectMapper()
  
      init {
          jsonMapper.propertyNamingStrategy = PropertyNamingStrategies.SnakeCaseStrategy()
          jsonMapper.registerModule(JavaTimeModule())
      }
  
      fun <T> listToFlux(list: List<T>): Flux<T> {
          return if (list.isNullOrEmpty()) Flux.empty<T>() else
              Flux.fromIterable(list.asIterable())
      }
  
      fun <T> listToJson(list: List<T>): String = jsonMapper.writeValueAsString(list)
  }
  ```

  * **listToFlux:** `List<T>`로 정의되어 있는 테스트 소스를, DTO에 해당하는 `Flux<T>`로 바꿔주는 유틸리티 메소드.
  * **listToJson:** `List<T>`로 정의되어 있는 테스트 소스를, HTTP Response Body에 해당하는 JSON String으로 바꿔주는 유틸리티 메소드.

* **TimeFactory:** `OffsetDateTime`을 모킹할 때, 원하는 값의 `OffsetDateTime` 객체를 생성시켜 주는 팩토리 오브젝트.

  ```kotlin
  object TimeFactory {
      data class TimeDelta(val unit: Unit, val value: Long = 1) {
          enum class Unit(val operator: KFunction2<OffsetDateTime, Long, OffsetDateTime>) {
              SECOND(OffsetDateTime::plusSeconds),
              MINUTE(OffsetDateTime::plusMinutes),
              HOUR(OffsetDateTime::plusHours),
              DAY(OffsetDateTime::plusDays),
              MONTH(OffsetDateTime::plusMonths),
              YEAR(OffsetDateTime::plusYears)
          }
      }
  
      private val mockedOffsetDateTime: OffsetDateTime =
          OffsetDateTime.parse(
              "2021-01-01T12:34:56.789Z",
              DateTimeFormatter.ISO_OFFSET_DATE_TIME
          )
  
      fun create(delta: TimeDelta? = null): OffsetDateTime {
          delta?.let {
              return it.unit.operator(mockedOffsetDateTime, it.value)
          }
          return mockedOffsetDateTime
      }
  }
  ```

* **TaskFactory:** `Task`를 모킹할 때, 원하는 값의 `Task` 객체를 생성시켜 주는 팩토리 오브젝트.

  ```kotlin
  object TaskFactory {
      private var counter: Long = 1
  
      private fun fetchAndIncrease(): Long {
          val current = counter
          counter += 1
          return current
      }
  
      fun create(type: Task.Status, id: Long = fetchAndIncrease()): Task {
          return when (type) {
              Task.Status.TODO -> Task(
                  id,
                  "Task$id",
                  type,
                  TimeFactory.create(TimeFactory.TimeDelta(TimeFactory.TimeDelta.Unit.HOUR, id)),
                  null,
                  TimeFactory.create(TimeFactory.TimeDelta(TimeFactory.TimeDelta.Unit.DAY, id.unaryMinus())),
                  TimeFactory.create(TimeFactory.TimeDelta(TimeFactory.TimeDelta.Unit.DAY, id.unaryMinus()))
              )
              Task.Status.DONE -> Task(
                  id,
                  "Task$id",
                  type,
                  TimeFactory.create(TimeFactory.TimeDelta(TimeFactory.TimeDelta.Unit.HOUR, id.unaryMinus())),
                  TimeFactory.create(),
                  TimeFactory.create(TimeFactory.TimeDelta(TimeFactory.TimeDelta.Unit.DAY, id.unaryMinus())),
                  TimeFactory.create()
              )
              Task.Status.IN_PROGRESS -> Task(
                  id,
                  "Task$id",
                  type,
                  TimeFactory.create(TimeFactory.TimeDelta(TimeFactory.TimeDelta.Unit.HOUR, id)),
                  null,
                  TimeFactory.create(TimeFactory.TimeDelta(TimeFactory.TimeDelta.Unit.DAY, id.unaryMinus())),
                  TimeFactory.create(TimeFactory.TimeDelta(TimeFactory.TimeDelta.Unit.DAY, id.unaryMinus()))
              )
              Task.Status.WONT_DO -> Task(
                  id,
                  "Task$id",
                  type,
                  TimeFactory.create(TimeFactory.TimeDelta(TimeFactory.TimeDelta.Unit.HOUR, id.unaryMinus())),
                  TimeFactory.create(),
                  TimeFactory.create(TimeFactory.TimeDelta(TimeFactory.TimeDelta.Unit.DAY, id.unaryMinus())),
                  TimeFactory.create()
              )
          }
      }
  }
  ```

#### Service 테스트 코드 구현

```kotlin
class TaskServiceTest : BehaviorSpec() {
    /* ... */
    private val manyTaskList = listOf(
        TaskFactory.create(Task.Status.IN_PROGRESS),
        TaskFactory.create(Task.Status.TODO),
        TaskFactory.create(Task.Status.WONT_DO),
        TaskFactory.create(Task.Status.DONE)
    )
    private val manyTaskFlux = TestUtils.listToFlux(manyTaskList)
    private val manyGetAllTasksDataList = manyTaskList.map { GetAllTasksData.fromTask(it) }

    init {
        val taskRepository = mockk<TaskRepository>()
        val service = TaskService(taskRepository)

        afterContainer {
            clearAllMocks()
        }

        /* ... */

        Given("저장된 작업이 N개 있는 상황에서") {
            every { taskRepository.getAllTasks() } returns manyTaskFlux.log()
            When("모든 작업 리스트를 요청하면") {
                val result = StepVerifier.create(service.getAllTasks())

                Then("결과 리스트를 조회해야 한다") {
                    verify(exactly = 1) {
                        taskRepository.getAllTasks()
                    }
                }
                Then("조회된 결과 리스트에 N개의 작업이 존재해야 한다") {
                    result
                        .expectSubscription()
                        .expectNext(*manyGetAllTasksDataList.toTypedArray())
                        .expectComplete()
                        .verify()
                }
            }
        }
    }
}
```

* 'Given-When-Then' 패턴을 사용하기로 했으므로, `TaskServiceTest` 클래스는 `BehaviorSpec`을 사용하게 된다.
* `TaskFactory`로 `Task`의 리스트를 만든 뒤, 이를 `Flux<Task>`로 변경하여, Repository의 `getAllTasks()`의 리턴 타입과 맞춘다. 
* `TaskRepository`를 Mockk로 모킹하여, `manyTaskFlux`를 테스트 소스 삼아 리턴하게끔 `getAllTasks()`를 Mock 함수로 만든다.
* Console에 로그를 남기면서 Flux의 스트림 처리 이벤트를 확인하고 싶었기에 이벤트 소스에 대해 `.log()` 메소드를 사용했다.
* DTO의 Flux를 반환하는 `service.getAllTasks()`에 대해 테스트 케이스를 작성하기 위해, `reactor.test`의 `StepVerifier`를 사용한다.
* `service.getAllTasks()`가 호출되고 나면, 당연히 그 Repository 메소드인 `taskRepository.getAllTask()`도 호출되어야 함으로 Verify.
* `StepVerifier`는 Flux 스트림의 각 이벤트에 대해 검증할 수 있다.
  * `expectSubscription()`: `onSubscribe` 이벤트를 검증하며, 당연히 제일 우선적으로 호출해야 한다. (`expectNoEvent`보다도!)
  * `expectNext()`: `onNext` 이벤트를 검증하며, 각 데이터의 값에 대해 검증할 수 있다.
    * 원시 데이터를 리스트로 만든 이유로, `vararg T`에 대한 오버로딩도 있기에 Spread Operator를 써서 쉽게 순차 검증을 할 수 있다.
  * `expectComplete()`: `onComplete` 이벤트를 검증하며, 스트림이 정상 종료되었음을 확인하기 위해 마지막에 호출해야 한다.
  * `verify()`: `StepVerifier`에 정의된 검증 케이스들을 모두 수행한다. (이 시점에 테스트 케이스의 성공/실패가 결정된다.)

#### Controller 테스트 코드 구현

```kotlin
class TaskControllerTest : DescribeSpec() {
    /* ... */
    private val manyTaskList = listOf(
        TaskFactory.create(Task.Status.IN_PROGRESS),
        TaskFactory.create(Task.Status.TODO),
        TaskFactory.create(Task.Status.WONT_DO),
        TaskFactory.create(Task.Status.DONE)
    )
    private val manyGetAllTasksDataFlux = TestUtils.listToFlux(
        manyTaskList.map { GetAllTasksData.fromTask(it) }
    )
    private val manyTaskResponseBody = TestUtils.listToJson(
        manyTaskList.map { GetAllTasksData.fromTask(it) }
    )

    init {
        val taskService = mockk<TaskService>()
        val webTestClient = WebTestClient
            .bindToController(TaskController(taskService))
            .build()

        afterContainer {
            clearAllMocks()
        }

        describe("getAllTasks를") {
            val performRequest = { webTestClient.get().uri("/task").exchange() }
            /* ... */
            context("저장된 데이터가 N개인 상황에서 요청한 경우") {
                every { taskService.getAllTasks() } returns manyGetAllTasksDataFlux.log()
                val response = performRequest()

                it("서비스를 통해 데이터를 조회한다") {
                    verify(exactly = 1) {
                        taskService.getAllTasks()
                    }
                }
                it("요청은 성공한다") {
                    response.expectStatus().isOk
                }
                it("반환 형식은 JSON이다") {
                    response.expectHeader().contentType(MediaType.APPLICATION_JSON)
                }
                it("JSON에는 데이터 N개 있다") {
                    response.expectBody().json(manyTaskResponseBody)
                }
            }
        }
    }
}
```

* `TaskServiceTest` 클래스와 달리, 'Describe-Context-It' 패턴을 사용했기 때문에 `DescribeSpec`을 지정했다.
* `TaskServiceTest`가 유닛 테스트의 개념에 가까웠다면, 여기서는 HTTP Request를 알맞게 처리하는지를 보는 기능 테스트의 단계라고 보자.
* 다른 계층을 Mockk로 모킹해주는 것은 똑같지만, 테스트 케이스의 최종 검증 범위는 HTTP Response 전체라는 점에서 조금 차이가 있다.
  * `expectStatus().isOk`를 통해 HTTP Status Code가 `200 OK`로 왔는지 검증한다.
  * `expectHeader().contentType(MediaType.APPLICATION_JSON)`을 통해 헤더의 `Content-Type: application/json`를 검증한다.
  * `expectBody().json()`을 통해 `listToJson`을 통해 JSON String으로 직렬화 된 데이터와 실제 결과값 검증을 수행한다.

### 시행착오들에 대한 내 개인적인 메모

#### clearAllMocks()에 대해

```kotlin
afterContainer {
    clearAllMocks()
}
```

* 위에 기술된 세 줄... 매우 중요하다!
* 풀어서 쓰면 아래와 같은 의미인데,
  * `afterContainer`: Describe나 Given 등, 각 컨테이너 블록이 끝나고 난 뒤에 실행되는 동작을 기술한다.
  * `clearAllMocks()`: 모킹된 모든 객체들을 다시 원상 복구한다.
* 그러니까... 얘 빼먹으면 이전에 이미 수행된 테스트 케이스의 Mock 객체들이 그대로 사용되어, 원치 않은 결과가 나올 수 있다.

#### Kotest / Mockk의 라이프 사이클에 대한 잡담

* 이렇게 작성해도 정상적으로 동작한다.

  ```kotlin
  class SimpleServiceTest : BehaviorSpec({
      val simpleRepository = mockk<simpleRepository>()
      /* ... */
      Given("Test Situation 1,") {
          /* ... */
      }
      /* ... */
  }) {
      companion object {
          val simpleEntity = Simple(/* ... */)
      }
  }
  ```

* 이렇게 작성해도 정상적으로 동작한다.

  ```kotlin
  class SimpleServiceTest : BehaviorSpec({
      val simpleEntity = Simple(/* ... */)
      init {
          val simpleRepository = mockk<simpleRepository>()
          /* ... */
          Given("Test Situation 1,") {
              /* ... */
          }
          /* ... */
      }
  }
  ```

* 무슨 말을 하고 싶은거냐면... Kotest / Mockk 테스트 코드는 테스트 클래스가 로드/초기화 되는 시점에 대해 작성되어야 한다.

---

[^1]:Test-Driven-Development (테스트 주도 개발), 별도 TIL로 작성 예정.
[^2]:Behaivor-Driven-Developement(행위 주도 개발), 사용자 입장에서의 행위를 기반으로 테스트 시나리오를 작성한다.
[^3]:Domain Specific Language, 특정 목적(도메인)을 위해 파생된 별도 언어로 라이브러리나 프레임워크의 사용성을 높여준다.
[^4]: '첫 인상이 평생을 간다'라고, 사실 이 점이 가장 마음에 들었다.
[^5]: Data Transfer Object, 계층간 데이터 교환을 하기 위해 명세를 제한한 데이터 객체 (Data Class).

---

##### References

* ["스프링에서 코틀린 스타일 테스트 코드 작성하기" by "우아한 형제들 기술 블로그"](https://techblog.woowahan.com/5825/)
* ["Comparing Testing Library for Kotlin" by "Veluexer"](https://veluxer62.github.io/explanation/comparing-testing-library-for-kotlin/)
* ["Kotest Introduction" by "Kotest"](https://kotest.io/docs/framework/framework.html)
* ["단위 테스트와 TDD(테스트 주도 개발) 프로그래밍 방법 소개" by "망나니개발자"](https://mangkyu.tistory.com/182)
* ["Mockk: Better way to mock in Kotlin than Mockito" by "Prashant Pol"](https://medium.com/@prashantspol/mockk-better-to-way-to-mock-in-kotlin-than-mockito-1b659c5232ec)
* ["Mocking is not rocket science" by "Oleksiy Pylypenko"](https://blog.kotlin-academy.com/mocking-is-not-rocket-science-basics-ae55d0aadf2b)
