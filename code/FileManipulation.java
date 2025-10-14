import java.io.*;
import java.util.*;

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
        String sqlFilePath="D:\\collage class\\CS213_database\\project1\\database_project1\\data_for_file\\flights.sql";
        List<Flight> flights = parseSQLFile(sqlFilePath);
        List<Flight> matchedFlights = new ArrayList<>();

        // 使用LIKE语义进行匹配
        for (Flight flight : flights) {
            if (flight.getDay_op().contains(day_op)) {
                matchedFlights.add(flight);
            }
        }

        // 格式化输出结果
        return formatFlights(matchedFlights);
    }

    /**
     * 解析SQL文件，提取航班数据
     */
    private List<Flight> parseSQLFile(String filePath) {
        List<Flight> flights = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // 查找INSERT语句
                if (line.trim().toUpperCase().startsWith("INSERT INTO FLIGHTS")) {
                    Flight flight = parseInsertStatement(line);
                    if (flight != null) {
                        flights.add(flight);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return flights;
    }

    /**
     * 解析单条INSERT语句
     */
    private Flight parseInsertStatement(String insertStatement) {
        try {
            // 提取VALUES后面的部分
            int valuesIndex = insertStatement.toUpperCase().indexOf("VALUES");
            if (valuesIndex == -1) return null;

            String valuesPart = insertStatement.substring(valuesIndex + "VALUES".length()).trim();

            // 去除括号并分割字段
            valuesPart = valuesPart.substring(1, valuesPart.lastIndexOf(')')).trim();

            // 分割字段，注意处理字符串中的逗号
            List<String> fields = splitFields(valuesPart);

            if (fields.size() >= 9) {
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
            }
        } catch (Exception e) {
            System.err.println("解析INSERT语句失败: " + insertStatement);
            e.printStackTrace();
        }

        return null;
    }

    /**
     * 分割字段，处理字符串中的逗号
     */
    private List<String> splitFields(String valuesPart) {
        List<String> fields = new ArrayList<>();
        StringBuilder currentField = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < valuesPart.length(); i++) {
            char c = valuesPart.charAt(i);

            if (c == '\'') {
                inQuotes = !inQuotes;
                currentField.append(c);
            } else if (c == ',' && !inQuotes) {
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
     * 清理字段值（去除引号等）
     */
    private String cleanField(String field) {
        if (field.startsWith("'") && field.endsWith("'")) {
            return field.substring(1, field.length() - 1);
        }
        return field;
    }

    /**
     * 格式化航班信息输出
     */
    private String formatFlights(List<Flight> flights) {
        if (flights.isEmpty()) {
            return "未找到匹配的航班信息";
        }

        StringBuilder strb = new StringBuilder();
        for (Flight flight : flights) {
            strb.append(String.format("%-5s\t", flight.getDeparture()));
            strb.append(flight.getArrival()).append("\t");
            strb.append(flight.getDay_op()).append("\t");
            strb.append(flight.getDep_time()).append("\t");
            strb.append(flight.getCarrier()).append("\t");
            strb.append(flight.getAirline()).append("\t");
            strb.append(String.format("%-5s\t", flight.getFlightnum()));
            strb.append(String.format("%-5s\t", flight.getDuration()));
            strb.append(flight.getAircraft()).append("\n");
        }
        return strb.toString();
    }
}
