.ONESHELL:
ROOT_DIR:=$(shell dirname $(realpath $(firstword $(MAKEFILE_LIST))))
BUILD_DIR=$(ROOT_DIR)/build
SRC_DIR=$(ROOT_DIR)/src/main/java/com/edptoscaqs/toscaservice
TEST_DIR=$(ROOT_DIR)/src/test/java/com/edptoscaqs/toscaservice
VERSION=v0.1
COMMIT=`git rev-parse --short=4 HEAD`
BUILD_TIME=`date +"%m/%d/%Y %H:%M:%S"`

build: get build_jar build_linux

build_jar: build_prepare
	cd $(ROOT_DIR)
	./gradlew build

build_linux: build_prepare
	cd $(ROOT_DIR) && ./gradlew nativeBuild
	cp $(ROOT_DIR)/build/native/nativeCompile/toscactl-linux-amd64 $(BUILD_DIR)/toscactl-linux-amd64

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