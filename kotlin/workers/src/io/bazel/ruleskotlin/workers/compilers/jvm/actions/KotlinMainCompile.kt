/*
 * Copyright 2018 The Bazel Authors. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.bazel.ruleskotlin.workers.compilers.jvm.actions


import io.bazel.ruleskotlin.workers.BuildAction
import io.bazel.ruleskotlin.workers.CompileResult
import io.bazel.ruleskotlin.workers.Context
import io.bazel.ruleskotlin.workers.KotlinToolchain
import io.bazel.ruleskotlin.workers.compilers.jvm.utils.KotlinCompilerOutputProcessor
import io.bazel.ruleskotlin.workers.model.CompileDirectories
import io.bazel.ruleskotlin.workers.model.Flags
import io.bazel.ruleskotlin.workers.model.Metas
import java.util.*

// The Kotlin compiler is not suited for javac compilation as of 1.2.21. The errors are not conveyed directly and would need to be preprocessed, also javac
// invocations Configured via Kotlin use eager analysis in some corner cases this can result in classpath exceptions from the Java Compiler..
class KotlinMainCompile(toolchain: KotlinToolchain) : BuildAction("compile kotlin classes", toolchain) {
    companion object {
        /**
         * Default fields that are directly mappable to kotlin compiler args.
         */
        private val COMPILE_MAPPED_FLAGS = arrayOf(
                Flags.CLASSPATH,
                Flags.KOTLIN_API_VERSION,
                Flags.KOTLIN_LANGUAGE_VERSION,
                Flags.KOTLIN_JVM_TARGET)

        val Result = CompileResult.Meta("kotlin_compile_result")
    }

    private fun prepareAnnotationProcessingArgs(ctx: Context): List<String>? =
            PluginDescriptors[ctx]?.let { descriptor ->
                if (descriptor.processors.isNotEmpty()) {
                    val compileDirectories = CompileDirectories[ctx]
                    PluginArgBuilder(toolchain.KAPT_JAR_PATH.toString(), "org.jetbrains.kotlin.kapt3").let { arg ->
                        arg["sources"] = compileDirectories.annotationProcessingSources
                        arg["classes"] = compileDirectories.annotionProcessingClasses
                        arg["stubs"] = compileDirectories.annotationProcessingStubs
                        arg["incrementalData"] = compileDirectories.annotationProcessingIncrementalData

                        arg["aptMode"] = "stubsAndApt"
                        arg["correctErrorTypes"] = "true"
//                    arg["verbose"] = "true"

                        val processorClassNames = mutableListOf<String>()
                        descriptor.processors.forEach {
                            it.classPath.forEach { arg["apclasspath"] = it }
                            processorClassNames.add(it.processorClass)
                        }
                        arg["processors"] = processorClassNames.joinToString(",")

                        arg.argList
                    }
                } else null
            }


    /**
     * Evaluate the compilation context and add Metadata to the ctx if needed.
     *
     * @return The args to pass to the kotlin compile class.
     */
    private fun setupCompileContext(ctx: Context): MutableList<String> {
        val args = mutableListOf<String>()
        val compileDirectories = CompileDirectories[ctx]

        ctx.copyOfFlags(*COMPILE_MAPPED_FLAGS).forEach { field, arg ->
            args.add(field.kotlinFlag!!); args.add(arg)
        }

//        Collections.addAll(args, "-kotlin-home", KotlinToolchain.KOTLIN_HOME.toString())
        Collections.addAll(args, "-d", compileDirectories.classes)
        return args
    }

    override fun invoke(ctx: Context): Int {
        val commonArgs = setupCompileContext(ctx)
        val maybeAnnotationPorcessingArgs = prepareAnnotationProcessingArgs(ctx)
        val sources = Metas.ALL_SOURCES.mustGet(ctx)

        // run a kapt generation phase if needed.
        if (maybeAnnotationPorcessingArgs != null) {
            val ret = invokeCompilePhase(
                    args = mutableListOf(*commonArgs.toTypedArray()).let {
                        it.addAll(maybeAnnotationPorcessingArgs)
                        it.addAll(sources)
                        it.toTypedArray()
                    },
                    onNonTeminalExitCode = { outputProcessors, exitCode ->
                        outputProcessors.process()
                        exitCode
                    }
            )
            if (ret != 0) {
                return ret
            }
        }

        return invokeCompilePhase(
                args = commonArgs.let { args ->
                    args.addAll(sources)
                    ctx.annotationProcessingGeneratedJavaSources()?.also { args.addAll(it) }
                    args.toTypedArray()
                },
                onNonTeminalExitCode = { outputProcessor, exitCode ->
                    // give javac a chance to process the java sources.
                    Result[ctx] = CompileResult.deferred(exitCode) { _ ->
                        outputProcessor.process()
                        exitCode
                    }
                    0
                }
        )
    }

    private fun invokeCompilePhase(args: Array<String>, onNonTeminalExitCode: (KotlinCompilerOutputProcessor, Int) -> Int): Int {
        val outputProcessor = KotlinCompilerOutputProcessor.ForKotlinC(System.out)

        val exitCode = try {
            toolchain.kotlinCompiler.compile(args, outputProcessor.collector)
        } catch (ex: Exception) {
            outputProcessor.process()
            throw ex
        }

        if (exitCode < 2) {
            // 1 is a standard compilation error
            // 2 is an internal error
            // 3 is the script execution error
            return onNonTeminalExitCode(outputProcessor, exitCode)
        } else {
            outputProcessor.process()
            throw RuntimeException("KotlinMainCompile returned terminal error code: $exitCode")
        }
    }
}
