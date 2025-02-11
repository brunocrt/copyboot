import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CopyBook2PojoGen {
    public static void main(String[] args) {
        String copybookFilePath = "path_to_copybook.txt";  // Provide your file path
        try {
            String pojoClass = generatePojoFromCopybook(copybookFilePath);
            System.out.println(pojoClass);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String generatePojoFromCopybook(String copybookFilePath) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(copybookFilePath));
        List<Field> fields = new ArrayList<>();
        String line;
        String currentGroup = "";

        while ((line = reader.readLine()) != null) {
            if (line.trim().isEmpty()) continue;

            String[] parts = line.trim().split("\\s+");

            // Ensure there are enough parts for processing
            if (parts.length < 2) {
                continue; // Skip invalid lines that don't have enough parts
            }

            // Process group (nested structure) or field definition
            if (line.trim().startsWith("05") || line.trim().startsWith("10")) {
                String fieldName = parts[1].replace(".", "");  // Remove the period at the end
                String dataType = parts.length > 2 ? parts[2] : "";  // Handle cases where no data type is defined

                // Ensure valid field name and data type
                if (fieldName.contains("-")) {
                    fieldName = fieldName.replace("-", "_");  // Adjust for Java naming conventions
                }

                String javaType = determineJavaType(dataType);
                fields.add(new Field(fieldName, javaType, currentGroup));
            }

            // Detect group starting lines (e.g., CONTACTS)
            if (line.trim().startsWith("01")) {
                String[] groupParts = line.trim().split("\\s+");
                if (groupParts.length > 1) {
                    String groupName = groupParts[1].replace("-", "_");  // Adjust for Java naming conventions
                    currentGroup = groupName;
                }
            }
        }

        reader.close();
        return generatePojoClass(fields);
    }

    private static String determineJavaType(String cobolType) {
        if (cobolType.startsWith("X")) {
            return "String";  // COBOL 'X' is usually a String
        } else if (cobolType.startsWith("9")) {
            return "int";     // COBOL '9' is usually an integer
        } else if (cobolType.startsWith("S")) {
            return "double";  // COBOL 'S' could be a signed number (double here)
        }
        return "String";  // Default to String for unknown types
    }

    private static String generatePojoClass(List<Field> fields) {
        StringBuilder pojoClass = new StringBuilder("public class GeneratedPojo {\n\n");

        // Generate fields
        for (Field field : fields) {
            pojoClass.append("    private ").append(field.getJavaType()).append(" ").append(field.getFieldName()).append(";\n");
        }

        // Generate getters and setters
        for (Field field : fields) {
            pojoClass.append("\n    public ").append(field.getJavaType()).append(" get")
                    .append(capitalize(field.getFieldName())).append("() {\n")
                    .append("        return ").append(field.getFieldName()).append(";\n")
                    .append("    }\n");
            pojoClass.append("\n    public void set")
                    .append(capitalize(field.getFieldName())).append("(")
                    .append(field.getJavaType()).append(" ")
                    .append(field.getFieldName()).append(") {\n")
                    .append("        this.").append(field.getFieldName()).append(" = ")
                    .append(field.getFieldName()).append(";\n")
                    .append("    }\n");
        }

        pojoClass.append("\n}");
        return pojoClass.toString();
    }

    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    static class Field {
        private String fieldName;
        private String javaType;
        private String parentGroup;

        public Field(String fieldName, String javaType, String parentGroup) {
            this.fieldName = fieldName;
            this.javaType = javaType;
            this.parentGroup = parentGroup;
        }

        public String getFieldName() {
            return fieldName;
        }

        public String getJavaType() {
            return javaType;
        }

        public String getParentGroup() {
            return parentGroup;
        }
    }
}
