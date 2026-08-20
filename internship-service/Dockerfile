# ============================================================
#  Dockerfile PATRON  -  a copier dans chaque service Spring
#  Seule ligne a changer d'un service a l'autre : EXPOSE
# ============================================================

# ---------- etape 1 : build ----------
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

# Le wrapper Maven d'abord : cette couche est mise en cache tant que
# le pom.xml ne change pas, ce qui evite de retelecharger les
# dependances a chaque modification du code source.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

COPY src ./src
RUN ./mvnw clean package -DskipTests -B

# ---------- etape 2 : runtime ----------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Ne jamais tourner en root dans un conteneur.
RUN addgroup -S app && adduser -S app -G app

COPY --from=build /app/target/*.jar app.jar
USER app

EXPOSE 8083

# MaxRAMPercentage : la JVM respecte le mem_limit du conteneur.
# SerialGC : moins de threads, adapte aux petits services.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=70 -XX:+UseSerialGC -Xss512k"

ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar"]
