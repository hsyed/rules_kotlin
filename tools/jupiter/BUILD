# Copyright 2018 The Bazel Authors. All rights reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
java_library(
    name = "jupiter_api",
    testonly = 1,
    exports = [
        "//third_party/jvm/org/junit/jupiter:junit_jupiter_api",
        "//third_party/jvm/org/junit/jupiter:junit_jupiter_params",
        "//third_party/jvm/org/junit/platform:junit_platform_suite_api",
        "//third_party/jvm/org/junit/platform:junit_platform_runner",
        "//third_party/jvm/junit:junit",
        "//third_party/jvm/org/apiguardian:apiguardian_api"
    ],
    runtime_deps = [
        "//third_party/jvm/org/junit/platform:junit_platform_engine",
        "//third_party/jvm/org/junit/jupiter:junit_jupiter_engine",
    ],
    visibility = ["//visibility:public"],
)