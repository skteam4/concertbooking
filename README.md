# concert

# Table of contents

- [콘서트 예매 시스템](#---)
  - [서비스 시나리오](#서비스-시나리오)
  - [체크포인트](#체크포인트)
  - [분석/설계](#분석설계)
  - [구현:](#구현-)
    - [DDD 의 적용](#ddd-의-적용)
    - [Gateway 적용](#Gateway-적용)
    - [CQRS](#CQRS)
    - [폴리글랏 퍼시스턴스](#폴리글랏-퍼시스턴스)
    - [동기식 호출과 Fallback 처리](#동기식-호출과-Fallback-처리)
  - [운영](#운영)
    - [Deploy/Pipeline](#Deploy/Pipeline)
    - [Config Map](#Config-Map)
    - [Persistence Volume](#Persistence-Volume)
    - [Autoscale(HPA)](#Autoscale(HPA))
    - [Circuit Breaker](#Circuit-Breaker)
    - [Zero-Downtime deploy(Readiness Probe)](#Zero-Downtime-deploy(Readiness-Probe))
    - [Self-healing(Liveness Probe)](#Self-healing(Liveness-Probe))

# 서비스 시나리오

# 기능적 요구사항
  1.  콘서트 관리자는 콘서트 정보를 등록한다.
  2.  콘서트 관리자는 티켓을 등록한다.
  3.  고객은 콘서트를 예매한다.
  4.  고객은 콘서트 예매를 취소할 수 있다.
  5.  콘서트 좌석을 초과하여 예매할 수 없다.
  6.  고객이 예매를 하면, 예매가능한 티켓 수량이 감소한다.
  7.  고객이 콘서트 현장에서 티켓 발급 또한, 우편 수령 후 티켓 발급 완료 처리가 된다.
  8.  고객은 예매정보를 확인 할 수 있다.
  9.  결제가 완료되면 티켓을 배송한다.
  10. 결제가 취소되면, 배달이 취소된다.
  11. 고객에게 알림 메시지가 간다.

# 비기능적 요구사항
1. 트랜잭션
    1. 예매 수량은 좌석 수량을 초과하여 예약 할 수 없다. (Sync 호출)
1. 장애격리
    1. 배송 시스템이 수행되지 않더라도 예매 기능은 365일 24시간 받을 수 있어야 한다. Async (event-driven), Eventual Consistency
    1. 예약시스템이 과중 되면 사용자를 잠시동안 받지 않고 예약을 잠시후에 하도록 유도한다. Circuit breaker, fallback
1. 성능
    1. 고객은 MyPage에서 본인 예매 상태를 확인 할 수 있어야 한다. (CQRS)

# 체크포인트

- 분석 설계
  - 이벤트스토밍: 
    - 스티커 색상별 객체의미를 제대로 이해하여 헥사고날 아키텍처의 연계설계에 반영하고 있는가?
    - 각 도메인 이벤트가 의미있게 정의되었는가?
    - 어그리게잇: Command와 Event를 ACID 트랜잭션 단위의 Aggregate로 제대로 묶었는가?
    - 기능요구사항과 비기능요구사항을 누락 없이 반영하였는가?    
  - 서브 도메인, 바운디드 컨텍스트 분리
    - 항목 
      - 팀별 KPI 와 관심사, 상이한 배포주기 등에 따른  
      - Sub-domain이나 Bounded Context를 분리하였고 그 분리기준이 합리적인가?
      - 3개 이상 서비스 분리
    - 폴리글랏 설계: 
      - 각 마이크로 서비스들의 구현목표와 기능특성에 따른 각자의 기술 Stack과 저장소 구조를 다양하게 채택하여 설계하였는가?
    - 서비스 시나리오 중 ACID 트랜잭션이 크리티컬한 Use 케이스에 대하여 무리하게 서비스가 과다하게 조밀히 분리되지 않았는가?
  - 컨텍스트 매핑 / 이벤트 드리븐 아키텍처 
    - 업무 중요성과 도메인간 서열을 구분할 수 있는가? (Core, Supporting, General Domain)
    - Request-Response 방식과 이벤트 드리븐 방식을 구분하여 설계할 수 있는가?
    - 장애격리: 서포팅 서비스를 제거 하여도 기존 서비스에 영향이 없도록 설계하였는가?
    - 신규 서비스를 추가 하였을때 기존 서비스의 데이터베이스에 영향이 없도록 설계(열려있는 아키택처)할 수 있는가?
    - 이벤트와 폴리시를 연결하기 위한 Correlation-key 연결을 제대로 설계하였는가?
  - 헥사고날 아키텍처
    - 설계 결과에 따른 헥사고날 아키텍처 다이어그램을 제대로 그렸는가?
    
- 구현
  - [DDD] 분석단계에서의 스티커별 색상과 헥사고날 아키텍처에 따라 구현체가 매핑되게 개발되었는가?
    - Entity Pattern과 Repository Pattern을 적용하여 JPA를 사용한 데이터 접근 어댑터를 개발하였는가?
    - [헥사고날 아키텍처] 
      - REST Inbound adaptor 이외에 gRPC 등의 Inbound Adaptor 를 추가함에 있어서 
      - 도메인 모델의 손상을 주지 않고 새로운 프로토콜에 기존 구현체를 적응시킬 수 있는가?
    - 분석단계에서의 유비쿼터스 랭귀지(업무현장에서 쓰는 용어)를 사용하여 소스코드가 서술되었는가?
  - Request-Response 방식의 서비스 중심 아키텍처 구현
    - 마이크로 서비스간 Request-Response 호출에 있어 대상 서비스를 어떠한 방식으로 찾아서 호출 하였는가? 
      - Service Discovery, REST, FeignClient
    - 서킷브레이커를 통하여  장애를 격리시킬 수 있는가?
  - 이벤트 드리븐 아키텍처의 구현
    - 카프카를 이용하여 PubSub 으로 하나 이상의 서비스가 연동되었는가?
    - Correlation-key
      - 각 이벤트 건 (메시지)가 어떠한 폴리시를 처리할때 어떤 건에 연결된 처리건인지를 구별하기 위한 
      - Correlation-key 연결을 제대로 구현 하였는가?
    - Message Consumer 마이크로서비스가 장애상황에서 수신받지 못했던 기존 이벤트들을 다시 수신받아 처리하는가?
    - Scaling-out
      - Message Consumer 마이크로서비스의 Replica 를 추가했을때 중복없이 이벤트를 수신할 수 있는가
    - CQRS
      - Materialized View 를 구현하여
      - 타 마이크로서비스의 데이터 원본에 접근없이(Composite 서비스나 조인SQL 등 없이)도 
      - 내 서비스의 화면 구성과 잦은 조회가 가능한가?
  - 폴리글랏 플로그래밍
    - 각 마이크로 서비스들이 하나이상의 각자의 기술 Stack 으로 구성되었는가?
    - 각 마이크로 서비스들이 각자의 저장소 구조를 자율적으로 채택하고 각자의 저장소 유형을 선택하여 구현하였는가?
      - RDB, NoSQL, File System 등
  - API 게이트웨이
    - API GW를 통하여 마이크로 서비스들의 집입점을 통일할 수 있는가?
    - 게이트웨이와 인증서버(OAuth), JWT 토큰 인증을 통하여 마이크로서비스들을 보호할 수 있는가?
- 운영
  - SLA 준수
    - 셀프힐링
      - Liveness Probe 를 통하여 어떠한 서비스의 health 상태가 지속적으로 저하됨에 따라 
      - 어떠한 임계치에서 pod 가 재생되는 것을 증명할 수 있는가?
    - 서킷브레이커, 레이트리밋 등을 통한 장애격리와 성능효율을 높힐 수 있는가?
    - 오토스케일러 (HPA) 를 설정하여 확장적 운영이 가능한가?
    - 모니터링, 앨럿팅: 
  - 무정지 운영 CI/CD (10)
    - Readiness Probe 의 설정과 Rolling update을 통하여 
      - 신규 버전이 완전히 서비스를 받을 수 있는 상태일때 신규버전의 서비스로 전환됨을 siege 등으로 증명 
    - Contract Test :  자동화된 경계 테스트를 통하여 구현 오류나 API 계약위반를 미리 차단 가능한가?




# 분석/설계
## Event Storming 결과
* MSAEz 로 모델링한 이벤트스토밍 결과:  http://www.msaez.io/#/storming/qLV82i54nWb4vM37CFUde5Jdpew2/mine/42d737bdb5439088560e23bbc2d7e5e7

### 이벤트 도출
![image](https://user-images.githubusercontent.com/85874443/122223814-8000b380-ceee-11eb-93a3-3c69d431226b.png)

### 부적격 이벤트 제거
![image](https://user-images.githubusercontent.com/85874443/122223889-8ee76600-ceee-11eb-9cd0-970c00b43cef.png)

### 액터/커맨드 부착
![image](https://user-images.githubusercontent.com/85874443/122223935-97d83780-ceee-11eb-9e5c-7b3cb1b13978.png)

### 어그리게잇으로 묶기
![image](https://user-images.githubusercontent.com/85874443/122223970-9f97dc00-ceee-11eb-8cd1-16ac65057044.png)

### 바운디드 컨텍스트로 묶기
![image](https://user-images.githubusercontent.com/85874443/122224015-a9b9da80-ceee-11eb-8118-9b10af5d378f.png)

### 이벤트스토밍 최종 결과
![image](https://user-images.githubusercontent.com/85874443/122227160-9ceab600-cef1-11eb-9dee-52b2f63dd9ac.png)

### 시나리오 요구사항 check
<img width="100%" height="100%" alt="hex" src="https://user-images.githubusercontent.com/85874443/122320354-0fdd4680-cf5d-11eb-9ab2-1bfcf9ac89e9.PNG">

## 헥사고날 아키텍처 다이어그램 도출
<img width="100%" height="100%" alt="hex" src="https://user-images.githubusercontent.com/85874443/122163362-0944c580-ceb0-11eb-9316-3280f171995f.PNG">




# 구현:

분석/설계 단계에서 도출된 헥사고날 아키텍처에 따라, 각 BC별로 대변되는 마이크로 서비스들을 스프링부트와 파이선으로 구현하였다. 구현한 각 서비스를 로컬에서 실행하는 방법은 아래와 같다 (각자의 포트넘버는 8081 ~ 808n 이다)

```
cd view
mvn spring-boot:run

cd alarm
mvn spring-boot:run

cd booking
mvn spring-boot:run 

cd concert
mvn spring-boot:run  

cd delivery
mvn spring-boot:run  

cd payment
mvn spring-boot:run  
```

## DDD 의 적용
- 이벤트 스토밍을 통해 도출된 Micro Service 는 총 6개이나, 3개만 구현하였으며 그 중 View는 CQRS를 위한 서비스이다.

|MSA|기능|port|URL|
| :--: | :--: | :--: | :--: |
|concert| 티켓정보 관리 |8081|http://localhost:8081/concerts|
|booking| 티켓예매 관리 |8082|http://localhost:8082/bookings|
|view| 콘서트 예매내역 조회 |8086|http://localhost:8086/views|


- 적용 후 REST API 의 테스트
```
# concert 서비스의 티켓등록
http localhost:8081/concerts item="통닭"

# booking 서비스의 예매
http localhost:8082/bookings orderId=1

# view 티켓예매 상태 확인
http localhost:8086/views/1

```


## Gateway 적용
API GateWay를 통하여 마이크로 서비스들의 진입점을 통일할 수 있다. 
다음과 같이 GateWay를 적용하였다.

```yaml
server:
  port: 8088

---

spring:
  profiles: default
  cloud:
    gateway:
      routes:
        - id: Concert
          uri: http://localhost:8081
          predicates:
            - Path=/concerts/**, /check/**  
        - id: Booking
          uri: http://localhost:8082
          predicates:
            - Path=/bookings/** 
        - id: Alarm
          uri: http://localhost:8083
          predicates:
            - Path=/alarms/** 
        - id: Delivery
          uri: http://localhost:8084
          predicates:
            - Path=/deliveries/** 
        - id: Payment
          uri: http://localhost:8085
          predicates:
            - Path=/payments/** 
        - id: View
          uri: http://localhost:8086
          predicates:
            - Path= /mypages/**
      globalcors:
        corsConfigurations:
          '[/**]':
            allowedOrigins:
              - "*"
            allowedMethods:
              - "*"
            allowedHeaders:
              - "*"
            allowCredentials: true


---

spring:
  profiles: docker
  cloud:
    gateway:
      routes:
        - id: Concert
          uri: http://Concert:8080
          predicates:
            - Path=/concerts/**, /check/**  
        - id: Booking
          uri: http://Booking:8080
          predicates:
            - Path=/bookings/** 
        - id: Alarm
          uri: http://Alarm:8080
          predicates:
            - Path=/alarms/** 
        - id: Delivery
          uri: http://Delivery:8080
          predicates:
            - Path=/deliveries/** 
        - id: Payment
          uri: http://Payment:8080
          predicates:
            - Path=/payments/** 
        - id: View
          uri: http://View:8080
          predicates:
            - Path= /mypages/**
      globalcors:
        corsConfigurations:
          '[/**]':
            allowedOrigins:
              - "*"
            allowedMethods:
              - "*"
            allowedHeaders:
              - "*"
            allowCredentials: true

server:
  port: 8080
```  
view 서비스의 GateWay 적용


![image](https://user-images.githubusercontent.com/82795860/120988904-f0eeef80-c7b9-11eb-92e3-ed97ecc2b047.png)

## CQRS
Materialized View 를 구현하여, 타 마이크로서비스의 데이터 원본에 접근없이(Composite 서비스나 조인SQL 등 없이) 도 내 서비스의 화면 구성과 잦은 조회가 가능하게 구현해 두었다.
본 프로젝트에서 View 역할은 view 서비스가 수행한다.

예약(Booked) 실행 후 view 화면
 
![image](https://user-images.githubusercontent.com/82795860/121005958-526b8a00-c7cb-11eb-9bae-ad4bd70ef2eb.png)



![image](https://user-images.githubusercontent.com/82795860/121006311-bb530200-c7cb-11eb-9d85-a7b22d1a2729.png)
  
## 폴리글랏 퍼시스턴스
mypage 서비스의 DB와 Booking/injection/vaccine 서비스의 DB를 다른 DB를 사용하여 MSA간 서로 다른 종류의 DB간에도 문제 없이 동작하여 다형성을 만족하는지 확인하였다.
(폴리글랏을 만족)

|서비스|DB|pom.xml|
| :--: | :--: | :--: |
|concert| H2 |![image](https://user-images.githubusercontent.com/2360083/121104579-4f10e680-c83d-11eb-8cf3-002c3d7ff8dc.png)|
|booking| H2 |![image](https://user-images.githubusercontent.com/2360083/121104579-4f10e680-c83d-11eb-8cf3-002c3d7ff8dc.png)|
|view| HSQL |![image](https://user-images.githubusercontent.com/2360083/120982836-1842be00-c7b4-11eb-91de-ab01170133fd.png)|

<!-- 
		<dependency>
			<groupId>com.h2database</groupId>
			<artifactId>h2</artifactId>
			<scope>runtime</scope>
		</dependency>
 -->
		<dependency>
		    <groupId>org.hsqldb</groupId>
		    <artifactId>hsqldb</artifactId>
		    <version>2.4.0</version>
		    <scope>runtime</scope>
		</dependency>



## 동기식 호출과 Fallback 처리
분석단계에서의 조건 중 하나로  콘서트 티켓 예약수량은 등록된 티켓 수량을 초과 할 수 없으며
예약(Booking)->콘서트(Concert) 간의 호출은 동기식 일관성을 유지하는 트랜잭션으로 처리하기로 하였다. 
호출 프로토콜은 Rest Repository 에 의해 노출되어있는 REST 서비스를 FeignClient 를 이용하여 호출하도록 한다.



Booking  내 external.ConcertService

```java
package concertbooking.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;


@FeignClient(name="Concert", url="http://localhost:8081")
public interface ConcertService {

    @RequestMapping(method= RequestMethod.GET, path="/checkAndBookStock")
    public boolean checkAndBookStock(@RequestParam("ccId") Long ccId , @RequestParam("qty") int qty);

}
```

Booking 서비스 내 Req/Resp

```java
    @PostPersist
    public void onPostPersist() throws Exception{
        
        
        boolean rslt = BookingApplication.applicationContext.getBean(concertbooking.external.ConcertService.class)
            .checkAndBookStock(this.getCcId(), this.getQty());

            if (rslt) {
                Booked booked = new Booked();
                BeanUtils.copyProperties(this, booked);
                booked.publishAfterCommit();
            }  
            else{
                throw new Exception("Out of Stock Exception Raised.");
            }      
        

    }
```

Concert 서비스 내 Booking 서비스 Feign Client 요청 대상

```java
@RestController
public class ConcertController {

@Autowired
ConcertRepository concertRepository;

@RequestMapping(value = "/checkAndBookStock",
        method = RequestMethod.GET,
        produces = "application/json;charset=UTF-8")

public boolean checkAndBookStock(HttpServletRequest request, HttpServletResponse response)
        throws Exception {
     
        System.out.println("##### /concert/checkAndBookStock  called #####");

        boolean status = false;
        
        Long ccId = Long.valueOf(request.getParameter("ccId"));
        int qty = Integer.parseInt(request.getParameter("qty"));

        System.out.println("##### ccid #####" + ccId +"##### qty" + qty);
        Optional<Concert> concert = concertRepository.findById(ccId);
        
        if(concert.isPresent()){

                Concert concertValue = concert.get();

                if (concertValue.getStock() >= qty) {
                        concertValue.setStock(concertValue.getStock() - qty);
                        concertRepository.save(concertValue);
                        status = true;
                        System.out.println("##### /concert/checkAndBookStock  qty check true ##### stock"+concertValue.getStock()+"### qty"+ qty);
                }

                System.out.println("##### /concert/checkAndBookStock  qty check false ##### stock"+concertValue.getStock()+"### qty"+ qty);
        }

        return status;
        }
        
 }
```

동작 확인

티켓 예매하기 시도 시 티켓의 재고 수량을 체크함

![image](https://user-images.githubusercontent.com/82795860/120994076-1e8a6780-c7bf-11eb-8374-53f7a4336a1a.png)


티켓 예매 시 티켓의 재고수량을 초과하지 않으면 예매 가능

![image](https://user-images.githubusercontent.com/82795860/120997798-78406100-c7c2-11eb-90fa-b8ff71f53c77.png)


티켓 예매시 티켓 재고수량을 초과할 경우 예매 안됨

![image](https://user-images.githubusercontent.com/82795860/120993294-5b099380-c7be-11eb-8970-b2b0e28d6e40.png)



# 운영

## Deploy/ Pipeline
각 구현체들은 각자의 source repository 에 구성되었고, 사용한 CI/CD 플랫폼은 AWS를 사용하였으며, pipeline build script 는 각 프로젝트 폴더 이하에 deployment.yml, service.yml 에 포함되었다.

- git에서 소스 가져오기

```
git clone --recurse-submodules https://github.com/skteam4/concert/concertbooking.git
```

- Build 하기

```bash
cd /alarm
cd gateway
mvn package

cd ..
cd booking
mvn package

cd ..
cd concert
mvn package

cd ..
cd delivery
mvn package

cd ..
cd payment
mvn package
```

- Docker Image Push/deploy/서비스생성(yml이용)

```sh
-- 기본 namespace 설정
kubectl config set-context --current --namespace=anticorona

-- namespace 생성
kubectl create ns anticorona

cd gateway
az acr build --registry skccanticorona --image skccanticorona.azurecr.io/gateway:latest .

cd kubernetes
kubectl apply -f deployment.yml
kubectl apply -f service.yaml

cd ..
cd booking
az acr build --registry skccanticorona --image skccanticorona.azurecr.io/booking:latest .

cd kubernetes
kubectl apply -f deployment.yml
kubectl apply -f service.yaml

cd ..
cd vaccine
az acr build --registry skccanticorona --image skccanticorona.azurecr.io/vaccine:latest .

cd kubernetes
kubectl apply -f deployment.yml
kubectl apply -f service.yaml

cd ..
cd injection
az acr build --registry skccanticorona --image skccanticorona.azurecr.io/injection:latest .

cd kubernetes
kubectl apply -f deployment.yml
kubectl apply -f service.yaml

cd ..
cd mypage
az acr build --registry skccanticorona --image skccanticorona.azurecr.io/mypage:latest .

cd kubernetes
kubectl apply -f deployment.yml
kubectl apply -f service.yaml

```

- anticorona/gateway/kubernetes/deployment.yml 파일 

```yml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: gateway
  namespace: anticorona
  labels:
    app: gateway
spec:
  replicas: 1
  selector:
    matchLabels:
      app: gateway
  template:
    metadata:
      labels:
        app: gateway
    spec:
      containers:
        - name: gateway
          image: skccanticorona.azurecr.io/gateway:latest
          ports:
            - containerPort: 8080
```	  

- anticorona/gateway/kubernetes/service.yaml 파일 

```yml
apiVersion: v1
kind: Service
metadata:
  name: gateway
  namespace: anticorona
  labels:
    app: gateway
spec:
  ports:
    - port: 8080
      targetPort: 8080
  type: LoadBalancer
  selector:
    app: gateway
```	  

- anticorona/booking/kubernetes/deployment.yml 파일 

```yml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: booking
  namespace: anticorona
  labels:
    app: booking
spec:
  replicas: 1
  selector:
    matchLabels:
      app: booking
  template:
    metadata:
      labels:
        app: booking
    spec:
      containers:
        - name: booking
          image: skccanticorona.azurecr.io/booking:latest
          ports:
            - containerPort: 8080
          env:
            - name: vaccine-url
              valueFrom:
                configMapKeyRef:
                  name: apiurl
                  key: url
```	  

- anticorona/booking/kubernetes/service.yaml 파일 

```yml
apiVersion: v1
kind: Service
metadata:
  name: booking
  namespace: anticorona
  labels:
    app: booking
spec:
  ports:
    - port: 8080
      targetPort: 8080
  selector:
    app: booking
```	  

- deploy 완료(istio 부착기준)

![image](https://user-images.githubusercontent.com/82795806/120998532-24824780-c7c3-11eb-8f01-d73860d68426.png)

***

## Config Map

- 변경 가능성이 있는 설정을 ConfigMap을 사용하여 관리  
  - booking 서비스에서 바라보는 vaccine 서비스 url 일부분을 ConfigMap 사용하여 구현​  

- in booking src (booking/src/main/java/anticorona/external/VaccineService.java)  
    ![configmap-in src](https://user-images.githubusercontent.com/18115456/120984025-35c45780-c7b5-11eb-8181-bfed9a943e67.png)

- booking application.yml (booking/src/main/resources/application.yml)​  
    ![configmap-application yml](https://user-images.githubusercontent.com/18115456/120984136-5096cc00-c7b5-11eb-8745-78cb754c0e1b.PNG)

- booking deploy yml (booking/kubernetes/deployment.yml)  
    ![configmap-deploy yml](https://user-images.githubusercontent.com/18115456/120984461-a2d7ed00-c7b5-11eb-9f2f-6b09ad0ba9cf.png)

- configmap 생성 후 조회

    ```sh
    kubectl create configmap apiurl --from-literal=url=vaccine -n anticorona
    ```

    ![configmap-configmap조회](https://user-images.githubusercontent.com/18115456/120985042-2eea1480-c7b6-11eb-9dbc-e73d696c003b.PNG)

- configmap 삭제 후, 에러 확인  

    ```sh
    kubectl delete configmap apiurl
    ```

    ![configmap-오류1](https://user-images.githubusercontent.com/18115456/120985205-5b9e2c00-c7b6-11eb-8ede-df74eff7f344.png)

    ![configmap-오류2](https://user-images.githubusercontent.com/18115456/120985213-5ccf5900-c7b6-11eb-9c06-5402942329a3.png)  

## Persistence Volume
  
PVC 생성 파일

<code>injection-pvc.yml</code>
- AccessModes: **ReadWriteMany**
- storeageClass: **azurefile**

![image](https://user-images.githubusercontent.com/2360083/120986163-41188280-c7b7-11eb-8e23-755d645efbed.png)

<code>deployment.yml</code>

- Container에 Volumn Mount

![image](https://user-images.githubusercontent.com/2360083/120983890-175e5c00-c7b5-11eb-9332-04033438cea1.png)

<code>application.yml</code>
- profile: **docker**
- logging.file: PVC Mount 경로

![image](https://user-images.githubusercontent.com/2360083/120983856-10374e00-c7b5-11eb-93d5-42e1178912a8.png)

마운트 경로에 logging file 생성 확인

```sh
$ kubectl exec -it injection -n anticorona -- /bin/sh
$ cd /mnt/azure/logs
$ tail -n 20 -f injection.log
```

<img src="https://user-images.githubusercontent.com/2360083/121015318-d296ed00-c7d5-11eb-90ad-679f6513905d.png" width="100%" />

## Autoscale (HPA)

  앞서 CB 는 시스템을 안정되게 운영할 수 있게 해줬지만 사용자의 요청을 100% 받아들여주지 못했기 때문에 이에 대한 보완책으로 자동화된 확장 기능을 적용하고자 한다. 
  
- 현재상태 확인
```sh
$ kubectl get deploy complain -w -n conertbooking

- 예약 서비스에 리소스에 대한 사용량을 정의한다.

<code>booking/kubernetes/deployment.yml</code>

```yml
  resources:
    requests:
      memory: "64Mi"
      cpu: "250m"
    limits:
      memory: "500Mi"
      cpu: "500m"
```

- 예약 서비스에 대한 replica 를 동적으로 늘려주도록 HPA 를 설정한다. 설정은 CPU 사용량이 15프로를 넘어서면 replica 를 10개까지 늘려준다:

```sh
$ kubectl autoscale deploy booking --min=1 --max=10 --cpu-percent=15
```

![image](https://user-images.githubusercontent.com/82795806/120987663-c51f3a00-c7b8-11eb-8cc3-59d725ca2f69.png)


- CB 에서 했던 방식대로 워크로드를 걸어준다.

```sh
$ siege -c200 -t10S -v --content-type "application/json" 'http://booking:8080/bookings POST {"ccId":1, "ccName":"FIZER", "qty":5, "status":"BOOKED"}'
```

- 오토스케일이 어떻게 되고 있는지 모니터링을 걸어둔다:

```sh
$ kubectl get deploy booking -w
```

- 어느정도 시간이 흐른 후 스케일 아웃이 벌어지는 것을 확인할 수 있다:

* siege 부하테스트 - 전

![image](https://user-images.githubusercontent.com/82795806/120990254-51caf780-c7bb-11eb-98a6-243b69344f12.png)

* siege 부하테스트 - 후

![image](https://user-images.githubusercontent.com/82795806/120989337-66f35680-c7ba-11eb-9b4e-b1425d4a3c2f.png)


- siege 의 로그를 보아도 전체적인 성공률이 높아진 것을 확인 할 수 있다. 

![image](https://user-images.githubusercontent.com/82795806/120990490-93f43900-c7bb-11eb-9295-c3a0a8165ff6.png)

## Circuit Breaker

  * 서킷 브레이킹 프레임워크의 선택: Spring FeignClient + Istio를 설치하여, concertbooking namespace에 주입하여 구현함

시나리오는 예약(booking)-->콘서트(concert) 연결을 RESTful Request/Response 로 연동하여 구현이 되어있고, 예약 요청이 과도할 경우 CB 를 통하여 장애격리.

- Istio 다운로드 및 PATH 추가, 설치, namespace에 istio주입

```sh
$ curl -L https://istio.io/downloadIstio | ISTIO_VERSION=1.7.1 TARGET_ARCH=x86_64 sh -
※ istio v1.7.1은 Kubernetes 1.16이상에서만 동작
```

- istio PATH 추가

```sh
$ cd istio-1.7.1
$ export PATH=$PWD/bin:$PATH
```

- istio 설치

```sh
$ istioctl install --set profile=demo --set hub=gcr.io/istio-release
※ Docker Hub Rate Limiting 우회 설정
```

- namespace에 istio주입

```sh
$ kubectl label anticorona tutorial istio-injection=enabled
```

- Virsual Service 생성 (Timeout 3초 설정)
- anticorona/booking/kubernetes/booking-istio.yaml 파일 

```yml
  apiVersion: networking.istio.io/v1alpha3
  kind: VirtualService
  metadata:
    name: vs-booking-network-rule
    namespace: anticorona
  spec:
    hosts:
    - booking
    http:
    - route:
      - destination:
          host: booking
      timeout: 3s
```	  

![image](https://user-images.githubusercontent.com/82795806/120985451-956f3280-c7b6-11eb-95a4-eb5a8c1ebce4.png)


- Booking 서비스 재배포 후 Pod에 CB 부착 확인

![image](https://user-images.githubusercontent.com/82795806/120985804-ed0d9e00-c7b6-11eb-9f13-8a961c73adc0.png)


- 부하테스터 siege 툴을 통한 서킷 브레이커 동작 확인:
  - 동시사용자 100명, 60초 동안 실시

```sh
$ siege -c100 -t10S -v --content-type "application/json" 'http://booking:8080/bookings POST {"vaccineId":1, "vcName":"FIZER", "userId":5, "status":"BOOKED"}'
```
![image](https://user-images.githubusercontent.com/82795806/120986972-1549cc80-c7b8-11eb-83e1-7bac5a0e80ed.png)


- 운영시스템은 죽지 않고 지속적으로 CB 에 의하여 적절히 회로가 열림과 닫힘이 벌어지면서 자원을 보호하고 있음을 보여줌. 
- 약 84%정도 정상적으로 처리되었음.

***

## Zero-Downtime deploy (Readiness Probe)

- deployment.yml에 정상 적용되어 있는 readinessProbe  
```yml
readinessProbe:
  httpGet:
    path: '/actuator/health'
    port: 8080
  initialDelaySeconds: 10
  timeoutSeconds: 2
  periodSeconds: 5
  failureThreshold: 10
```

- deployment.yml에서 readiness 설정 제거 후, 배포중 siege 테스트 진행
- kubectl delete deploy --all
- kubectl apply -f deployment.yml
- kubectl apply -f service.yaml
    - hpa 설정에 의해 target 지수 초과하여 booking scale-out 진행됨  
        ![readiness-배포중](https://user-images.githubusercontent.com/18115456/120991348-7ecbda00-c7bc-11eb-8b4d-bdb6dacad1cf.png)

    - booking이 배포되는 중,  
    정상 실행중인 booking으로의 요청은 성공(201),  
    배포중인 booking으로의 요청은 실패(503 - Service Unavailable) 확인
        ![readiness2](https://user-images.githubusercontent.com/18115456/120987386-81c4cb80-c7b8-11eb-84e7-5c00a9b1a2ff.PNG)  

- 다시 readiness 정상 적용 후, Availability 100% 확인  
![readiness4](https://user-images.githubusercontent.com/18115456/120987393-825d6200-c7b8-11eb-887e-d01519123d42.PNG)

    
## Self-healing (Liveness Probe)

- deployment.yml에 정상 적용되어 있는 livenessProbe  

```yml
livenessProbe:
  httpGet:
    path: '/actuator/health'
    port: 8080
  initialDelaySeconds: 120
  timeoutSeconds: 2
  periodSeconds: 5
  failureThreshold: 5
```

- port 및 path 잘못된 값으로 변경 후, retry 시도 확인 (in booking 서비스)  
    - booking deploy yml 수정  
        ![selfhealing(liveness)-세팅변경](https://user-images.githubusercontent.com/18115456/120985806-ed0d9e00-c7b6-11eb-834f-ffd2c627ecf0.png)

    - retry 시도 확인  
        ![selfhealing(liveness)-restarts수](https://user-images.githubusercontent.com/18115456/120985797-ebdc7100-c7b6-11eb-8b29-fed32d4a15a3.png)  
