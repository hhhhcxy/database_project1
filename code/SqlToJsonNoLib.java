import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

public class SqlToJsonNoLib {

    public static void main(String[] args) throws IOException {
        String inputPath = "D:\\collage class\\CS213_database\\project1\\database_project1\\data_for_file\\flights.sql";
        String outputPath = "D:\\collage class\\CS213_database\\project1\\database_project1\\data_for_file\\flights.json";

        String sql = Files.readString(Paths.get(inputPath));

        // 1️⃣ 提取表名
        Matcher tableMatcher = Pattern.compile("create table\\s+(\\w+)\\s*\\(", Pattern.CASE_INSENSITIVE).matcher(sql);
        String tableName = tableMatcher.find() ? tableMatcher.group(1) : "unknown_table";

        // 2️⃣ 提取列名
        Matcher colsMatcher = Pattern.compile("create table.*?\\((.*?)\\);", Pattern.DOTALL | Pattern.CASE_INSENSITIVE).matcher(sql);
        List<String> columns = new ArrayList<>();
        if (colsMatcher.find()) {
            String cols = colsMatcher.group(1);
            for (String line : cols.split(",")) {
                String col = line.trim().split("\\s+")[0];
                if (!col.isEmpty()) columns.add(col);
            }
        }

        // 3️⃣ 提取 INSERT 数据
        Pattern insertPattern = Pattern.compile(
                "INSERT INTO\\s+\\w+\\s*\\(([^)]+)\\)\\s*VALUES\\s*\\(([^)]+)\\);",
                Pattern.CASE_INSENSITIVE);
        Matcher insertMatcher = insertPattern.matcher(sql);

        List<Map<String, Object>> rows = new ArrayList<>();
        while (insertMatcher.find()) {
            String[] insertCols = insertMatcher.group(1).replaceAll("\\s+", "").split(",");
            String[] values = splitValues(insertMatcher.group(2));

            Map<String, Object> row = new LinkedHashMap<>();
            for (int i = 0; i < insertCols.length && i < values.length; i++) {
                row.put(insertCols[i], cleanValue(values[i]));
            }
            rows.add(row);
        }

        // 4️⃣ 手动构造 JSON 字符串（不依赖外部库）
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"table\": \"").append(tableName).append("\",\n");

        sb.append("  \"columns\": [");
        for (int i = 0; i < columns.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append("\"").append(columns.get(i)).append("\"");
        }
        sb.append("],\n");

        sb.append("  \"data\": [\n");
        for (int r = 0; r < rows.size(); r++) {
            Map<String, Object> row = rows.get(r);
            sb.append("    {");
            int c = 0;
            for (Map.Entry<String, Object> entry : row.entrySet()) {
                if (c++ > 0) sb.append(", ");
                sb.append("\"").append(entry.getKey()).append("\": ");
                Object v = entry.getValue();
                if (v instanceof String) {
                    sb.append("\"").append(v).append("\"");
                } else {
                    sb.append(v);
                }
            }
            sb.append("}");
            if (r < rows.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("  ]\n");
        sb.append("}\n");

        // 写出结果
        Files.writeString(Paths.get(outputPath), sb.toString());
        System.out.println("✅ 转换完成！输出文件：" + outputPath);
    }

    /** 拆分 SQL VALUES 内容，防止被逗号干扰 */
    private static String[] splitValues(String valuesPart) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (char c : valuesPart.toCharArray()) {
            if (c == '\'') inQuotes = !inQuotes;
            if (c == ',' && !inQuotes) {
                values.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        if (current.length() > 0) values.add(current.toString().trim());
        return values.toArray(new String[0]);
    }

    /** 去除引号或转为数字 */
    private static Object cleanValue(String v) {
        v = v.trim();
        if (v.startsWith("'") && v.endsWith("'")) {
            return v.substring(1, v.length() - 1);
        } else if (v.matches("\\d+")) {
            return Integer.parseInt(v);
        }
        return v;
    }
}
