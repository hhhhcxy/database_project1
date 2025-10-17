import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileManipulation implements DataManipulation {

    @Override
    public int addOneMovie(String str) {
        try (FileWriter writer = new FileWriter("movies.txt", true)) {
            writer.write(str);
        } catch (IOException e) {
            e.printStackTrace();
            return 0;
        }
        return 1;
    }

    @Override
    public String allContinentNames() {
        String line;
        int continentIndex = 2;
        Set<String> continentNames = new HashSet<>();
        StringBuilder sb = new StringBuilder();

        try (BufferedReader bufferedReader = new BufferedReader(new FileReader("countries.txt"))) {
            bufferedReader.readLine();
            while ((line = bufferedReader.readLine()) != null) {
                line = line.split(";")[continentIndex];
                if (!continentNames.contains(line)) {
                    sb.append(line).append("\n");
                    continentNames.add(line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return sb.toString();
    }

    @Override
    public String continentsWithCountryCount() {
        String line;
        int continentIndex = 2;
        Map<String, Integer> continentCount = new HashMap<>();
        StringBuilder sb = new StringBuilder();

        try (BufferedReader bufferedReader = new BufferedReader(new FileReader("countries.txt"))) {
            bufferedReader.readLine();
            while ((line = bufferedReader.readLine()) != null) {
                line = line.split(";")[continentIndex];
                if (continentCount.containsKey(line)) {
                    continentCount.put(line, continentCount.get(line) + 1);
                } else {
                    continentCount.put(line, 1);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        for (Map.Entry<String, Integer> entry : continentCount.entrySet()) {
            sb.append(entry.getKey())
                    .append("\t")
                    .append(entry.getValue())
                    .append("\n");
        }

        return sb.toString();
    }

    private Map<String, String> getCountryMap() {
        String line;
        String[] splitArray;
        int countryCodeIndex = 0, countryNameIndex = 1, continentIndex = 2;
        Map<String, String> rst = new HashMap<>();

        try (BufferedReader bufferedReader = new BufferedReader(new FileReader("countries.txt"))) {
            bufferedReader.readLine();
            while ((line = bufferedReader.readLine()) != null) {
                splitArray = line.split(";");
                rst.put(splitArray[countryCodeIndex].trim(), String.format("%-18s", splitArray[countryNameIndex])
                        + splitArray[continentIndex]);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return rst;
    }

    private List<FullInformation> getFullInformation(Map<String, String> countryMap, int min, int max) {
        String line;
        String[] splitArray;
        List<FullInformation> list = new ArrayList<>();
        int titleIndex = 1, countryIndex = 2, runTimeIndex = 4, runTime;

        try (BufferedReader bufferedReader = new BufferedReader(new FileReader("movies.txt"))) {
            bufferedReader.readLine();
            while ((line = bufferedReader.readLine()) != null) {
                splitArray = line.split(";");

                if (!"null".equals(splitArray[runTimeIndex])) {
                    runTime = Integer.parseInt(splitArray[runTimeIndex]);
                    if (runTime >= min && runTime <= max) {
                        line = runTime + "\t" + countryMap.get(splitArray[countryIndex].trim()) + "\t"
                                + splitArray[titleIndex] + "\n";
                        list.add(new FullInformation(runTime, line));
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return list;
    }

    @Override
    public String FullInformationOfMoviesRuntime(int min, int max) {
        Map<String, String> countryMap = getCountryMap();
        List<FullInformation> list = getFullInformation(countryMap, min, max);
        list.sort(Comparator.comparing(f -> f.runTime));

        StringBuilder sb = new StringBuilder();
        for (FullInformation f : list) {
            sb.append(f.information);
        }

        return sb.toString();
    }

    @Override
    public String findMovieById(int id) {
        return null;
    }

    class FullInformation {
        int runTime;
        String information;

        FullInformation(int runTime, String information) {
            this.runTime = runTime;
            this.information = information;
        }
    }

    @Override
    public String findMoviesByLimited10(String title) {
        return null;
    }

    @Override
    public String findFlightsByDay_op(String day_op) {
        String jsonPath = "D:\\collage class\\CS213_database\\project1\\database_project1\\data_for_file\\flights.json"; // 转换后的 JSON 文件路径

        try {
            // 1️⃣ 读取整个 JSON 文件
            String json = Files.readString(Paths.get(jsonPath));

            // 2️⃣ 提取 data 数组部分
            int start = json.indexOf("\"data\": [");
            if (start == -1) return "❌ JSON 格式错误：未找到 data 字段";
            start = json.indexOf("[", start) + 1;
            int end = json.lastIndexOf("]");
            String dataSection = json.substring(start, end).trim();

            // 3️⃣ 按每个对象分割（每个 { ... } 就是一条航班记录）
            String[] entries = dataSection.split("\\},\\s*\\{");

            StringBuilder strb = new StringBuilder();

            for (String e : entries) {
                String entry = e;
                if (!entry.startsWith("{")) entry = "{" + entry;
                if (!entry.endsWith("}")) entry = entry + "}";

                // 精确匹配 day_op
                String dayValue = extractJsonValue(entry, "day_op");
                if (dayValue.equals(day_op)) {
                    strb.append(String.format("%-5s\t", extractJsonValue(entry, "departure")));
                    strb.append(extractJsonValue(entry, "arrival")).append("\t");
                    strb.append(dayValue).append("\t");
                    strb.append(extractJsonValue(entry, "dep_time")).append("\t");
                    strb.append(extractJsonValue(entry, "carrier")).append("\t");
                    strb.append(extractJsonValue(entry, "airline")).append("\t");
                    strb.append(String.format("%-5s\t", extractJsonValue(entry, "flightnum")));
                    strb.append(String.format("%-5s\t", extractJsonValue(entry, "duration")));
                    strb.append(extractJsonValue(entry, "aircraft")).append("\n");
                }
            }

            return strb.length() > 0 ? strb.toString() : "未找到匹配的航班。";

        } catch (IOException e) {
            e.printStackTrace();
            return "❌ 文件读取失败：" + e.getMessage();
        }
    }

    /**
     * 从 JSON 对象字符串中提取指定字段的值。
     * 例如 extractJsonValue("{\"departure\":\"ACC\",\"arrival\":\"AMS\"}", "arrival") → "AMS"
     */
    private static String extractJsonValue(String json, String key) {
        String regex = "\"" + key + "\"\\s*:\\s*(\"[^\"]*\"|\\d+)";
        Matcher m = Pattern.compile(regex).matcher(json);
        if (m.find()) {
            String val = m.group(1);
            if (val.startsWith("\"") && val.endsWith("\""))
                val = val.substring(1, val.length() - 1);
            return val;
        }
        return "";
    }
}
