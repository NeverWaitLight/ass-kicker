.PHONY: build build-java build-web test test-java test-web run-java run-web deps-mongo deps-rocketmq clean

# ===== Build =====

build: build-java build-web

build-java:
	mvn -f services/java/pom.xml clean package -DskipTests

build-web:
	npm --prefix web ci
	npm --prefix web run build

# ===== Test =====

test: test-java test-web

test-java:
	mvn -f services/java/pom.xml test

test-web:
	npm --prefix web run test:unit

# ===== Run =====

run-java:
	mvn -f services/java/pom.xml spring-boot:run

run-web:
	npm --prefix web run dev

# ===== Dependencies =====

deps-mongo:
	docker compose -f infra/docker/docker-compose-mongodb.yml -p mongo up -d

deps-rocketmq:
	docker compose -f infra/docker/docker-compose-rocketmq.yml -p rocketmq up -d

# ===== Clean =====

clean:
	mvn -f services/java/pom.xml clean
	rm -rf web/dist web/node_modules
