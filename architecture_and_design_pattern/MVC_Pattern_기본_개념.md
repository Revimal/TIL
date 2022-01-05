### MVC Pattern 기본 개념

_2022/01/05_

### 개요

### MVC Pattern이란?

MVC (Model-View-Controller) Pattern은 사용자 인터페이스, 데이터 및 제어 로직 구성에 사용되는 아키텍처

실제 서비스에서 쓰이는 MVC 패턴은 조금 다른 걸[^1]로 알고 있지만, 가장 기본이 되는 개념을 정리해본다

![MVC-common-diagram](../img/architecture_and_design_pattern/MVC_common_diagram.png)

* 모델 (Model) : 데이터와 비즈니스 로직[^2] 을 관리
  * 어플리케이션이 무엇을 하는지를 정의하는 부분
  * 데이터를 저장하기 위해 DB와의 CRUD[^3] 상호 작용하거나 특정 요구사항을 충족 시키게 데이터를 가공
  * 데이터의 변경이 일어나면, 다른 구성요소에게 어떻게 통지할 것인지를 고민해야 함
  * 다른 구성 요소들에 대해서는 어떠한 정보도 알지 말아야 한다
  * 완성도 높은 모델 설계를 하기 위해서는, 그만큼 요구사항에 대한 깊은 이해가 필요
  * 책임과 역할을 기준으로 분리하여 개개의 모델 구성부는 작은 규모를 유지하는게 좋음
* 뷰 (View) : 외부로 보여지는 부분을 처리
  * 어플리케이션의 데이터를 외부로 표현해주는 역할을 수행
  * 데이터를 저장하지 않고, "무엇을 어떻게 보여줄 것인지"만을 다루는 부분
* 컨트롤러 (Controller) : 요청에 대해 반응하여 모델과 뷰를 업데이트
  * API이나 기타 인터페이스를 통해 사용자로부터 들어온 요청을 처리하는 역할
  * 원칙상으로 모델과 뷰는 서로의 정보를 몰라야 하기 때문에, 그 중간에서 데이터와 로직 흐름을 중개
  * 어플리케이션의 규모가 커질 수록, 필연적으로 모델과 뷰가 늘어나 컨트롤러가 비대해지는 Massive-View-Controller 문제가 발생
* 라우터 (Router) : MVC 기반의 웹 서비스에서 많이 쓰이는 추가 구성 요소로, 요청의 처리 경로를 결정하는 역할
  * `/api/image/39910245.png` 라는 요청이 들어왔을 때, `/image/` 부분을 보고 `ImageController`에 요청의 처리를 위임하는 식으로


### MVC Pattern의 예시

근처 맛집을 추천해주는 서비스를 예시로 들어보자 (MVC + Router)

1. 사용자는 API 서버로 자신의 현재 위치 정보를 담아서 `/api/place/recommend`라고 요청을 보낸다
2. 라우터가 요청을 받고, `/place/`를 보고 장소와 관련된 요청임을 파악해 관련 컨트롤러로 요청을 넘겨준다
3. 장소 컨트롤러가 바통을 넘겨 받고, `recommend`에 대해 처리할 수 있는 로직을 호출하게 된다
4. 위경도를 포함한 장소 데이터를 명세하는 모델을 통해, DB에서 데이터를 추출하고 특정 거리 이내의 장소 리스트를 구성한다
5. 해당 장소 리스트를 다시 별점 순으로 정렬해서 최종적으로 사용자에게 건네 줄 장소 리스트를 만든다
6. 장소 컨트롤러는 장소 리스트를 뷰로 전달한다



---

[^1]: Model에는 데이터의 명세만 담겨있고 Controller나 그에서 분리된 Service에서 실제 데이터 변경 로직이 존재한다던가
[^2]: 특정 요구사항을 충족하기 위해 데이터를 생성, 조회, 변경, 제거를 수행하는 부분을 의미함
[^3]: Create / Read / Update / Delete 4가지 기능을 엮어서 부른 단어로, 데이터의 관리를 위해 필요한 기본 요소

---

##### References

1. ["MVC" by "MDN Web Docs"](https://developer.mozilla.org/ko/docs/Glossary/MVC)
2. ["Model-view-controller" by "Wikipedia"](https://en.wikipedia.org/wiki/Model%E2%80%93view%E2%80%93controller)
3. ["MVC 패턴" by "ljinsk3"](https://velog.io/@ljinsk3/MVC-%ED%8C%A8%ED%84%B4)
4. ["MVC 패턴이란?" by "Clint Jang"](https://medium.com/@jang.wangsu/%EB%94%94%EC%9E%90%EC%9D%B8%ED%8C%A8%ED%84%B4-mvc-%ED%8C%A8%ED%84%B4%EC%9D%B4%EB%9E%80-1d74fac6e256)
5. ["MVC(Model, View, Controller) Pattern" by "Junhyunny"](https://junhyunny.github.io/information/design-pattern/mvc-pattern/)



---

##### Image Reference

1. [img/architecture_and_design_pattern/MVC_common_diagram.png](https://commons.wikimedia.org/wiki/File:Router-MVC-DB.svg)