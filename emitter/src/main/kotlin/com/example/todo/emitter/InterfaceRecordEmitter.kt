package com.example.todo.emitter

import arrow.core.NonEmptyList
import arrow.core.toNonEmptyListOrNull
import community.flock.wirespec.compiler.core.emit.Emitted
import community.flock.wirespec.compiler.core.emit.PackageName
import community.flock.wirespec.compiler.core.emit.plus
import community.flock.wirespec.compiler.core.parse.ast.Field
import community.flock.wirespec.compiler.core.parse.ast.Identifier
import community.flock.wirespec.compiler.core.parse.ast.Module
import community.flock.wirespec.compiler.core.parse.ast.Reference
import community.flock.wirespec.compiler.core.parse.ast.Type
import community.flock.wirespec.compiler.core.parse.ast.Endpoint
import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.integration.spring.java.emit.SpringJavaEmitter

class InterfaceRecordEmitter(
    private val _packageName: PackageName
) : SpringJavaEmitter(_packageName) {

    private val extraEmitted = mutableListOf<Emitted>()
    private val typeMappings = mutableListOf<Pair<String, String>>()
    private val typeFields = mutableMapOf<String, List<Field>>()

    private fun emitRef(ref: Reference): String {
        val method = this.javaClass.getMethod("emit", Reference::class.java)
        return method.invoke(this, ref) as String
    }

    override fun emit(type: Type, module: Module): String {
        val identifier: Identifier = type.identifier
        val name = emit(identifier)
        val recordName = "${name}Record"

        val fields = type.shape.value

        // Build interface body with accessor methods
        val accessorMethods = fields.joinToString("\n") { field ->
            val fieldName = field.identifier.value
            val fieldType = emitRef(field.reference)
            "  $fieldType $fieldName();"
        }

        // Build record fields
        val recordFields = fields.joinToString(",\n") { field ->
            val fieldName = field.identifier.value
            val fieldType = emitRef(field.reference)
            "  $fieldType $fieldName"
        }

        // Compute sub-packages
        val modelPackage = _packageName + "model"
        val recordPackage = _packageName + "record"
        val flatbuffersPackage = _packageName + "flatbuffers"

        // Generate the record file content
        val recordContent = buildString {
            appendLine("package ${recordPackage.value};")
            appendLine()
            appendLine("import ${modelPackage.value}.$name;")
            appendLine()
            appendLine("public record $recordName (")
            appendLine(recordFields)
            appendLine(") implements $name {")
            append("};")
        }

        extraEmitted.add(
            Emitted("${recordPackage.toDir()}$recordName", recordContent)
        )

        // Generate FlatBuffers class
        val flatBufferContent = generateFlatBufferClass(flatbuffersPackage.value, name, fields)
        extraEmitted.add(
            Emitted("${flatbuffersPackage.toDir()}${name}FlatBuffer", flatBufferContent)
        )

        // Track mapping for Jackson module generation
        typeMappings.add(name to recordName)
        typeFields[name] = fields

        // Return interface body
        return buildString {
            val recordPackage = _packageName + "record"
            appendLine("@com.fasterxml.jackson.databind.annotation.JsonDeserialize(as = ${recordPackage.value}.${recordName}.class)")
            appendLine("public interface $name {")
            appendLine(accessorMethods)
            append("}")
        }
    }

    private fun generateFlatBufferClass(packageName: String, typeName: String, fields: List<Field>): String {
        val className = "${typeName}FlatBuffer"
        val fieldCount = fields.size
        val modelPackage = _packageName + "model"

        return buildString {
            appendLine("package $packageName;")
            appendLine()
            appendLine("import java.nio.ByteBuffer;")
            appendLine("import java.nio.ByteOrder;")
            appendLine("import com.google.flatbuffers.*;")
            appendLine("import ${modelPackage.value}.$typeName;")
            appendLine()
            appendLine("public final class $className extends Table implements $typeName {")
            appendLine()

            // getRootAs factory
            appendLine("  public static $className getRootAs$className(ByteBuffer bb) {")
            appendLine("    bb.order(ByteOrder.LITTLE_ENDIAN);")
            appendLine("    return (new $className()).__assign(bb.getInt(bb.position()) + bb.position(), bb);")
            appendLine("  }")
            appendLine()

            // __assign
            appendLine("  public $className __assign(int i, ByteBuffer bb) {")
            appendLine("    __reset(i, bb);")
            appendLine("    return this;")
            appendLine("  }")
            appendLine()

            // Accessor methods
            fields.forEachIndexed { index, field ->
                val fieldName = field.identifier.value
                val fbInfo = getFlatBufferFieldInfo(field.reference)
                val vtableOffset = 4 + 2 * index

                appendLine("  ${fbInfo.accessorComment(fieldName)}")
                appendLine("  ${fbInfo.accessorMethod(fieldName, vtableOffset)}")
                appendLine()
            }

            // Static create method
            val createParams = fields.mapIndexed { index, field ->
                val fieldName = field.identifier.value
                val fbInfo = getFlatBufferFieldInfo(field.reference)
                "${fbInfo.createParamType} $fieldName"
            }.joinToString(",\n      ")

            appendLine("  public static int create$className(FlatBufferBuilder builder,")
            appendLine("      $createParams) {")
            appendLine("    builder.startTable($fieldCount);")
            fields.forEachIndexed { index, field ->
                val fieldName = field.identifier.value
                appendLine("    add${fieldName.replaceFirstChar { it.uppercase() }}(builder, $fieldName);")
            }
            appendLine("    return end$className(builder);")
            appendLine("  }")
            appendLine()

            // start method
            appendLine("  public static void start$className(FlatBufferBuilder builder) {")
            appendLine("    builder.startTable($fieldCount);")
            appendLine("  }")
            appendLine()

            // add methods for each field
            fields.forEachIndexed { index, field ->
                val fieldName = field.identifier.value
                val methodName = "add${fieldName.replaceFirstChar { it.uppercase() }}"
                val fbInfo = getFlatBufferFieldInfo(field.reference)

                appendLine("  ${fbInfo.addMethod(methodName, index)}")
                appendLine()
            }

            // end method
            appendLine("  public static int end$className(FlatBufferBuilder builder) {")
            appendLine("    return builder.endTable();")
            appendLine("  }")

            append("}")
        }
    }

    private data class FlatBufferFieldInfo(
        val interfaceReturnType: String, // boxed type matching the interface (Long, Boolean, String)
        val createParamType: String,
        val readExpr: (String, Int) -> String, // (bb_pos_expr, vtableOffset) -> read expression
        val defaultValue: String,
        val builderMethod: String, // addLong, addOffset, addBoolean, etc.
        val builderDefault: String,
        val isOffset: Boolean = false
    ) {
        fun accessorComment(fieldName: String) = "// $fieldName"

        fun accessorMethod(fieldName: String, vtableOffset: Int): String {
            val readCode = readExpr("o + bb_pos", vtableOffset)
            return "public $interfaceReturnType $fieldName() { int o = __offset($vtableOffset); return o != 0 ? $readCode : $defaultValue; }"
        }

        fun addMethod(methodName: String, fieldIndex: Int): String {
            return if (isOffset) {
                "public static void $methodName(FlatBufferBuilder builder, $createParamType ${methodName.replaceFirstChar { it.lowercase() }}) { builder.addOffset($fieldIndex, ${methodName.replaceFirstChar { it.lowercase() }}, 0); }"
            } else {
                "public static void $methodName(FlatBufferBuilder builder, $createParamType ${methodName.replaceFirstChar { it.lowercase() }}) { builder.$builderMethod($fieldIndex, ${methodName.replaceFirstChar { it.lowercase() }}, $builderDefault); }"
            }
        }
    }

    private fun getFlatBufferFieldInfo(ref: Reference): FlatBufferFieldInfo {
        return when (ref) {
            is Reference.Primitive -> {
                val typeName = ref.type::class.simpleName ?: ""
                when (typeName) {
                    "Integer" -> FlatBufferFieldInfo(
                        interfaceReturnType = "Long",
                        createParamType = "long",
                        readExpr = { pos, _ -> "bb.getLong($pos)" },
                        defaultValue = "0L",
                        builderMethod = "addLong",
                        builderDefault = "0L"
                    )
                    "Number" -> FlatBufferFieldInfo(
                        interfaceReturnType = "Double",
                        createParamType = "double",
                        readExpr = { pos, _ -> "bb.getDouble($pos)" },
                        defaultValue = "0.0",
                        builderMethod = "addDouble",
                        builderDefault = "0.0"
                    )
                    "Boolean" -> FlatBufferFieldInfo(
                        interfaceReturnType = "Boolean",
                        createParamType = "boolean",
                        readExpr = { pos, _ -> "0 != bb.get($pos)" },
                        defaultValue = "false",
                        builderMethod = "addBoolean",
                        builderDefault = "false"
                    )
                    "String" -> FlatBufferFieldInfo(
                        interfaceReturnType = "String",
                        createParamType = "int",
                        readExpr = { pos, _ -> "__string($pos)" },
                        defaultValue = "null",
                        builderMethod = "addOffset",
                        builderDefault = "0",
                        isOffset = true
                    )
                    "Bytes" -> FlatBufferFieldInfo(
                        interfaceReturnType = "int",
                        createParamType = "int",
                        readExpr = { pos, _ -> "__vector_len($pos)" },
                        defaultValue = "0",
                        builderMethod = "addOffset",
                        builderDefault = "0",
                        isOffset = true
                    )
                    else -> FlatBufferFieldInfo(
                        interfaceReturnType = "Long",
                        createParamType = "long",
                        readExpr = { pos, _ -> "bb.getLong($pos)" },
                        defaultValue = "0L",
                        builderMethod = "addLong",
                        builderDefault = "0L"
                    )
                }
            }
            is Reference.Custom -> {
                val customName = ref.value + "FlatBuffer"
                FlatBufferFieldInfo(
                    interfaceReturnType = customName,
                    createParamType = "int",
                    readExpr = { pos, _ -> "(new $customName()).__assign(__indirect($pos), bb)" },
                    defaultValue = "null",
                    builderMethod = "addOffset",
                    builderDefault = "0",
                    isOffset = true
                )
            }
            is Reference.Iterable -> {
                // Vector of the inner type
                val innerInfo = getFlatBufferFieldInfo(ref.reference)
                FlatBufferFieldInfo(
                    interfaceReturnType = "int",
                    createParamType = "int",
                    readExpr = { pos, _ -> "__vector_len($pos)" },
                    defaultValue = "0",
                    builderMethod = "addOffset",
                    builderDefault = "0",
                    isOffset = true
                )
            }
            else -> FlatBufferFieldInfo(
                interfaceReturnType = "Long",
                createParamType = "long",
                readExpr = { pos, _ -> "bb.getLong($pos)" },
                defaultValue = "0L",
                builderMethod = "addLong",
                builderDefault = "0L"
            )
        }
    }

    private fun generateContentTypeContext(): Emitted {
        val modelPackage = _packageName + "model"
        val content = buildString {
            appendLine("package ${modelPackage.value};")
            appendLine()
            appendLine("public final class ContentTypeContext {")
            appendLine()
            appendLine("  private static final ThreadLocal<String> REQUEST_CONTENT_TYPE = new ThreadLocal<>();")
            appendLine("  private static final ThreadLocal<String> ACCEPT_CONTENT_TYPE = new ThreadLocal<>();")
            appendLine()
            appendLine("  private ContentTypeContext() {}")
            appendLine()
            appendLine("  public static String getRequestContentType() {")
            appendLine("    return REQUEST_CONTENT_TYPE.get();")
            appendLine("  }")
            appendLine()
            appendLine("  public static void setRequestContentType(String contentType) {")
            appendLine("    REQUEST_CONTENT_TYPE.set(contentType);")
            appendLine("  }")
            appendLine()
            appendLine("  public static String getAcceptContentType() {")
            appendLine("    return ACCEPT_CONTENT_TYPE.get();")
            appendLine("  }")
            appendLine()
            appendLine("  public static void setAcceptContentType(String contentType) {")
            appendLine("    ACCEPT_CONTENT_TYPE.set(contentType);")
            appendLine("  }")
            appendLine()
            appendLine("  public static void clear() {")
            appendLine("    REQUEST_CONTENT_TYPE.remove();")
            appendLine("    ACCEPT_CONTENT_TYPE.remove();")
            appendLine("  }")
            append("}")
        }
        return Emitted("${modelPackage.toDir()}ContentTypeContext", content)
    }

    private fun findArrayResponseTypes(module: Module): Set<String> {
        val arrayTypes = mutableSetOf<String>()
        for (statement in module.statements) {
            if (statement is Endpoint) {
                for (response in statement.responses) {
                    val ref = response.content?.reference
                    if (ref is Reference.Iterable) {
                        val inner = ref.reference
                        if (inner is Reference.Custom) {
                            arrayTypes.add(inner.value)
                        }
                    }
                }
            }
        }
        return arrayTypes
    }

    private fun generateListFlatBuffer(packageName: String, typeName: String): String {
        val className = "${typeName}ListFlatBuffer"
        val itemClassName = "${typeName}FlatBuffer"

        return buildString {
            appendLine("package $packageName;")
            appendLine()
            appendLine("import java.nio.ByteBuffer;")
            appendLine("import java.nio.ByteOrder;")
            appendLine("import com.google.flatbuffers.*;")
            appendLine()
            appendLine("public final class $className extends Table {")
            appendLine()

            // getRootAs factory
            appendLine("  public static $className getRootAs$className(ByteBuffer bb) {")
            appendLine("    bb.order(ByteOrder.LITTLE_ENDIAN);")
            appendLine("    return (new $className()).__assign(bb.getInt(bb.position()) + bb.position(), bb);")
            appendLine("  }")
            appendLine()

            // __assign
            appendLine("  public $className __assign(int i, ByteBuffer bb) {")
            appendLine("    __reset(i, bb);")
            appendLine("    return this;")
            appendLine("  }")
            appendLine()

            // items(int j) accessor - vtable offset 4 for the vector
            appendLine("  public $itemClassName items(int j) {")
            appendLine("    int o = __offset(4);")
            appendLine("    return o != 0 ? (new $itemClassName()).__assign(__indirect(__vector(o) + j * 4), bb) : null;")
            appendLine("  }")
            appendLine()

            // itemsLength()
            appendLine("  public int itemsLength() {")
            appendLine("    int o = __offset(4);")
            appendLine("    return o != 0 ? __vector_len(o) : 0;")
            appendLine("  }")
            appendLine()

            // createItemsVector
            appendLine("  public static int createItemsVector(FlatBufferBuilder builder, int[] data) {")
            appendLine("    builder.startVector(4, data.length, 4);")
            appendLine("    for (int i = data.length - 1; i >= 0; i--) builder.addOffset(data[i]);")
            appendLine("    return builder.endVector();")
            appendLine("  }")
            appendLine()

            // create method
            appendLine("  public static int create$className(FlatBufferBuilder builder, int itemsOffset) {")
            appendLine("    builder.startTable(1);")
            appendLine("    addItems(builder, itemsOffset);")
            appendLine("    return end$className(builder);")
            appendLine("  }")
            appendLine()

            // start method
            appendLine("  public static void start$className(FlatBufferBuilder builder) {")
            appendLine("    builder.startTable(1);")
            appendLine("  }")
            appendLine()

            // addItems
            appendLine("  public static void addItems(FlatBufferBuilder builder, int itemsOffset) {")
            appendLine("    builder.addOffset(0, itemsOffset, 0);")
            appendLine("  }")
            appendLine()

            // end method
            appendLine("  public static int end$className(FlatBufferBuilder builder) {")
            appendLine("    return builder.endTable();")
            appendLine("  }")

            append("}")
        }
    }

    private fun generateFlatBufferSerializer(module: Module): Emitted {
        val flatbuffersPackage = _packageName + "flatbuffers"
        val modelPackage = _packageName + "model"
        val recordPackage = _packageName + "record"
        val arrayTypes = findArrayResponseTypes(module)

        val content = buildString {
            appendLine("package ${flatbuffersPackage.value};")
            appendLine()
            appendLine("import java.nio.ByteBuffer;")
            appendLine("import java.util.ArrayList;")
            appendLine("import java.util.List;")
            appendLine("import com.google.flatbuffers.FlatBufferBuilder;")

            // Import all model types and records
            for ((typeName, _) in typeMappings) {
                appendLine("import ${modelPackage.value}.$typeName;")
                appendLine("import ${recordPackage.value}.${typeName}Record;")
            }

            appendLine()
            appendLine("public final class FlatBufferSerializer {")
            appendLine()
            appendLine("  private FlatBufferSerializer() {}")
            appendLine()

            // Generate serialize/deserialize methods per type
            for ((typeName, _) in typeMappings) {
                val fields = typeFields[typeName] ?: continue
                val fbClass = "${typeName}FlatBuffer"
                val recordClass = "${typeName}Record"

                // serialize method
                appendLine("  public static byte[] serialize$typeName($typeName obj) {")
                appendLine("    FlatBufferBuilder builder = new FlatBufferBuilder(256);")

                // Pre-create string offsets
                val stringFields = fields.filter { field ->
                    val ref = field.reference
                    ref is Reference.Primitive && (ref.type::class.simpleName ?: "") == "String"
                }
                for (field in stringFields) {
                    val fieldName = field.identifier.value
                    appendLine("    int ${fieldName}Offset = builder.createString(obj.$fieldName() != null ? obj.$fieldName() : \"\");")
                }

                // Build create call params
                val createArgs = fields.map { field ->
                    val fieldName = field.identifier.value
                    val ref = field.reference
                    if (ref is Reference.Primitive) {
                        val primType = ref.type::class.simpleName ?: ""
                        when (primType) {
                            "String" -> "${fieldName}Offset"
                            "Integer" -> "obj.$fieldName()"
                            "Boolean" -> "obj.$fieldName()"
                            "Number" -> "obj.$fieldName()"
                            else -> "obj.$fieldName()"
                        }
                    } else {
                        "obj.$fieldName()"
                    }
                }.joinToString(", ")

                appendLine("    int offset = $fbClass.create$fbClass(builder, $createArgs);")
                appendLine("    builder.finish(offset);")
                appendLine("    ByteBuffer buf = builder.dataBuffer();")
                appendLine("    byte[] result = new byte[buf.remaining()];")
                appendLine("    buf.get(result);")
                appendLine("    return result;")
                appendLine("  }")
                appendLine()

                // deserialize method
                appendLine("  public static $typeName deserialize$typeName(byte[] bytes) {")
                appendLine("    ByteBuffer buf = ByteBuffer.wrap(bytes);")
                appendLine("    $fbClass fb = $fbClass.getRootAs$fbClass(buf);")

                val recordArgs = fields.joinToString(", ") { field ->
                    val fieldName = field.identifier.value
                    "fb.$fieldName()"
                }

                appendLine("    return new $recordClass($recordArgs);")
                appendLine("  }")
                appendLine()

                // List serialize/deserialize only for array types
                if (typeName in arrayTypes) {
                    val listFbClass = "${typeName}ListFlatBuffer"

                    // serializeList
                    appendLine("  public static byte[] serialize${typeName}List(List<$typeName> list) {")
                    appendLine("    FlatBufferBuilder builder = new FlatBufferBuilder(256);")
                    appendLine("    int[] offsets = new int[list.size()];")
                    appendLine("    for (int i = 0; i < list.size(); i++) {")
                    appendLine("      $typeName item = list.get(i);")

                    // Pre-create string offsets for each item
                    for (field in stringFields) {
                        val fieldName = field.identifier.value
                        appendLine("      int ${fieldName}Offset = builder.createString(item.$fieldName() != null ? item.$fieldName() : \"\");")
                    }

                    val itemCreateArgs = fields.map { field ->
                        val fieldName = field.identifier.value
                        val ref = field.reference
                        if (ref is Reference.Primitive) {
                            val primType = ref.type::class.simpleName ?: ""
                            when (primType) {
                                "String" -> "${fieldName}Offset"
                                "Integer" -> "item.$fieldName()"
                                "Boolean" -> "item.$fieldName()"
                                "Number" -> "item.$fieldName()"
                                else -> "item.$fieldName()"
                            }
                        } else {
                            "item.$fieldName()"
                        }
                    }.joinToString(", ")

                    appendLine("      offsets[i] = $fbClass.create$fbClass(builder, $itemCreateArgs);")
                    appendLine("    }")
                    appendLine("    int itemsVector = $listFbClass.createItemsVector(builder, offsets);")
                    appendLine("    int root = $listFbClass.create$listFbClass(builder, itemsVector);")
                    appendLine("    builder.finish(root);")
                    appendLine("    ByteBuffer buf = builder.dataBuffer();")
                    appendLine("    byte[] result = new byte[buf.remaining()];")
                    appendLine("    buf.get(result);")
                    appendLine("    return result;")
                    appendLine("  }")
                    appendLine()

                    // deserializeList
                    appendLine("  public static List<$typeName> deserialize${typeName}List(byte[] bytes) {")
                    appendLine("    ByteBuffer buf = ByteBuffer.wrap(bytes);")
                    appendLine("    $listFbClass listFb = $listFbClass.getRootAs$listFbClass(buf);")
                    appendLine("    List<$typeName> result = new ArrayList<>();")
                    appendLine("    for (int i = 0; i < listFb.itemsLength(); i++) {")
                    appendLine("      $fbClass fb = listFb.items(i);")

                    val listRecordArgs = fields.joinToString(", ") { field ->
                        val fieldName = field.identifier.value
                        "fb.$fieldName()"
                    }

                    appendLine("      result.add(new $recordClass($listRecordArgs));")
                    appendLine("    }")
                    appendLine("    return result;")
                    appendLine("  }")
                    appendLine()
                }
            }

            append("}")
        }

        return Emitted("${flatbuffersPackage.toDir()}FlatBufferSerializer", content)
    }

    override fun emit(module: Module, logger: Logger): NonEmptyList<Emitted> {
        extraEmitted.clear()
        typeMappings.clear()
        typeFields.clear()

        val result = super.emit(module, logger)

        // Generate Jackson module if we have type mappings
        if (typeMappings.isNotEmpty()) {
            val modelPackage = _packageName + "model"
            val recordPackage = _packageName + "record"

            val imports = typeMappings.joinToString("\n") { (iface, record) ->
                "import ${modelPackage.value}.$iface;\nimport ${recordPackage.value}.$record;"
            }

            val mappings = typeMappings.joinToString("\n") { (iface, record) ->
                "    addAbstractTypeMapping(${iface}.class, ${record}.class);"
            }

            val jacksonModuleContent = buildString {
                appendLine("package ${modelPackage.value};")
                appendLine()
                appendLine("import com.fasterxml.jackson.databind.module.SimpleModule;")
                appendLine("import org.springframework.stereotype.Component;")
                appendLine(imports)
                appendLine()
                appendLine("@Component")
                appendLine("public class WirespecJacksonModule extends SimpleModule {")
                appendLine("  public WirespecJacksonModule() {")
                appendLine("    super(\"WirespecJacksonModule\");")
                appendLine(mappings)
                appendLine("  }")
                append("}")
            }

            extraEmitted.add(
                Emitted("${modelPackage.toDir()}WirespecJacksonModule", jacksonModuleContent)
            )
        }

        // Generate ContentTypeContext utility class (once per module)
        extraEmitted.add(generateContentTypeContext())

        // Generate ListFlatBuffer wrappers for array response types
        val arrayTypes = findArrayResponseTypes(module)
        val flatbuffersPackage = _packageName + "flatbuffers"
        for (typeName in arrayTypes) {
            val listFbContent = generateListFlatBuffer(flatbuffersPackage.value, typeName)
            extraEmitted.add(
                Emitted("${flatbuffersPackage.toDir()}${typeName}ListFlatBuffer", listFbContent)
            )
        }

        // Generate FlatBufferSerializer
        extraEmitted.add(generateFlatBufferSerializer(module))

        val allExtra = extraEmitted.toList()
        extraEmitted.clear()
        typeMappings.clear()
        typeFields.clear()

        return if (allExtra.isEmpty()) {
            result
        } else {
            val allFiles = result.toList() + allExtra
            allFiles.toNonEmptyListOrNull() ?: result
        }
    }
}
