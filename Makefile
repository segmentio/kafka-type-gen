.PHONY: all
all: kafka.bnf kafka.go

.PHONY: clean
clean:
	rm -f kafka.*

kafka-type-gen-go: $(wildcard ./cmd/kafka-type-gen-go/*.go)
	go build ./cmd/kafka-type-gen-go

kafka.bnf: ./build.gradle Main.java KafkaBNF.java
	gradle run --args="kafka.bnf"

kafka.go: ./build.gradle Main.java KafkaGo.java kafka-type-gen-go
	gradle run --args="kafka.go"
	./kafka-type-gen-go kafka.go
	go fmt kafka.go
