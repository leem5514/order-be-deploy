# 멀티 스테이지 빌드 방법 사용

# 첫번째 스테이지
FROM openjdk:11 as stage1

WORKDIR /app

# /app/gradlew 파일로 생성
COPY gradlew .
#/app/gradle 폴더로 생성
COPY gradle gradle
COPY src src
COPY build.gradle .
COPY settings.gradle .

FROM openjdk:11
WORKDIR /app
# stage1의 jar을 stage2의 app.jar 이름으로 copy
COPY --from=stage1 /app/build/libs/*.jar app.jar

#CMD 또는 ENTRYPOINT을 통해서 컨테이너를 실행
ENTRYPOINT ["java", "-jar", "app.jar"]
