FROM eclipse-temurin:25-jre-alpine

WORKDIR /app

COPY app/build/distributions/app-bundle.tar /app/app-bundle.tar
COPY db/migrations/ db/migrations
RUN tar -xf /app/app-bundle.tar

EXPOSE 3001

CMD ./app-bundle/bin/app
