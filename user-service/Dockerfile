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

# Point de montage des photos de profil (PHOTOS_PATH). Le repertoire
# doit exister ET appartenir a "app" DANS L'IMAGE : un volume nomme
# reprend les droits du repertoire qu'il recouvre. Sans cela il est
# monte en root et le service, qui ne tourne pas en root, ne peut pas
# y ecrire.
RUN mkdir -p /data/photos /data/cv && chown -R app:app /data

USER app

EXPOSE 8082

# MaxRAMPercentage : la JVM respecte le mem_limit du conteneur.
# SerialGC : moins de threads, adapte aux petits services.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=70 -XX:+UseSerialGC -Xss512k"

ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar"]
