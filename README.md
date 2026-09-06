# Globs JSON (gson)

Encode and decode [Glob](https://globsframework.org)s to and from JSON with `com.google.code.gson`, and
**without introspection**: every field is handled by walking the `GlobType` metamodel with a field visitor.
The library also serializes the metamodel itself (`GlobType` ⇄ JSON), so a type can be shipped over the wire
and rebuilt at runtime.

The output is standard JSON, and almost any JSON can be read straight into Globs.

Two properties shape everything else:

- **unset is not null.** An unset field is simply not written; an explicit JSON `null` is read back as a set
  null. Round-tripping a foreign document therefore preserves its exact field set.
- **type identity travels in `_kind`,** optionally. With `_kind` present, a `GlobTypeResolver` rebuilds the
  type; without it, the caller supplies the `GlobType`.

## Requirements

Java 21, `org.globsframework:globs`, `com.google.code.gson:gson`.

## Installation

```xml
<dependency>
    <groupId>org.globsframework</groupId>
    <artifactId>globs-gson</artifactId>
    <version>5.3.0</version>
</dependency>
```

## Encoding and decoding

```java
String json = GSonUtils.encode(glob);                    // encode(glob, true) adds "_kind"
String pretty = GSonUtils.niceEncode(glob);
GSonUtils.encode(writer, glob);                          // straight to a Writer, no intermediate String

Glob decoded = GSonUtils.decode(new StringReader(json), LocalType.TYPE);
Glob byKind  = GSonUtils.decode(json, resolver);         // the type comes from "_kind"
Glob[] all   = GSonUtils.decodeArray(reader, LocalType.TYPE);
GSonUtils.decodeArray(reader, LocalType.TYPE, glob -> consume(glob));   // streamed, one Glob at a time

String typeAsJson = GSonUtils.encodeGlobType(LocalType.TYPE);           // the metamodel itself
GlobType type = GSonUtils.decodeGlobType(typeAsJson, AllJsonAnnotations.RESOLVER, true);
```

`GSonUtils.encodeHidSensitiveData(glob)` replaces the values of the fields annotated `JsonHideValue` — for
logs.

### Which entry point

There are three implementations, on purpose:

| | Use it for |
| --- | --- |
| `GSonUtils` | the default: streaming, no intermediate object graph |
| `GlobsGson.createBuilder(resolver)` | a configured `Gson`, needed whenever a `GlobType`, a `GlobTypeSet` or a `ChangeSet` is involved, or when Globs are nested inside other Gson-serialized objects |
| `GlobJsonService` / `JsonSerializerServiceImpl` | repeated encode/decode of the same type — it pre-compiles one serializer per field — and the only path supporting `ToStringFieldJsonSerializer`, `JsonHideValue` and the `JsonFlatten*` family |

They do not all support the same annotations; `CLAUDE.md` has the coverage table.

## Annotations

Under `json/annottations/` (note the spelling), each as the usual Glob + `@interface` pair, registered in
`AllJsonAnnotations`:

| Annotation | Effect |
| --- | --- |
| `FieldName` (core) | the JSON name of a field, when it is not the Java name |
| `JsonDateFormat` / `JsonDateTimeFormat` | the pattern for a date/time field |
| `IsJsonContent` | the String field already holds raw JSON — written unquoted, kept verbatim |
| `JsonValueAsField` + `JsonAsObject` | encode an array as a JSON *object* keyed by one of the target type's fields |
| `JsonHideValue` | the value is masked by `encodeHidSensitiveData` |
| `JsonFlatten` family | absorb unknown JSON names into an array of key/value globs — an arbitrary-key map as Globs |

`JsonValueAsField` is what reads a document whose *keys* are data. The OpenAPI format is the usual example,
where the HTTP code is a key:

```
...
   200: {
       ...
       },
   204: {
       }    
```

When a Glob is read or written with `_kind`, the reader can instantiate it by finding the corresponding
`GlobType` in the model. Otherwise the `GlobType` must be provided:

```java
Glob decode = GSonUtils.decode(new StringReader("{\"id\":24,\"name\":\"TEST éè\",\"arrival\":\"2019-09-13 13:15:21\"}"), LocalType.TYPE);
```

## A real example

Here is a Shopify product:

```
{
  "admin_graphql_api_id": "gid:\/\/shopify\/Product\/6918907461686",
  "body_html": "description du produit",
  "created_at": "2023-02-14T02:30:41+01:00",
  "handle": "lien-vers-le-produit",
  "id": 6918907461686,
  "product_type": "Mercerie",
  "published_at": "2023-02-14T02:30:41+01:00",
  "template_suffix": null,
  "title": "le titre du produit",
  "updated_at": "2024-04-14T15:56:13+02:00",
  "vendor": "Osborne",
  "status": "active",
  "published_scope": "web",
  "tags": "",
  "variants": [
    {
      "admin_graphql_api_id": "gid:\/\/shopify\/ProductVariant\/40658354864182",
      "barcode": "0096685150284",
      "compare_at_price": null,
      "created_at": "2023-02-14T02:30:41+01:00",
      "fulfillment_service": "manual",
      "id": 40658354864182,
      "inventory_management": "shopify",
      "inventory_policy": "deny",
      "position": 1,
      "price": "1.66",
```

It is read with the `GlobType` below, and written back from it. Because a Glob tells *unset* from *null*, the
document that comes back out has exactly the fields the document that came in had — `template_suffix: null`
stays a null, and a field that was absent stays absent.

```java
public class ShopifyProductType {
    public static final String resourceType = "product";

    public static final GlobType TYPE;

    public static final LongField id;
    public static final StringField admin_graphql_api_id;
    public static final DateTimeField created_at;
    public static final DateTimeField updated_at;
    public static final StringField title;
    public static final StringField handle;
    public static final StringField vendor;
    public static final StringField body_html;
    public static final GlobArrayField<ShopifyProductOptionType> options;
    public static final GlobArrayField<ShopifyVariantType> variants;

    static {
        GlobTypeBuilder builder = GlobTypeBuilderFactory.create("ShopifyProductType");
        builder.addAnnotation(ShopifyResourceType.create(resourceType));

        id = builder.declareLongField("id", KeyField.ZERO, GraphqlName.create("legacyResourceId"));
        admin_graphql_api_id = builder.declareStringField("admin_graphql_api_id",
                FieldName.create("admin_graphql_api_id"), GraphqlName.create("id"));
        created_at = builder.declareDateTimeField("created_at", FieldName.create("created_at"));
        updated_at = builder.declareDateTimeField("updated_at", FieldName.create("updated_at"));
        title = builder.declareStringField("title");
        handle = builder.declareStringField("handle");
        vendor = builder.declareStringField("vendor");
        body_html = builder.declareStringField("body_html",
                FieldName.create("body_html"), GraphqlName.create("descriptionHtml"));
        options = builder.declareGlobArrayField("options", () -> ShopifyProductOptionType.TYPE,
                FieldName.create("options"));
        variants = builder.declareGlobArrayField("variants", () -> ShopifyVariantType.TYPE,
                ShopifyConnection.UNIQUE_GLOB);

        TYPE = builder.build();
    }
}
```

`FieldName` is core's; `ShopifyResourceType`, `GraphqlName` and `ShopifyConnection` are the application's
own, declared the same way as any Glob annotation — `create(value)` when they carry one, `UNIQUE_GLOB` when
they do not.


## ChangeSet

`ChangeSet`s serialize through the Gson adapters. Reading is two-phase: a `PreChangeSet` comes back first
and only becomes a `ChangeSet` once the caller supplies a `GlobAccessor` (`preChangeSet.resolve(accessor)`),
because resolving a change needs access to the existing globs.

## Building

```bash
mvn -o test                                    # JUnit 4 in this repo
mvn -o test -Dtest=GSonUtilsTest#globWriterTest
```

`PerfReadWriteTest` guards the hot paths.

## License

Apache License 2.0 — see <https://www.apache.org/licenses/LICENSE-2.0.txt>.

## Links

- [Globs Framework](https://globsframework.org)
- [GitHub repository](https://github.com/globsframework/globs-gson)
