MODULE_MAIN=.

##@ MAIN

jooq-codegen: ## Generate entities
	@bash -c "'$(CURDIR)/.make/scripts/jooq-codegen.sh'"
	@echo "Generated entities"

create-database: ## Create local database
	@bash -c "'$(CURDIR)/.make/scripts/create-database.sh'"
	@echo "Created local database"

postgresql-start: ## Postgresql server start
	@bash -c "'$(CURDIR)/.make/scripts/postgresql-start.sh'"

postgresql-stop: ## Postgresql server stop
	@bash -c "'$(CURDIR)/.make/scripts/postgresql-stop.sh'"

mvn-build: ## Building project
	./mvnw clean install -f ${MODULE_MAIN}

mvn-run: ## Run project with maven
	./mvnw mn:run -f ${MODULE_MAIN}