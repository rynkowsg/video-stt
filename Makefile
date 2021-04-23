####
# DEVELOPMENT ENV
.PHONY: check-deps repl lint

check-deps:
	-clojure -M:outdated

repl:
	-clj -M:dev:test:repl

lint:
	-clojure -M:dev:test:clj-kondo --lint app/main --lint app/test

format-check:
	-lein cljfmt check $(filter-out $@,$(MAKECMDGOALS))

format:
	-lein cljfmt fix $(filter-out $@,$(MAKECMDGOALS))

####
# TESTING
.PHONY: test test-watch test-config test-help

test:
	-clojure -M:dev:test -m kaocha.runner $(filter-out $@,$(MAKECMDGOALS))

test-watch:
	-make test -- --watch

test-config:
	-make test -- --print-config

test-help:
	-make test -- --test-help

####
# OTHER
.PHONY: pom

pom:
	-clojure -Spom

# Makefile tricks for passing parameters:
# - by params: https://stackoverflow.com/a/2826178
# - by filtering out non-target: https://stackoverflow.com/a/6273809
