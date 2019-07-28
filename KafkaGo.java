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

    public static String generate() {
        final StringBuilder b = new StringBuilder();

        b.append("// CODE GENERATED BY kafka-type-gen; DO NOT EDIT!\n\n");
        b.append("package kafka\n\n");

        for (ApiKeys key : ApiKeys.values()) {
            if (key.clusterAction) {
              continue;
            }

            Schema[] requests = key.requestSchemas;
            for (int version = 0; version < requests.length; version++) {
                Schema schema = requests[version];
                if (schema != null) {
                    String baseName = key.name + "RequestV" + version;
                    appendSchema(b, schema, baseName);
                }
            }

            Schema[] responses = key.responseSchemas;
            for (int version = 0; version < responses.length; version++) {
                Schema schema = responses[version];
                if (schema != null) {
                    String baseName = key.name + "ResponseV" + version;
                    appendSchema(b, schema, baseName);
                }
            }
        }

        return b.toString();
    }

    private static void appendSchema(StringBuilder b, Schema schema, String baseName) {
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
                subType = ((ArrayOf) field.def.type).type();
                fieldType = subType.toString();
                String goType = toGoType(fieldType);
                if (goType != fieldType) {
                    fieldType = goType;
                    subType = null;
                } else {
                    fieldType = trimPlural(baseName + fieldName);
                }
            } else if (field.def.type instanceof Schema) {
                fieldType = baseName + fieldName;
                subType = field.def.type;
            } else {
                fieldType = toGoType(field.def.type.toString());
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

        if (subTypes.isEmpty()) {
            return;
        }

        for (Map.Entry<String, Type> entry: subTypes.entrySet()) {
            appendSchema(b, (Schema) entry.getValue(), entry.getKey());
        }
    }

    private static String toGoType(String kafkaType) {
        switch (kafkaType) {
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
            return "[][]byte";
        case "ARRAY(INT32)":
            return "[]int32";
        default:
            return kafkaType;
        }
    }

    private static String toGoName(String s) {
        return CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, s);
    }

    private static String trimPlural(String s) {
        if (s.endsWith("s")) {
            return s.substring(0, s.length() - 1);
        }
        return s;
    }
}
