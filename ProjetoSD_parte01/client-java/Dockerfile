FROM eclipse-temurin:17-jdk

WORKDIR /app
COPY . .

RUN apt-get update && apt-get install -y wget

# Bibliotecas obrigatórias
RUN wget https://repo1.maven.org/maven2/org/zeromq/jeromq/0.5.2/jeromq-0.5.2.jar
RUN wget https://repo1.maven.org/maven2/org/msgpack/msgpack-core/0.9.0/msgpack-core-0.9.0.jar

RUN javac -cp ".:jeromq-0.5.2.jar:msgpack-core-0.9.0.jar" *.java

CMD ["java", "-cp", ".:jeromq-0.5.2.jar:msgpack-core-0.9.0.jar", "Client"]