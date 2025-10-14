import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SQLToTextConverter {

    public static void main(String[] args) {
        String inputFile = "D:\\collage class\\CS213_database\\project1\\database_project1\\data_for_file\\flights.sql";  // 输入SQL文件
        String outputFile = "D:\\collage class\\CS213_database\\project1\\database_project1\\data_for_file\\flights.json"; // 输出JSON文件

        SQLToTextConverter converter = new SQLToTextConverter();
        boolean success = converter.convertSQLToJSON(inputFile, outputFile);
        if (success) {
            System.out.println("转换完成！输出文件: " + outputFile);
        } else {
            System.out.println("转换失败，请检查输入文件和日志信息。");
        }
    }

    /**
     * 将SQL文件转换为JSON格式
     */
    public boolean convertSQLToJSON(String inputFilePath, String outputFilePath) {
        System.out.println("开始解析文件: " + inputFilePath);

        List<Flight> flights = parseSQLFile(inputFilePath);

        if (flights.isEmpty()) {
            System.err.println("警告: 没有解析到任何航班数据！");
            System.err.println("请检查:");
            System.err.println("1. 文件路径是否正确");
            System.err.println("2. SQL文件是否包含正确的INSERT语句");
            System.err.println("3. INSERT语句格式是否为: INSERT INTO flights (...) VALUES (...);");
            return false;
        }

        System.out.println("成功解析 " + flights.size() + " 条航班记录");
        writeToJSON(flights, outputFilePath);
        return true;
    }

    /**
     * 解析SQL文件，提取所有INSERT语句
     */
    private List<Flight> parseSQLFile(String filePath) {
        List<Flight> flights = new ArrayList<>();

        File file = new File(filePath);
        if (!file.exists()) {
            System.err.println("错误: 文件不存在 - " + filePath);
            return flights;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            int lineNumber = 0;
            int insertCount = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                String trimmedLine = line.trim();

                // 查找INSERT语句（不区分大小写）
                if (trimmedLine.toUpperCase().startsWith("INSERT INTO")) {
                    insertCount++;
                    System.out.println("在第 " + lineNumber + " 行找到INSERT语句");
                    System.out.println("原始语句: " + trimmedLine.substring(0, Math.min(100, trimmedLine.length())));

                    List<Flight> extractedFlights = extractFlightsFromInsert(trimmedLine);
                    if (extractedFlights.isEmpty()) {
                        System.err.println("警告: 无法从第 " + lineNumber + " 行的INSERT语句中提取数据");
                    } else {
                        flights.addAll(extractedFlights);
                        System.out.println("从该语句提取 " + extractedFlights.size() + " 条记录");
                    }
                }
            }

            System.out.println("总共找到 " + insertCount + " 条INSERT语句");

        } catch (IOException e) {
            System.err.println("读取文件错误: " + e.getMessage());
        }

        return flights;
    }

    /**
     * 从单条INSERT语句中提取航班数据
     */
    private List<Flight> extractFlightsFromInsert(String insertStatement) {
        List<Flight> flights = new ArrayList<>();

        try {
            // 更简单的解析方法：直接查找VALUES后面的内容
            int valuesIndex = insertStatement.toUpperCase().indexOf("VALUES");
            if (valuesIndex == -1) {
                System.err.println("错误: 未找到VALUES关键字");
                return flights;
            }

            String valuesPart = insertStatement.substring(valuesIndex + "VALUES".length()).trim();
            System.out.println("VALUES部分: " + valuesPart.substring(0, Math.min(100, valuesPart.length())));

            // 提取括号内的所有内容
            List<String> records = new ArrayList<>();
            int start = valuesPart.indexOf('(');
            int end = valuesPart.lastIndexOf(')');

            if (start == -1 || end == -1 || start >= end) {
                System.err.println("错误: 无法找到完整的括号对");
                return flights;
            }

            String content = valuesPart.substring(start + 1, end).trim();
            System.out.println("括号内容: " + content.substring(0, Math.min(100, content.length())));

            // 分割多条记录（如果有）
            List<String> recordStrings = splitRecords(content);
            System.out.println("分割出 " + recordStrings.size() + " 条记录");

            for (int i = 0; i < recordStrings.size(); i++) {
                String record = recordStrings.get(i);
                System.out.println("记录 " + (i+1) + ": " + record);

                Flight flight = parseRecord(record);
                if (flight != null) {
                    flights.add(flight);
                    System.out.println("成功解析记录 " + (i+1));
                } else {
                    System.err.println("解析记录 " + (i+1) + " 失败");
                }
            }

        } catch (Exception e) {
            System.err.println("解析INSERT语句时发生异常: " + e.getMessage());
            e.printStackTrace();
        }

        return flights;
    }

    /**
     * 分割多条记录
     */
    private List<String> splitRecords(String content) {
        List<String> records = new ArrayList<>();
        StringBuilder currentRecord = new StringBuilder();
        int parenDepth = 0;
        boolean inQuotes = false;
        char quoteChar = '\'';

        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);

            // 处理引号
            if (c == '\'' || c == '"') {
                if (!inQuotes) {
                    inQuotes = true;
                    quoteChar = c;
                } else if (c == quoteChar) {
                    inQuotes = false;
                }
            }

            // 处理括号
            if (!inQuotes) {
                if (c == '(') {
                    parenDepth++;
                } else if (c == ')') {
                    parenDepth--;
                }
            }

            // 分割记录
            if (!inQuotes && parenDepth == 0 && c == ',') {
                String record = currentRecord.toString().trim();
                if (!record.isEmpty()) {
                    records.add(record);
                }
                currentRecord = new StringBuilder();
            } else {
                currentRecord.append(c);
            }
        }

        // 添加最后一条记录
        String lastRecord = currentRecord.toString().trim();
        if (!lastRecord.isEmpty()) {
            records.add(lastRecord);
        }

        return records;
    }

    /**
     * 解析单条记录
     */
    private Flight parseRecord(String record) {
        try {
            List<String> fields = splitFields(record);
            System.out.println("分割出 " + fields.size() + " 个字段");

            if (fields.size() < 9) {
                System.err.println("错误: 字段数量不足，期望9个，实际: " + fields.size());
                return null;
            }

            String departure = cleanField(fields.get(0));
            String arrival = cleanField(fields.get(1));
            String day_op = cleanField(fields.get(2));
            String dep_time = cleanField(fields.get(3));
            String carrier = cleanField(fields.get(4));
            String airline = cleanField(fields.get(5));
            int flightnum = Integer.parseInt(cleanField(fields.get(6)));
            int duration = Integer.parseInt(cleanField(fields.get(7)));
            String aircraft = cleanField(fields.get(8));

            return new Flight(departure, arrival, day_op, dep_time, carrier,
                    airline, flightnum, duration, aircraft);

        } catch (Exception e) {
            System.err.println("解析记录时发生异常: " + e.getMessage());
            System.err.println("问题记录: " + record);
            return null;
        }
    }

    /**
     * 分割字段，正确处理字符串中的逗号
     */
    private List<String> splitFields(String record) {
        List<String> fields = new ArrayList<>();
        StringBuilder currentField = new StringBuilder();
        boolean inQuotes = false;
        char quoteChar = '\'';

        for (int i = 0; i < record.length(); i++) {
            char c = record.charAt(i);

            if ((c == '\'' || c == '"') && (i == 0 || record.charAt(i-1) != '\\')) {
                if (!inQuotes) {
                    inQuotes = true;
                    quoteChar = c;
                } else if (c == quoteChar) {
                    inQuotes = false;
                }
            }

            if (c == ',' && !inQuotes) {
                fields.add(currentField.toString().trim());
                currentField = new StringBuilder();
            } else {
                currentField.append(c);
            }
        }

        // 添加最后一个字段
        if (currentField.length() > 0) {
            fields.add(currentField.toString().trim());
        }

        return fields;
    }

    /**
     * 清理字段值（去除引号）
     */
    private String cleanField(String field) {
        String trimmed = field.trim();
        if ((trimmed.startsWith("'") && trimmed.endsWith("'")) ||
                (trimmed.startsWith("\"") && trimmed.endsWith("\""))) {
            return trimmed.substring(1, trimmed.length() - 1);
        }
        return trimmed;
    }

    /**
     * 将航班数据写入JSON文件
     */
    private void writeToJSON(List<Flight> flights, String outputFilePath) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath))) {
            writer.write("[\n");

            for (int i = 0; i < flights.size(); i++) {
                Flight flight = flights.get(i);
                writer.write("  {\n");
                writer.write(String.format("    \"departure\": \"%s\",\n", escapeJson(flight.getDeparture())));
                writer.write(String.format("    \"arrival\": \"%s\",\n", escapeJson(flight.getArrival())));
                writer.write(String.format("    \"day_op\": \"%s\",\n", escapeJson(flight.getDay_op())));
                writer.write(String.format("    \"dep_time\": \"%s\",\n", escapeJson(flight.getDep_time())));
                writer.write(String.format("    \"carrier\": \"%s\",\n", escapeJson(flight.getCarrier())));
                writer.write(String.format("    \"airline\": \"%s\",\n", escapeJson(flight.getAirline())));
                writer.write(String.format("    \"flightnum\": %d,\n", flight.getFlightnum()));
                writer.write(String.format("    \"duration\": %d,\n", flight.getDuration()));
                writer.write(String.format("    \"aircraft\": \"%s\"\n", escapeJson(flight.getAircraft())));

                if (i < flights.size() - 1) {
                    writer.write("  },\n");
                } else {
                    writer.write("  }\n");
                }
            }

            writer.write("]");
            System.out.println("成功写入JSON文件: " + outputFilePath);

        } catch (IOException e) {
            System.err.println("写入JSON文件错误: " + e.getMessage());
        }
    }

    /**
     * 转义JSON字符串中的特殊字符
     */
    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * 航班数据类
     */
    static class Flight {
        private String departure;
        private String arrival;
        private String day_op;
        private String dep_time;
        private String carrier;
        private String airline;
        private int flightnum;
        private int duration;
        private String aircraft;

        public Flight(String departure, String arrival, String day_op, String dep_time,
                      String carrier, String airline, int flightnum, int duration, String aircraft) {
            this.departure = departure;
            this.arrival = arrival;
            this.day_op = day_op;
            this.dep_time = dep_time;
            this.carrier = carrier;
            this.airline = airline;
            this.flightnum = flightnum;
            this.duration = duration;
            this.aircraft = aircraft;
        }

        // Getter方法
        public String getDeparture() { return departure; }
        public String getArrival() { return arrival; }
        public String getDay_op() { return day_op; }
        public String getDep_time() { return dep_time; }
        public String getCarrier() { return carrier; }
        public String getAirline() { return airline; }
        public int getFlightnum() { return flightnum; }
        public int getDuration() { return duration; }
        public String getAircraft() { return aircraft; }
    }
}