all:: api docker

docker::
	@docker compose up -d --build --remove-orphans api

ci::
	@./gradlew assemble test --refresh-dependencies --rerun-tasks

api::
	@./gradlew assemble test

clean::
	@./gradlew clean
