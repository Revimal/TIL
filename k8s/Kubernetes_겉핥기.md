# Kubernetes 겉핥기

_2021/01/04_

### 개요

##### Kubernetes의 기본 개념

컨테이너 오케스트레이션[^1]을 제공하는 오픈소스 플랫폼

길게 적기 귀찮아서 k8s라고 칭하는 경우도 많음

##### Kubernetes의 구성

* 컨트롤 플레인 (Control plane)
  * 컨테이너 런타임[^2]이 존재하는 노드와 별개로, 컨테이너를 통합 관리하기 위한 API 및 인터페이스를 제공
  * 노드 혹은 그 내부에서 사용되는 모든 API 요청은 컨트롤 플레인으로 모이게 됨 [^3]
  * kube-apiserver가 노드와의 모든 API 통신을 중계하는 역할을 담당
  * kube-controller-manager가 오브젝트 명세를 보고 상태를 관리하는 역할을 전담
  * kube-scheduler가 워커 노드에 생성된 팟을 적절하게 분배해주는 역할을 수행
  * etcd라는 key-value 저장소가 API 서버와 직결되어 클러스터 전체의 공용 데이터[^4]를 저장하게 됨
  * 이 친구는 SPOF[^5]이기 때문에, 다중화 하는 경우가 많음
* 노드 (Node)
  * 컨테이너를 실제로 실행시키는 컨테이너 런타임이 가장 중요한 역할을 담당
  * kubelet이 컨트롤 플레인과 정보를 주고 받는 에이전트 역할을 수행
* 네임스페이스 (Namespace)
  * k8s 클러스터를 다시 논리적으로 분리하기 위한 방식
  * 예시로, 한 클러스터를 '개발 네임스페이스 / 테스트 네임스페이스' 와 같이 나눌 수 있음
  * '배포 네임스페이스'는 따로 분리함이 좋은게, 네임스페이스는 물리적 격리 단위가 아니기 때문
  * 네임스페이스 정책으로 네트워크 자원을 제한할 수는 있지만, 그래도 클러스터 자체 분리를 추천
* 오브젝트 (Object)
  * yaml 파일을 기본으로 기술되는, '현재 상태'와 '목표 상태'가 기술된 명세서라고 보면 편함
  * 팟 (Pod)
    * 여러 애플리케이션을 엮어 하나의 목적을 가지고 구성된 컨테이너 집합
    * 단일 팟 내의 컨테이너들은 서로 자원을 공유함 (IP도 공유되어 서로 localhost 접근 가능)
    * 팟이 죽으면 데이터도 날아가니까, 필요에 따라 볼륨과 같은 비휘발성 스토리지도 사용해야 함
    * k8s에서 배포 가능한 최소 단위 (단일 컨테이너도 팟으로 관리)
  * 레플리카셋 (ReplicaSet)
    * 한 개 이상의 동일한 팟을 통합 관리하기 위한 컨트롤러
    * 팟의 개수를 지정하여, 해당 개수만큼 복제된 팟이 실행될 수 있게끔 유지
  * 데몬셋 (DaemonSet)
    * 어떤 팟이 한 노드에서 한 개만 돌 수 있게 관리해주는 컨트롤러
  * 디플로이먼트 (Deployment)
    * 내부적으로 레플리카셋을 사용하는 컨트롤러로, 레플리카셋에 버전 관리 등과 관련된 추가 기능들이 더해짐

##### 팟이 생성되는 과정 (w/레플리카셋)

1. kubectl 혹은 다른 방식으로 외부에서 레플리카셋 명세가 kube-apiserver로 전달
2. kube-apiserver는 etcd에 레플리카셋을 저장
3. kube-controller-manager는 레플리카 셋의 조건을 만족하는 팟이 있는지 확인
4. 팟이 없다면, 레플리카 셋의 팟 템플릿을 보고 kube-apiserver에 팟 생성 요청
5. kube-scheduler는 생성된 팟을 어떤 노드에 설정할 것인지 결정하고 다시 kube-apiserver에 팟 할당 요청
6. kubelet은 할당된 팟을 컨테이너 런타임을 통해 실제로 생성
7. kubelet은 생성된 팟들의 상태를 계속해서 kube-apiserver로 전달

##### Kubernetes의 사상

* 순차적으로 작업을 처리하는 파이프라인 도구 보다는, 상태에 기반한 독립적인 제어 도구에 가까움
* 팟의 갯수나 네트워크 토폴로지와 같은 '조건'을 설정하면, k8s가 해당 조건을 맞추기 위한 동작을 자동 수행

### 직접 해보기

##### Kubernetes 우분투에서 써보기

_k8s 클러스터 구성에는 최소 2개의 장비가 필요하지만, 집에 데스크탑 1대 밖에 없어서 microk8s를 설치_

1. sudo snap install microk8s --classic
2. microk8s status --wait-ready
3. microk8s enable dashboard dns registry istio
4. microk8s inspect
5. microk8s kubectl get all --all-namespaces
6. microk8s kubectl get svc/kubernetes-dashboard -n kube-system

##### nginx를 팟 형태로 생성하기

1. nginx 팟을 생성하기 위한 yaml 파일 작성

   ```yaml
   apiVersion: v1
   kind: Pod
   metadata:
     name: nginx-pod
     labels:
       environment: sandbox
       app: nginx
   spec:
     containers:
       - name: nginx
         image: nginx:1.14.2
         ports:
         - containerPort: 80
   ```

2. 작성한 yaml 파일을 kubectl을 통해 적용

   ```bash
   hhseo@hhseo-desktop:~$ microk8s kubectl apply -f ~/TIL/src/k8s/sandbox-nginx-pod.yml 
   pod/nginx-pod created
   ```

3. 생성된 팟을 kubectl을 통해 확인

   ```bash
   hhseo@hhseo-desktop:~$ microk8s kubectl get pod -o wide
   NAME        READY   STATUS    RESTARTS   AGE    IP            NODE            NOMINATED NODE   READINESS GATES
   nginx-pod   1/1     Running   0          3m2s   10.1.241.21   hhseo-desktop   <none>           <none>
   ```

4. 팟의 IP로 접속이 되는지 검증

   ```bash
   hhseo@hhseo-desktop:~$ curl 10.1.241.21
   <!DOCTYPE html>
   <html>
   <head>
   <title>Welcome to nginx!</title>
   <style>
       body {
           width: 35em;
           margin: 0 auto;
           font-family: Tahoma, Verdana, Arial, sans-serif;
       }
   </style>
   </head>
   <body>
   <h1>Welcome to nginx!</h1>
   <p>If you see this page, the nginx web server is successfully installed and
   working. Further configuration is required.</p>
   
   <p>For online documentation and support please refer to
   <a href="http://nginx.org/">nginx.org</a>.<br/>
   Commercial support is available at
   <a href="http://nginx.com/">nginx.com</a>.</p>
   
   <p><em>Thank you for using nginx.</em></p>
   </body>
   </html>
   ```

5. Apache Jmeter를 이용하여 다량의 HTTP GET Request를 보내어 팟에 의도적으로 부하를 줌

   ```bash
   hhseo@hhseo-desktop:~/TIL/src/k8s$ echo "ip:$(microk8s kubectl get pod nginx-pod -o yaml|grep "podIP:"|tail -n 1|awk -F ': ' '{print $2}')" > sandbox-nginx-pod-stress.csv
   hhseo@hhseo-desktop:~/TIL/src/k8s$ cat sandbox-nginx-pod-stress.csv
   ip:10.1.241.21
   hhseo@hhseo-desktop:~/TIL/src/k8s$ jmeter -n -t sandbox-nginx-pod-stress.jmx -l sandbox-nginx-pod-stress.jtl
   Creating summariser <summary>
   Created the tree successfully using sandbox-nginx-pod-stress.jmx
   Starting the test @ Tue Jan 04 03:22:57 KST 2022 (1641234177751)
   Waiting for possible shutdown message on port 4445
   summary +  24062 in   2.2s = 10877.9/s Avg:     0 Min:     0 Max:    30 Err:     0 (0.00%) Active: 3 Started: 3 Finished: 0
   ```
   
6. 팟에 실제로 부하가 걸리고 있는지 확인

   ```bash
   hhseo@hhseo-desktop:~$ while true; do microk8s kubectl top pod nginx-pod; sleep 1; done
   NAME        CPU(cores)   MEMORY(bytes)   
   nginx-pod   0m           2Mi           
   NAME        CPU(cores)   MEMORY(bytes)   
   nginx-pod   0m           2Mi      
   NAME        CPU(cores)   MEMORY(bytes)   
   nginx-pod   165m         3Mi           
   ```

7. 이제는 필요 없어진 팟을 제거

   ```bash
   hhseo@hhseo-desktop:~$ microk8s kubectl delete pod nginx-pod 
   pod "nginx-pod" deleted
   hhseo@hhseo-desktop:~$ microk8s kubectl get pod
   No resources found in default namespace.
   ```

##### nginx를 디플로이먼트 형태로 생성하기

​	_레플리카셋을 직접 사용하는 경우는 거의 없다고 해서, 레플리카셋 데모를 건너뛰고 바로 디플로이먼트로 넘어옴_

1. nginx 디플로이먼트가 명세되어 있는 yaml 파일을 생성

   ```yaml
   apiVersion: apps/v1
   kind: Deployment
   metadata:
     name: nginx-deployment
   spec:
     selector:
       matchLabels:
         environment: sandbox
         app: nginx
     replicas: 3
     template:
       metadata:
         labels:
           environment: sandbox
           app: nginx
       spec:
         containers:
           - name: nginx
             image: nginx:1.14.2
             ports:
             - containerPort: 80
   ```

2. 작성한 파일을 적용하고 k8s의 상태 확인

   ``` bash
   스크린샷, 2022-01-05 00-16-20hhseo@hhseo-desktop:~$ microk8s kubectl get deployment -o wide
   NAME               READY   UP-TO-DATE   AVAILABLE   AGE    CONTAINERS   IMAGES         SELECTOR
   nginx-deployment   3/3     3            3           3m7s   nginx        nginx:1.14.2   app=nginx,environment=sandbox
   hhseo@hhseo-desktop:~$ microk8s kubectl get replicaset -o wide
   NAME                       DESIRED   CURRENT   READY   AGE    CONTAINERS   IMAGES         SELECTOR
   nginx-deployment-d4c97fc   3         3         3       3m9s   nginx        nginx:1.14.2   app=nginx,environment=sandbox,pod-template-hash=d4c97fc
   hhseo@hhseo-desktop:~$ microk8s kubectl get pod -o wide
   NAME                             READY   STATUS    RESTARTS   AGE     IP            NODE            NOMINATED NODE   READINESS GATES
   nginx-deployment-d4c97fc-rxl89   1/1     Running   0          3m10s   10.1.241.23   hhseo-desktop   <none>           <none>
   nginx-deployment-d4c97fc-8z759   1/1     Running   0          3m10s   10.1.241.24   hhseo-desktop   <none>           <none>
   nginx-deployment-d4c97fc-4g5fb   1/1     Running   0          65s     10.1.241.25   hhseo-desktop   <none>           <none>
   ```

3. 디플로이먼트를 통해 생겨난 pod 중에 하나를 강제로 죽였을 때 다시 복구 되는지 확인

   ```bash
   hhseo@hhseo-desktop:~$ microk8s kubectl delete pod nginx-deployment-d4c97fc-rxl89; microk8s kubectl get deployment -o wide; microk8s kubectl get replicaset -o wide; microk8s kubectl get pod -o wide
   pod "nginx-deployment-d4c97fc-rxl89" deleted
   NAME               READY   UP-TO-DATE   AVAILABLE   AGE    CONTAINERS   IMAGES         SELECTOR
   nginx-deployment   2/3     3            2           4m7s   nginx        nginx:1.14.2   app=nginx,environment=sandbox
   NAME                       DESIRED   CURRENT   READY   AGE    CONTAINERS   IMAGES         SELECTOR
   nginx-deployment-d4c97fc   3         3         2       4m8s   nginx        nginx:1.14.2   app=nginx,environment=sandbox,pod-template-hash=d4c97fc
   NAME                             READY   STATUS              RESTARTS   AGE    IP            NODE            NOMINATED NODE   READINESS GATES
   nginx-deployment-d4c97fc-8z759   1/1     Running             0          4m8s   10.1.241.24   hhseo-desktop   <none>           <none>
   nginx-deployment-d4c97fc-4g5fb   1/1     Running             0          2m3s   10.1.241.25   hhseo-desktop   <none>           <none>
   nginx-deployment-d4c97fc-xz2jc   0/1     ContainerCreating   0          2s     <none>        hhseo-desktop   <none>           <none>
   ```

4. 디플로이먼트의 팟 복제본을 2개로 줄여보기

   ```bash
   hhseo@hhseo-desktop:~$ microk8s kubectl scale deployment --replicas=2 nginx-deployment
   deployment.apps/nginx-deployment scaled
   hhseo@hhseo-desktop:~$ microk8s kubectl get deployment -o wide
   NAME               READY   UP-TO-DATE   AVAILABLE   AGE    CONTAINERS   IMAGES         SELECTOR
   nginx-deployment   2/2     2            2           8m5s   nginx        nginx:1.14.2   app=nginx,environment=sandbox
   hhseo@hhseo-desktop:~$ microk8s kubectl get replicaset -o wide
   NAME                       DESIRED   CURRENT   READY   AGE    CONTAINERS   IMAGES         SELECTOR
   nginx-deployment-d4c97fc   2         2         2       8m8s   nginx        nginx:1.14.2   app=nginx,environment=sandbox,pod-template-hash=d4c97fc
   hhseo@hhseo-desktop:~$ microk8s kubectl get pod -o wide
   NAME                             READY   STATUS    RESTARTS   AGE     IP            NODE            NOMINATED NODE   READINESS GATES
   nginx-deployment-d4c97fc-8z759   1/1     Running   0          8m12s   10.1.241.24   hhseo-desktop   <none>           <none>
   nginx-deployment-d4c97fc-4g5fb   1/1     Running   0          6m7s    10.1.241.25   hhseo-desktop   <none>           <none>
   ```

5. 디플로이먼트의 상세 정보에서 스케일 인/아웃 히스토리 확인하기

   ```bash
   hhseo@hhseo-desktop:~$ microk8s kubectl describe deployment nginx-deployment
   Name:                   nginx-deployment
   Namespace:              default
   CreationTimestamp:      Tue, 04 Jan 2022 22:35:02 +0900
   Labels:                 <none>
   Annotations:            deployment.kubernetes.io/revision: 1
   Selector:               app=nginx,environment=sandbox
   Replicas:               2 desired | 2 updated | 2 total | 2 available | 0 unavailable
   StrategyType:           RollingUpdate
   MinReadySeconds:        0
   RollingUpdateStrategy:  25% max unavailable, 25% max surge
   Pod Template:
     Labels:  app=nginx
              environment=sandbox
     Containers:
      nginx:
       Image:        nginx:1.14.2
       Port:         80/TCP
       Host Port:    0/TCP
       Environment:  <none>
       Mounts:       <none>
     Volumes:        <none>
   Conditions:
     Type           Status  Reason
     ----           ------  ------
     Progressing    True    NewReplicaSetAvailable
     Available      True    MinimumReplicasAvailable
   OldReplicaSets:  <none>
   NewReplicaSet:   nginx-deployment-d4c97fc (2/2 replicas created)
   Events:
     Type    Reason             Age   From                   Message
     ----    ------             ----  ----                   -------
     Normal  ScalingReplicaSet  9m3s  deployment-controller  Scaled up replica set nginx-deployment-d4c97fc to 3
     Normal  ScalingReplicaSet  65s   deployment-controller  Scaled down replica set nginx-deployment-d4c97fc to 2
   ```

6. 이제 디플로이먼트도 할 일 다 했으니 제거

   ```bash
   hhseo@hhseo-desktop:~$ microk8s kubectl delete deployment nginx-deployment
   deployment.apps "nginx-deployment" deleted
   hhseo@hhseo-desktop:~$ microk8s kubectl get deployment -o wide
   No resources found in default namespace.
   hhseo@hhseo-desktop:~$ microk8s kubectl get replicaset -o wide
   No resources found in default namespace.
   hhseo@hhseo-desktop:~$ microk8s kubectl get pod -o wide
   No resources found in default namespace.
   ```

---

[^1]: 여러 노드에 존재하는 컨테이너들을 통합 관리 및 자동화 한다고 이해하고 넘어가자
[^2]: 컨테이너를 실행시킬 수 있는 소프트웨어로, 대표적으로 containerd나 Docker 등이 존재 
[^3]: 물류 처리에서 Hub-And-Spoke 방식이라고 부르는 형상을 그려보면 보면 이해하기 편함
[^4]: 모든 설정과 상태가 저장되기에 이 친구만 백업하면 클러스터 복구 가능
[^5]: Single Point Of Failure, 그러니까 '얘 죽으면 전부 다 죽어요'라는 의미

---

##### References

1. [쿠버네티스 공식 문서](https://kubernetes.io/ko/docs/concepts/)
2. ['쿠버네티스 시작하기' by 'subicura'](https://subicura.com/2019/05/19/kubernetes-basic-1.html)
3. ['쿠버네티스, 이것만 알면 된다!' by 'Tech. KT Cloud'](https://tech.ktcloud.com/m/67)
4. ['Kubernetes의 이해-개념정리' by 'preiner'](https://preiner.medium.com/kubernetes%EC%9D%98-%EC%9D%B4%ED%95%B4-%EA%B0%9C%EB%85%90%EC%A0%95%EB%A6%AC-17245a0d5f4d)
5. ['Deployment로 애플리케이션 배포' by 'huisam'](https://huisam.tistory.com/entry/k8s-deployment)
6. ['쿠버네티스의 모든것' by '미니대왕님'](https://tommypagy.tistory.com/190)
7. ['Kubernetes' by '탕탕탕구리'](https://real-dongsoo7.tistory.com/135)