.PHONY: all
all: kafka.bnf kafka.go

.PHONY: clean
clean:
	rm -f kafka.*

kafka.bnf: ./build.gradle Main.java KafkaBNF.java
	gradle run --args="kafka.bnf"

kafka.go: ./build.gradle Main.java KafkaGo.java
	gradle run --args="kafka.go"
	go fmt kafka.go
