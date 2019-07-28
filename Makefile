sources := $(wildcard *.java)

.PHONY: all
all: kafka.bnf kafka.go

.PHONY: clean
clean:
	rm -f kafka.*

kafka.bnf: $(sources)
	gradle run --args="kafka.bnf"

kafka.go: $(sources)
	gradle run --args="kafka.go"
	go fmt kafka.go
