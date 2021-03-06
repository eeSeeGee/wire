/*
 * Copyright 2018 Square Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.wire.schema

import com.google.common.io.Closer
import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import okio.buffer
import okio.sink
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Test
import java.nio.file.FileSystem
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.assertFailsWith
import kotlin.text.Charsets.UTF_8

class RootTest {
  private val fs = Jimfs.newFileSystem(Configuration.unix())
  private val closer = Closer.create()

  @After fun tearDown() {
    closer.close()
  }

  @Test fun standaloneFile() {
    fs.add("sample/src/main/proto/squareup/dinosaurs/dinosaur.proto", "/* dinosaur.proto */")

    val location = Location.get("sample/src/main/proto", "squareup/dinosaurs/dinosaur.proto")
    val roots = location.roots(fs, closer)
    assertThat(roots.size == 1)

    // Standalone files resolve because we have a base directory.
    assertThat(roots[0].resolve("squareup/dinosaurs/dinosaur.proto")).isEqualTo(roots[0])
    assertThat(roots[0].resolve("sample/src/main/proto/squareup/dinosaurs/dinosaur.proto")).isNull()

    // But we can enumerate their contents.
    assertThat(roots[0].allProtoFiles().map { it.location }).containsOnly(location)
  }

  @Test fun directory() {
    fs.add("sample/src/main/proto/squareup/dinosaurs/dinosaur.proto", "/* dinosaur.proto */")
    fs.add("sample/src/main/proto/squareup/dinosaurs/geology.proto", "/* geology.proto */")

    val sourceDir = Location.get("sample/src/main/proto")
    val roots = sourceDir.roots(fs, closer)
    assertThat(roots.size == 1)

    assertThat(roots[0].resolve("squareup/dinosaurs/dinosaur.proto")?.location)
        .isEqualTo(Location.get(sourceDir.toString(), "squareup/dinosaurs/dinosaur.proto"))

    assertThat(roots[0].resolve("squareup/dinosaurs/unknown.proto")).isNull()

    assertThat(roots[0].allProtoFiles().map { it.location }).containsExactlyInAnyOrder(
        Location.get(sourceDir.toString(), "squareup/dinosaurs/dinosaur.proto"),
        Location.get(sourceDir.toString(), "squareup/dinosaurs/geology.proto")
    )
  }

  @Test fun zip() {
    fs.addZip(
        "lib/dinosaurs.zip",
        "squareup/dinosaurs/dinosaur.proto" to "/* dinosaur.proto */",
        "squareup/dinosaurs/geology.proto" to "/* geology.proto */")

    val sourceZip = Location.get("lib/dinosaurs.zip")
    val roots = sourceZip.roots(fs, closer)
    assertThat(roots.size == 1)

    assertThat(roots[0].resolve("squareup/dinosaurs/dinosaur.proto")?.location)
        .isEqualTo(Location.get(sourceZip.toString(), "squareup/dinosaurs/dinosaur.proto"))

    assertThat(roots[0].resolve("squareup/dinosaurs/unknown.proto")).isNull()

    assertThat(roots[0].allProtoFiles().map { it.location }).containsExactlyInAnyOrder(
        Location.get(sourceZip.toString(), "squareup/dinosaurs/dinosaur.proto"),
        Location.get(sourceZip.toString(), "squareup/dinosaurs/geology.proto")
    )
  }

  @Test fun zipProtoFilesOnly() {
    fs.addZip(
        "lib/dinosaurs.zip",
        "squareup/dinosaurs/raptor.proto" to "/* raptor.proto */",
        "squareup/dinosaurs/raptor.nba" to "/* raptor.nba */")

    val sourceZip = Location.get("lib/dinosaurs.zip")
    val roots = sourceZip.roots(fs, closer)
    assertThat(roots.size == 1)
    assertThat(roots[0].allProtoFiles().map { it.location })
        .containsOnly(Location.get(sourceZip.toString(), "squareup/dinosaurs/raptor.proto"))
  }

  @Test fun directoryProtoFilesOnly() {
    fs.add("sample/src/main/proto/squareup/dinosaurs/raptor.proto", "/* raptor.proto */")
    fs.add("sample/src/main/proto/squareup/dinosaurs/raptor.nba", "/* raptor.nba */")

    val sourceDir = Location.get("sample/src/main/proto")
    val roots = sourceDir.roots(fs, closer)
    assertThat(roots.size == 1)
    assertThat(roots[0].allProtoFiles().map { it.location })
        .containsOnly(Location.get(sourceDir.toString(), "squareup/dinosaurs/raptor.proto"))
  }

  @Test fun standaloneFileMustBeProtoOrZip() {
    fs.add("sample/src/main/proto/squareup/dinosaurs/raptor.nba", "/* raptor.nba */")

    val onlyFile = Location.get("sample/src/main/proto/squareup/dinosaurs/raptor.nba")
    val exception = assertFailsWith<IllegalArgumentException> {
      onlyFile.roots(fs, closer)
    }
    assertThat(exception)
        .hasMessage("expected a directory, archive (.zip / .jar / etc.), or .proto: $onlyFile")
  }
}

fun FileSystem.add(pathString: String, contents: String) {
  val path = getPath(pathString)
  if (path.parent != null) {
    Files.createDirectories(path.parent)
  }
  Files.write(path, contents.toByteArray(UTF_8))
}

fun FileSystem.symlink(linkPathString: String, targetPathString: String) {
  val linkPath = getPath(linkPathString)
  if (linkPath.parent != null) {
    Files.createDirectories(linkPath.parent)
  }
  val targetPath = getPath(targetPathString)
  Files.createSymbolicLink(linkPath, targetPath)
}

fun FileSystem.get(pathString: String): String {
  val path = getPath(pathString)
  return String(Files.readAllBytes(path), UTF_8)
}

fun FileSystem.find(path: String): Set<String> {
  val result = mutableSetOf<String>()
  Files.walkFileTree(getPath(path), object : SimpleFileVisitor<Path>() {
    override fun visitFile(path: Path, attrs: BasicFileAttributes): FileVisitResult {
      if (!Files.isDirectory(path)) {
        result.add(path.toString())
      }
      return FileVisitResult.CONTINUE
    }
  })
  return result
}

fun FileSystem.addZip(pathString: String, vararg contents: Pair<String, String>) {
  val path = getPath(pathString)
  if (path.parent != null) {
    Files.createDirectories(path.parent)
  }

  ZipOutputStream(Files.newOutputStream(path)).use { zipOut ->
    val zipSink = zipOut.sink().buffer()
    for ((elementPath, elementContents) in contents) {
      zipOut.putNextEntry(ZipEntry(elementPath))
      zipSink.writeUtf8(elementContents)
      zipSink.flush()
    }
  }
}