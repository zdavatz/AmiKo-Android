identifier="com.ywesee.amiko"

amiko:
	./gradlew assembleAmiko $(ARGS)
.PHONEY: amiko

comed:
	./gradlew assembleComed $(ARGS)
.PHONEY: comed

clean:
	./gradlew clean
.PHONEY: clean
