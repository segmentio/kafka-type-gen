import com.google.common.base.CaseFormat;

import org.apache.kafka.common.protocol.types.ArrayOf;
import org.apache.kafka.common.protocol.types.BoundField;
import org.apache.kafka.common.protocol.types.Schema;
import org.apache.kafka.common.protocol.types.Type;
import org.apache.kafka.common.protocol.Protocol;
import org.apache.kafka.common.protocol.ApiKeys;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class KafkaGo {

    static String generate() {
        final StringBuilder b = new StringBuilder();

        b.append("// CODE GENERATED BY kafka-type-gen; DO NOT EDIT!\n\n");
        b.append("package types\n\n");

        b.append("type APIKey int16\n\n");
        b.append("const (\n");

        for (ApiKeys key: ApiKeys.values()) {
            if (!key.clusterAction) {
                b.append("\t");
                b.append(key.name);
                b.append(" = APIKey(");
                b.append(key.id);
                b.append(")\n");
            }
        }

        b.append(")\n\n");

        b.append("type APIVersion int16\n\n");

        b.append("var MaxAPIVersion = [...]APIVersion{\n");
        for (ApiKeys key: ApiKeys.values()) {
            if (!key.clusterAction) {
                b.append(key.name);
                b.append(": ");
                b.append(key.requestSchemas.length - 1);
                b.append(",\n");
            }
        }
        b.append("}\n\n");

        for (ApiKeys key: ApiKeys.values()) {
            if (key.clusterAction) {
              continue;
            }

            Schema[] requests = key.requestSchemas;
            for (int version = 0; version < requests.length; version++) {
                appendSchema(b, requests[version], baseRequestNameOf(key, version));
            }

            b.append("func New" + key.name + "Request(apiVersion APIVersion) Request {\n");
            b.append("\tswitch apiVersion {\n");

            for (int version = 0; version < requests.length; version++) {
                b.append("\tcase ");
                b.append(version);
                b.append(":\n");
                b.append("\t\treturn new(");
                b.append(baseRequestNameOf(key, version));
                b.append(")\n");
            }

            b.append("\tdefault:\n");
            b.append("\t\treturn nil\n");
            b.append("\t}\n");
            b.append("}\n");

            Schema[] responses = key.responseSchemas;
            for (int version = 0; version < responses.length; version++) {
                appendSchema(b, responses[version], baseResponseNameOf(key, version));
            }

            b.append("func New" + key.name + "Response(apiVersion APIVersion) Response {\n");
            b.append("\tswitch apiVersion {\n");

            for (int version = 0; version < responses.length; version++) {
                b.append("\tcase ");
                b.append(version);
                b.append(":\n");
                b.append("\t\treturn new(");
                b.append(baseResponseNameOf(key, version));
                b.append(")\n");
            }

            b.append("\tdefault:\n");
            b.append("\t\treturn nil\n");
            b.append("\t}\n");
            b.append("}\n");
        }

        b.append("\n");

        b.append("type Marshaler interface { Marshal(*Writer) }\n\n");
        b.append("type Unmarshaler interface { Ummarshaler(*Reader) }\n\n");
        b.append("type Request interface { Marshaler; Unmarshaler }\n\n");
        b.append("type Response interface {Marshaler; Unmarshaler }\n\n");

        b.append("func NewRequest(apiKey APIKey, apiVersion APIVersion) Request {\n");
        b.append("\tswitch apiKey {\n");

        for (ApiKeys key: ApiKeys.values()) {
            if (!key.clusterAction) {
                b.append("\tcase ");
                b.append(key.name);
                b.append(":\n");
                b.append("\t\treturn New" + key.name + "Request(apiVersion)\n");
            }
        }

        b.append("\tdefault:\n");
        b.append("\t\treturn nil\n");
        b.append("\t}\n");
        b.append("}\n\n");

        b.append("func NewResponse(apiKey APIKey, apiVersion APIVersion) Response {\n");
        b.append("\tswitch apiKey {\n");

        for (ApiKeys key: ApiKeys.values()) {
            if (!key.clusterAction) {
                b.append("\tcase ");
                b.append(key.name);
                b.append(":\n");
                b.append("\t\treturn New" + key.name + "Response(apiVersion)\n");
            }
        }

        b.append("\tdefault:\n");
        b.append("\t\treturn nil\n");
        b.append("\t}\n");
        b.append("}\n\n");

        return b.toString();
    }

    static String baseRequestNameOf(ApiKeys key, int version) {
        return baseNameOf(key, version, "Request");
    }

    static String baseResponseNameOf(ApiKeys key, int version) {
        return baseNameOf(key, version, "Response");
    }

    static String baseNameOf(ApiKeys key, int version, String type) {
        return key.name + type + "V" + version;
    }

    static void appendSchema(StringBuilder b, Schema schema, String baseName) {
        final Map<String, Type> subTypes = new LinkedHashMap<>();

        b.append("type ");
        b.append(baseName);
        b.append(" struct {\n");

        for (BoundField field: schema.fields()) {
            String fieldName = toGoName(field.def.name);
            String fieldType = "";
            String fieldMod = "";
            Type subType = null;

            if (field.def.type instanceof ArrayOf) {
                fieldMod = "[]";
                fieldType = toGoSubType((ArrayOf) field.def.type, baseName, fieldName);
                subType = ((ArrayOf) field.def.type).type();
                if (toGoType(subType) != "") {
                    subType = null;
                }
            } else if (field.def.type instanceof Schema) {
                fieldType = baseName + fieldName;
                subType = field.def.type;
            } else {
                fieldType = toGoType(field.def.type);
            }

            if (subType != null) {
                subTypes.put(fieldType, subType);
            }

            b.append("\t");
            b.append(fieldName);
            b.append(" ");
            b.append(fieldMod);
            b.append(fieldType);
            b.append("\n");
        }

        b.append("}\n\n");
        int varIndex;

        // func (rx *TYPE) Marshal(w *Writer)
        b.append("func (rx *");
        b.append(baseName);
        b.append(") Marshal(w *Writer) {\n");

        for (BoundField field: schema.fields()) {
            String fieldName = toGoName(field.def.name);

            if (field.def.type instanceof ArrayOf) {
                Type subType = ((ArrayOf) field.def.type).type();
                String fieldType = toGoSubType((ArrayOf) field.def.type, baseName, fieldName);

                // w.WriteInt32(int32(len(rx.FIELD)))
                b.append("w.WriteInt32(int32(len(rx.");
                b.append(fieldName);
                b.append(")))\n");

                // for i := range rx.FIELD
                b.append("for i := range rx.");
                b.append(fieldName);
                b.append(" {\n");
                // w.WriteTYPE(rx.FIELD[i]) | rx.FIELD[i].Marshal(w)
                String writeMethod = toWriteMethod(subType);
                if (writeMethod != "") {
                    b.append("w.");
                    b.append(writeMethod);
                    b.append("(rx.");
                    b.append(fieldName);
                    b.append("[i])\n");
                } else {
                    b.append("rx.");
                    b.append(fieldName);
                    b.append("[i].Marshal(w)\n");
                }
                b.append("}\n");

            } else if (field.def.type instanceof Schema) {
                // rx.FIELD.Marshal(w)
                b.append("rx.");
                b.append(fieldName);
                b.append(".Marshal(w)\n");
            } else {
                // w.WriteTYPE(rx.FIELD)
                b.append("w.");
                b.append(toWriteMethod(field.def.type));
                b.append("(rx.");
                b.append(fieldName);
                b.append(")\n");
            }
        }
        b.append("}\n\n");

        // func(rx *TYPE) Unmarshal(r *Reader)
        b.append("func (rx *");
        b.append(baseName);
        b.append(") Unmarshal(r *Reader) {\n");
        varIndex = 0;

        for (BoundField field: schema.fields()) {
            String fieldName = toGoName(field.def.name);

            if (field.def.type instanceof ArrayOf) {
                Type subType = ((ArrayOf) field.def.type).type();
                String fieldType = toGoSubType((ArrayOf) field.def.type, baseName, fieldName);
                String nVar = "n" + varIndex;
                varIndex++;
                // n := int(r.ReadInt32())
                b.append(nVar);
                b.append(" := int(r.ReadInt32())\n");

                // rx.FIELD = make([]TYPE, n)
                b.append("rx.");
                b.append(fieldName);
                b.append(" = make([]");
                b.append(fieldType);
                b.append(", ");
                b.append(nVar);
                b.append(")\n");

                b.append("for i := 0; i < n; i++ {\n");
                // rx.FIELD[i] = r.ReadTYPE() | rx.FIELD[i].Unmarshal(r)
                b.append("rx.");
                b.append(fieldName);
                b.append("[i]");
                String readMethod = toReadMethod(subType);
                if (readMethod != "") {
                    b.append(" = r.");
                    b.append(readMethod);
                } else {
                    b.append(".Unmarshal(r)\n");
                }
                b.append("}\n");

            } else if (field.def.type instanceof Schema) {
                // rx.FIELD.Unmarshal(r)
                b.append("rx.");
                b.append(fieldName);
                b.append(".Unmarshal(r)\n");
            } else {
                // rx.FIELD = r.ReadTYPE()
                b.append("rx.");
                b.append(fieldName);
                b.append(" = r.");
                b.append(toReadMethod(field.def.type));
                b.append("()\n");
            }
        }
        b.append("}\n\n");

        if (subTypes.isEmpty()) {
            return;
        }

        for (Map.Entry<String, Type> entry: subTypes.entrySet()) {
            appendSchema(b, (Schema) entry.getValue(), entry.getKey());
        }
    }

    static String toGoName(String s) {
        return CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, s).replace("'", "");
    }

    static String toReadMethod(Type kafkaType) {
        switch (kafkaType.toString()) {
        case "BOOLEAN":
            return "ReadBool";
        case "BYTES":
            return "ReadBytes";
        case "INT8":
            return "ReadInt8";
        case "INT16":
            return "ReadInt16";
        case "INT32":
            return "ReadInt32";
        case "INT64":
            return "ReadInt64";
        case "STRING":
            return "ReadString";
        case "NULLABLE_STRING":
            return "ReadString";
        case "RECORDS":
            return "ReadRecords";
        case "ARRAY(INT32)":
            return "ReadArrayInt32";
        default:
            return "";
        }
    }

    static String toWriteMethod(Type kafkaType) {
        switch (kafkaType.toString()) {
        case "BOOLEAN":
            return "ReadBool";
        case "BYTES":
            return "WriteBytes";
        case "INT8":
            return "WriteInt8";
        case "INT16":
            return "WriteInt16";
        case "INT32":
            return "WriteInt32";
        case "INT64":
            return "WriteInt64";
        case "STRING":
            return "WriteString";
        case "NULLABLE_STRING":
            return "WriteString";
        case "RECORDS":
            return "WriteRecords";
        case "ARRAY(INT32)":
            return "WriteArrayInt32";
        default:
            return "";
        }
    }

    static String toGoType(Type kafkaType) {
        switch (kafkaType.toString()) {
        case "BOOLEAN":
            return "bool";
        case "BYTES":
            return "[]byte";
        case "INT8":
            return "int8";
        case "INT16":
            return "int16";
        case "INT32":
            return "int32";
        case "INT64":
            return "int64";
        case "STRING":
            return "string";
        case "NULLABLE_STRING":
            return "NullableString";
        case "RECORDS":
            return "Records";
        case "ARRAY(INT32)":
            return "[]int32";
        default:
            return "";
        }
    }

    static String toGoSubType(ArrayOf type, String baseName, String fieldName) {
        Type subType = type.type();
        String goType = toGoType(subType);
        if (goType != "") {
            return goType;
        }
        return trimPlural(baseName + fieldName);
    }

    static String trimPlural(String s) {
        if (s.endsWith("s")) {
            return s.substring(0, s.length() - 1);
        }
        return s;
    }

}