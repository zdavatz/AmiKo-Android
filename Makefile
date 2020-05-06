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

bundle:
	./gradlew bundle $(ARGS)
.PHONEY: bundle

publishBundle:
	./gradlew publishBundle $(ARGS)
.PHONEY: publishBundle

clean:
	./gradlew clean
.PHONEY: clean
