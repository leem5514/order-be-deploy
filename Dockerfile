#멀티 스테이지 빌드?

#첫 번쨰 스테이지
FROM openjdk:11 as stage1
#이 위치로 카피한다고 생각하면된다
WORKDIR /app

#로컬호스트에있는 파일들을 도커 컨테이너 에다가 copy를 해야한다.
#필요한 파일들만 카피하면 된다. 꼭 필요한 요소들만해야함

# . 찍으면 /app/gradlew 파일로 생성
COPY gradlew .
# /app/gradle 폴더로 생성
COPY gradle gradle
COPY src src
COPY build.gradle .
COPY settings.gradle .
# 도커 컨테이너 안에서 bootJar
# 명령어를 보내는 것인데 chmod 777이 필요할 수 있다.
RUN ./gradlew bootJar

#두번째 스테이지란? 새로운 컨테이너(리눅스)환경을 다시 만들어버리겠다.
FROM openjdk:11
WORKDIR /app
#자르파일 생성되면 해당 경로에 생성된다
# stage1에 있는 jar를 stage2의 app.jar라는 이름으로 copy 하겠다.
COPY --from=stage1 /app/build/libs/*.jar app.jar

#CMD 또는 ENTRYPOINT(엔트리포인트)를 통해 컨테이너를 실행한다.
#엔트리포인트가 더 많이 사용된다
ENTRYPOINT ["java","-jar","app.jar"]
#도커 컨테이너 내에서 밖의 전체 host를 지칭하는 도메인 : host.docker.internal
#docker run -d -p 8080:8080 -e SPRING_DATASOURCE_URL=jdbc:mariadb://host.docker.internal:3306/board ordersystem:latest
#(USERNAME)도 들어갈 수 있다.
#이름붙여서 docker run --name ordersystem-be -d -p 8080:8080 -e SPRING_DATASOURCE_URL=jdbc:mariadb://host.docker.internal:3306/board ordersystem:latest

#볼륨 옵션 (중요함)
#docker run -d -p 8081:8080 -e SPRING_DATASOURCE_URL=jdbc:mariadb://host.docker.internal:3306/board -v C:\Users\Playdata\Desktop\devops\tmp_logs:/app/logs spring:latest