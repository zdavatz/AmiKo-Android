identifier="com.ywesee.amiko"

amiko: clean
	./gradlew assembleAmiko $(ARGS)
.PHONEY: amiko

comed: clean
	./gradlew assembleComed $(ARGS)
.PHONEY: comed

bundleAmiko: clean
	./gradlew bundleAmiko $(ARGS)
.PHONEY: bundleAmiko

bundleComed: clean
	./gradlew bundleComed $(ARGS)
.PHONEY: bundleComed

clean:
	./gradlew clean
.PHONEY: clean
