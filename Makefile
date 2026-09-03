all:: api docker

docker::
	@docker compose up -d --build --remove-orphans api

api::
	@./gradlew assemble test

clean::
	@./gradlew clean
