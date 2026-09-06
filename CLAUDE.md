# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & test

Maven, Java 21 (`source`/`target` 21 in `pom.xml`; note the GitHub workflows still pin JDK 17).

```bash
mvn test                                  # full suite (~15s, PerfReadWriteTest dominates)
mvn test -Dtest=GSonUtilsTest             # single test class
mvn test -Dtest=GSonUtilsTest#globWriterTest
mvn package
mvn -s settings.xml -B package            # what CI runs; settings.xml maps the GitHub Packages
                                          # repo to $GH_MAVEN_REGISTRY_USER / $GH_MAVEN_REGISTRY_ACCESS_TOKEN
```

The `org.globsframework:globs` dependency comes from GitHub Packages, so a cold local repo needs those
env vars (or `-o` once the artifacts are cached).

Releases go through `mvn release:prepare/perform` (leftover `pom.xml.releaseBackup` / `release.properties`
in the working tree are release-plugin artifacts, not source). The `release` profile adds sources,
javadoc, GPG signing and Sonatype Central publishing.

## What this library does

Serializes/deserializes globs framework `Glob` objects to JSON **without reflection** — every field is
handled by walking the `GlobType` metamodel with a field visitor. It also serializes the metamodel
itself (`GlobType` ⇄ JSON), so types can be shipped over the wire and rebuilt at runtime.

Two properties drive most design decisions:

- A `Glob` distinguishes *unset* from *null*. Unset fields are simply not written; an explicit JSON
  `null` is read back as `setValue(field, null)`. Round-tripping foreign JSON therefore preserves the
  original field set.
- Type identity is carried by an optional `_kind` attribute (`GlobsGson.KIND_NAME`). With `_kind`
  present, a `GlobTypeResolver` reconstructs the type; without it, the caller must supply the
  `GlobType` up front.

## The three serialization paths

This is the single most important thing to know: there are **three parallel implementations**, each
with its own set of field visitors and its own annotation coverage. A change to serialization behavior
usually has to be replicated in more than one of them.

1. **`GSonUtils`** — streaming, no intermediate object graph. Writes via
   `JsonFieldValueWithWriterVisitor` (and `JsonFieldValueVisitorHideWithWriterSensitiveData` for the
   hide-sensitive-data variant), reads via `GlobGSonDeserializer.readFields` →
   `ReadJsonWithReaderFieldVisitor`. The default entry point for plain encode/decode. Explicitly does
   **not** honour custom `ToStringFieldJsonSerializer` (see the comment at the top of the class).

2. **`GlobsGson.createBuilder(resolver)`** — a configured `Gson` with type-hierarchy adapters for
   `Glob`, `GlobType`, `GlobTypeSet`, `ChangeSet` and `PreChangeSet`. Works over `JsonElement` trees
   (`GSonVisitor`, `GlobGSonDeserializer.deserialize`). Required whenever a `GlobType`, a
   `GlobTypeSet` or a `ChangeSet` is involved, or when globs are nested inside other Gson-serialized
   objects. The `GlobType` adapter is stateful — do not share a builder across unrelated resolvers.

3. **`GlobJsonService` / `JsonSerializerServiceImpl`** — pre-compiles, per `GlobType`, an array of
   `JsonFieldSerializer`/`JsonFieldDeSerializer` indexed by field index (built by
   `JsonFieldSerializerVisitor`, the largest file in the repo). Fastest for repeated
   encode/decode of the same type and the only path supporting `ToStringFieldJsonSerializer`,
   `JsonHideValue` and the `JsonFlatten*` family. Instances are cached and built under `synchronized`
   so recursive `GlobType`s resolve. Writes through the library's own `JsonWriter` abstraction
   (`field/JsonWriter.java`) rather than Gson's, so wrappers like `NoNameJsonWriter` can suppress
   names for flattened values.

Rough annotation coverage per path:

| Annotation | `GSonUtils` | Gson adapters | `GlobJsonService` |
| --- | --- | --- | --- |
| `IsJsonContent` | ✓ | ✓ | ✓ |
| `JsonDateFormat` / `JsonDateTimeFormat` | ✓ | ✓ | ✓ |
| `JsonAsObject` + `JsonValueAsField` | ✓ | — | ✓ |
| `JsonHideValue` | via `encodeHidSensitiveData` | — | ✓ |
| `JsonFlatten*` | — | — | ✓ |

## Annotation convention

A JSON annotation is one file under `json/annottations/` (note the spelling of the package):
`Xxx.java`, the glob-native annotation — a `GlobType TYPE` built with `GlobTypeBuilderFactory`, plus
`UNIQUE_KEY` / `UNIQUE_GLOB` (or fields, when the annotation is parameterized like
`JsonDateTimeFormat`). This is what runtime code tests with `field.hasAnnotation(Xxx.UNIQUE_KEY)`.

There used to be a second file per annotation, `Xxx_.java`, a plain Java `@interface` mirroring the
type for `GlobType`s declared as Java classes. **That half is gone across the workspace — do not
write one.**

Adding an annotation means: write the file, register `Xxx.TYPE` in
`AllJsonAnnotations.MODEL`, and implement it in whichever of the three paths must support it.
`AllJsonAnnotations.RESOLVER` (core annotations + JSON annotations) is what callers pass to
`GSonUtils.decodeGlobType`.

Semantics worth knowing:
- `JsonValueAsField` + `JsonAsObject`: a `GlobArrayField` is encoded as a JSON *object* whose keys are
  the value of the target type's `JsonValueAsField` string field (e.g. OpenAPI `"200": {...}`).
- `JsonFlatten` (on a type) + `JsonFlattenTargetArray` (on the array field) + `JsonFlattenAttribute`
  (the key field of the target) + optional `JsonFlattenTargetAttribute`: unknown JSON names are
  absorbed into the array rather than skipped — an arbitrary-key map represented as globs.
- `IsJsonContent`: the string field holds raw JSON, written with `jsonValue()` instead of quoted.
- `UnknownAnnotation`: when an annotation kind cannot be resolved and `ignoreUnknownAnnotation` is on,
  the raw JSON is kept in a placeholder glob so it survives a round trip.

## GlobType serialization

`GlobTypeSet.export(type)` transitively collects the type, its field target types, union targets and
all annotation types, then the Gson adapters emit the `{kind, fields:[{name,type,annotations}], annotations}`
format whose string constants all live in `GlobsGson`. `helper/LoadingGlobTypeResolver` reads such a
document back, resolving forward and circular references lazily by keeping the not-yet-built
`JsonObject`s in a map and building on first `findType`.

## ChangeSet

`ChangeSetGsonAdapter` / `ChangeValuesGsonAdapter` serialize a `ChangeSet`. Reading is two-phase:
`PreChangeSetGsonAdapter` produces a `PreChangeSet`, which only becomes a `ChangeSet` once the caller
supplies a `GlobAccessor` (`preChangeSet.resolve(accessor)`) — needed because resolving a change
requires access to existing globs, notably for union-typed key fields.

## Performance-sensitive code

`PerfReadWriteTest` guards the hot paths, and several classes exist purely for throughput:
`GSonUtils.NoLockStringReader` and `StringWriterToBuilder` avoid `StringReader`/`StringBuffer`
synchronization, date formatters are cached in `CACHE_DATE` / `CACHE_DATE_TIME` (keyed by `Field`),
`helper/ISO8601Utils` is a hand-rolled ISO-8601 parser/formatter used by the `useFastIso8601` /
`strictIso8601` options, and the field-indexed serializer arrays in `JsonSerializerServiceImpl` replace
per-field lookups. Prefer keeping allocations out of these paths.

### Core callers were tried on `JsonSerializerServiceImpl`, and reverted (2026-08-21)

globs-bin-serialisation and globs-grpc drive their per-field leaves through a core *caller*
(`FromGlobCaller` / `ToGlobCaller`, `org.globsframework.core.model.caller`): a generated class holds each leaf
in a `static final` and unrolls the loop, so every field is a monomorphic call instead of the one megamorphic
call site a loop over a table of closures gives. The same was implemented here for the `GlobJsonService` path
only — both composites of `JsonSerializerServiceImpl`, a `call` written out in each of the ~43 leaves of
`JsonFieldSerializerVisitor`, a `NameKeySource` translating each JSON name into a field index on the read
side — and it was **reverted after measuring**. Don't redo it without new numbers.

Measured in separate JVMs (no profile pollution), 14-field type, 200k encodes/decodes, best of 6, against
globs-generate's real ASM services:

| | write | read |
| --- | --- | --- |
| loop (today) | 208-215 ms | 211-223 ms |
| caller | 208-218 ms | 235-241 ms |

The write side is a wash and the read side is ~10-13% **slower**, stable over three runs. The reason is that
JSON is not TLV: the cost of reading is Gson's tokenizer and the strings it builds, not the dispatch, and a
caller adds a `KeySource` allocation per object plus one call per name where the loop already does a plain
array index by field index. `-Dglobs.caller.toGlob` being a process-wide flag, an application turning it on
for binser or grpc would have paid that here without knowing.

Two things that came out of the exercise and would still hold if it were retried: a field with a registered
`ToStringFieldJsonSerializer` cannot be caller-driven on the write side (it serializes from the whole Glob,
where a caller only hands out the value), and a `JsonFlatten` type's composites cannot either (they write and
read names the type has no field for).

## Tests

JUnit 4 (`org.junit.Test` / `Assert`). Test `GlobType`s are declared inline as static nested classes,
either the Java-annotation style or via `GlobTypeBuilderFactory.create(...)` +
`declareXxxField(..., Xxx.UNIQUE_GLOB)`. Round-trip assertions commonly compare
`GSonUtils.normalize(expectedJson)` with `GSonUtils.normalize(actual)` to ignore formatting.
