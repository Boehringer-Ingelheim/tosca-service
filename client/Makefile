.ONESHELL:
ROOT_DIR:=$(shell dirname $(realpath $(firstword $(MAKEFILE_LIST))))
BUILD_DIR=$(ROOT_DIR)/build
SRC_DIR=$(ROOT_DIR)/src/main/java/com/edptoscaqs/toscaservice
TEST_DIR=$(ROOT_DIR)/src/test/java/com/edptoscaqs/toscaservice
VERSION=v0.1
COMMIT=`git rev-parse --short=4 HEAD`
BUILD_TIME=`date +"%m/%d/%Y %H:%M:%S"`

build: get build_jar package

build_jar: build_prepare
	cd $(ROOT_DIR)
	./gradlew build

test:
	cd $(ROOT_DIR)
	./gradlew test

clean:
	cd $(ROOT_DIR)
	./gradlew clean

get:
	cd $(ROOT_DIR)
	./gradlew dependencies

build_prepare:
	mkdir -p "$(BUILD_DIR)"

package:
	cd $(ROOT_DIR)
	jpackage --input docker/ --name toscactl --main-jar app.jar --main-class org.springframework.boot.loader.JarLauncher --java-options "--enable-preview" --type app-image
	mv toscactl toscactl-linux-amd64