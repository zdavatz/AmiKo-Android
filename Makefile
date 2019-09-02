identifier="com.ywesee.amiko"

amiko:
	./gradlew assembleAmiko $(ARGS)
.PHONEY: amiko

comed:
	./gradlew assembleComed $(ARGS)
.PHONEY: comed

bundleAmiko:
	./gradlew bundleAmiko $(ARGS)
.PHONEY: bundleAmiko

bundleComed:
	./gradlew bundleComed $(ARGS)
.PHONEY: bundleComed

clean:
	./gradlew clean
.PHONEY: clean
