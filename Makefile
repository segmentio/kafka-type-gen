dependencies := ./build.gradle
sources := $(wildcard *.java)

.PHONY: all
all: kafka.bnf kafka.go

.PHONY: clean
clean:
	rm -f kafka.*

kafka.bnf: $(sources) $(dependencies)
	gradle run --args="kafka.bnf"

kafka.go: $(sources) $(dependencies)
	gradle run --args="kafka.go"
	go fmt kafka.go
