
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.*;

public class PostgresqlManipulation implements DataManipulation {
    private Connection con = null;
    private ResultSet resultSet;

    private String host = "localhost";
    private String dbname = "Project1";
    private String user = "postgres";
    private String pwd = "0xccCheng";
    private String port = "5432";

    @Override
    public void initDatabase() {
        String sqlFilePath="D:\\collage class\\CS213_database\\project1\\database_project1\\data_for_postgreSQL\\flights.sql";
        String url = "jdbc:postgresql://" + host + ":" + port + "/postgres"; // 连接默认库postgres

        try (Connection conn = DriverManager.getConnection(url, user, pwd);
             Statement stmt = conn.createStatement()) {

            conn.setAutoCommit(true);

            System.out.println("连接到 PostgreSQL 数据库服务器...");

            // 1️⃣ 删除旧数据库（如果存在）
            System.out.println("正在删除旧数据库（如果存在）...");
            stmt.executeUpdate("DROP DATABASE IF EXISTS \"" + dbname + "\";");

            // 2️⃣ 创建新数据库
            System.out.println("正在创建新数据库...");
            stmt.executeUpdate("CREATE DATABASE \"" + dbname + "\" WITH ENCODING 'UTF8';");

            // 3️⃣ 导入SQL文件
            importSQL(sqlFilePath);

            restartPostgresService();

            System.out.println("✅ 数据库初始化完成！");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void importSQL(String sqlFilePath) {
        try {
            String command = String.format(
                    "psql -h %s -p %s -U %s -d %s -f \"%s\"",
                    host, port, user, dbname, sqlFilePath
            );

            System.out.println("执行命令：" + command);

            // ✅ 在进程中设置 PGPASSWORD 环境变量
            ProcessBuilder pb = new ProcessBuilder("cmd", "/c", command);
            pb.environment().put("PGPASSWORD", pwd);

            Process process = pb.start();

            // 读取输出流（避免阻塞）
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));
            BufferedReader errReader = new BufferedReader(
                    new InputStreamReader(process.getErrorStream()));

            String line;
            while ((line = reader.readLine()) != null) System.out.println(line);
            while ((line = errReader.readLine()) != null) System.err.println(line);

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                System.out.println("✅ SQL 导入成功。");
            } else {
                System.err.println("⚠️ SQL 导入失败，退出代码：" + exitCode);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    /**
     * 可选：重启数据库服务（仅限本地、需管理员权限）
     */
    private void restartPostgresService() {
        String serviceName = "postgresql-x64-17"; // 请根据你的 PostgreSQL 版本修改
        try {
            System.out.println("尝试重启 PostgreSQL 服务 (" + serviceName + ")...");

            // 停止服务
            Process stopProcess = new ProcessBuilder("cmd", "/c", "net stop " + serviceName).start();
            stopProcess.waitFor();

            // 启动服务
            Process startProcess = new ProcessBuilder("cmd", "/c", "net start " + serviceName).start();
            startProcess.waitFor();

            System.out.println("✅ PostgreSQL 服务已成功重启。");
        } catch (Exception e) {
            System.err.println("⚠️ 无法重启 PostgreSQL 服务，请检查服务名或管理员权限。");
            e.printStackTrace();
        }
    }


    private void getConnection() {
        try {
            Class.forName("org.postgresql.Driver");

        } catch (Exception e) {
            System.err.println("Cannot find the PostgreSQL driver. Check CLASSPATH.");
            System.exit(1);
        }

        try {
            String url = "jdbc:postgresql://" + host + ":" + port + "/" + dbname;
            con = DriverManager.getConnection(url, user, pwd);

        } catch (SQLException e) {
            System.err.println("Database connection failed");
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }


    private void closeConnection() {
        if (con != null) {
            try {
                con.close();
                con = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public int addOneMovie(String str) {
        getConnection();
        int result = 0;
        String sql = "insert into movies (title, country,year_released,runtime) " +
                "values (?,?,?,?)";
        String movieInfo[] = str.split(";");
        try {
            PreparedStatement preparedStatement = con.prepareStatement(sql);
            preparedStatement.setString(1, movieInfo[0]);
            preparedStatement.setString(2, movieInfo[1]);
            preparedStatement.setInt(3, Integer.parseInt(movieInfo[2]));
            preparedStatement.setInt(4, Integer.parseInt(movieInfo[3]));
            System.out.println(preparedStatement.toString());

            result = preparedStatement.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            closeConnection();
        }
        return result;
    }

    @Override
    public String allContinentNames() {
        getConnection();
        StringBuilder sb = new StringBuilder();
        String sql = "select continent from countries group by continent";
        try {
            Statement statement = con.createStatement();
            resultSet = statement.executeQuery(sql);
            while (resultSet.next()) {
                sb.append(resultSet.getString("continent") + "\n");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            closeConnection();
        }

        return sb.toString();
    }

    @Override
    public String continentsWithCountryCount() {
        getConnection();
        StringBuilder sb = new StringBuilder();
        String sql = "select continent, count(*) countryNumber from countries group by continent;";
        try {
            Statement statement = con.createStatement();
            resultSet = statement.executeQuery(sql);
            while (resultSet.next()) {
                sb.append(resultSet.getString("continent") + "\t");
                sb.append(resultSet.getString("countryNumber"));
                sb.append(System.lineSeparator());
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            closeConnection();
        }

        return sb.toString();
    }

    @Override
    public String FullInformationOfMoviesRuntime(int min, int max) {
        getConnection();
        StringBuilder sb = new StringBuilder();
        String sql = "select m.title,c.country_name country,c.continent ,m.runtime " +
                "from movies m " +
                "join countries c on m.country=c.country_code " +
                "where m.runtime between ? and ? order by runtime;";
        try {
            PreparedStatement preparedStatement = con.prepareStatement(sql);
            preparedStatement.setInt(1, min);
            preparedStatement.setInt(2, max);
            resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                sb.append(resultSet.getString("runtime") + "\t");
                sb.append(String.format("%-18s", resultSet.getString("country")));
                sb.append(resultSet.getString("continent") + "\t");
                sb.append(resultSet.getString("title") + "\t");
                sb.append(System.lineSeparator());
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            closeConnection();
        }
        return sb.toString();
    }

    @Override
    public String findMovieById(int id) {
        return null;
    }

    @Override
    public String findMoviesByLimited10(String title) {
        getConnection();    // start connection
        String sql = "select m.title, c.country_name country, m.runtime,m.year_released\n"+ "from movies m join countries c on m.country = c.country_code\n"+ "where m.title like '%'||?||'%'limit 10;";// string combination
        try {
            PreparedStatement preparedStatement = con.prepareStatement(sql);//change here!
            preparedStatement.setString(1, title);// change here!
            resultSet = preparedStatement.executeQuery();// and here!

            StringBuilder strb=new StringBuilder(); //combine multi-strings
            while (resultSet.next()){
                strb.append(String.format("%-20s\t",
                        resultSet.getString("country")));
                strb.append(resultSet.getInt("year_released")).append("\t");
                strb.append(resultSet.getInt("runtime")).append("\t");
                strb.append(resultSet.getString("title")).append("\n");
            }
            return strb.toString();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        finally {
            closeConnection();  // close connection
        }
        return null;
    }

    @Override
    public String findFlightsByDay_op(String day_op){
        getConnection();    // start connection
        String sql = "select * from flights f where day_op like ?;";// string combination
        try {
            PreparedStatement preparedStatement = con.prepareStatement(sql);//change here!
            preparedStatement.setString(1, day_op);// change here!
            resultSet = preparedStatement.executeQuery();// and here!

            StringBuilder strb=new StringBuilder(); //combine multi-strings
            while (resultSet.next()){
                strb.append(String.format("%-5s\t", resultSet.getString("departure")));
                strb.append(resultSet.getString("arrival")).append("\t");
                strb.append(resultSet.getString("day_op")).append("\t");
                strb.append(resultSet.getString("dep_time")).append("\t");
                strb.append(resultSet.getString("carrier")).append("\t");
                strb.append(resultSet.getString("airline")).append("\t");
                strb.append(String.format("%-5s\t", resultSet.getString("flightnum")));
                strb.append(String.format("%-5s\t", resultSet.getInt("duration")));
                strb.append(resultSet.getString("aircraft")).append("\n");
            }
            return strb.toString();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        finally {
            closeConnection();  // close connection
        }
        return null;
    }

    @Override
    public String findFlightsByFlightnum(String flightnum) {
        getConnection();  // start connection
        String sql = "SELECT * FROM flights WHERE flightnum::text LIKE ?;"; // SQL语句

        try {
            PreparedStatement preparedStatement = con.prepareStatement(sql);
            // 使用模糊匹配（包含该子串）
            preparedStatement.setString(1, "%" + flightnum + "%");

            resultSet = preparedStatement.executeQuery();

            StringBuilder strb = new StringBuilder();
            while (resultSet.next()) {
                strb.append(String.format("%-5s\t", resultSet.getString("departure")));
                strb.append(resultSet.getString("arrival")).append("\t");
                strb.append(resultSet.getString("day_op")).append("\t");
                strb.append(resultSet.getString("dep_time")).append("\t");
                strb.append(resultSet.getString("carrier")).append("\t");
                strb.append(resultSet.getString("airline")).append("\t");
                strb.append(String.format("%-5s\t", resultSet.getString("flightnum")));
                strb.append(String.format("%-5s\t", resultSet.getInt("duration")));
                strb.append(resultSet.getString("aircraft")).append("\n");
            }
            return strb.toString();

        } catch (SQLException throwables) {
            throwables.printStackTrace();
        } finally {
            closeConnection(); // close connection
        }

        return null;
    }

    @Override
    public String updateFlightnumSubstring(String oldSubstr, String newSubstr) {
        getConnection(); // start connection

        // 使用 SQL 的 REPLACE() 实现字符串替换
        String sql = "UPDATE flights SET flightnum = REPLACE(flightnum::text, ?, ?)::integer;";

        try {
            PreparedStatement preparedStatement = con.prepareStatement(sql);
            preparedStatement.setString(1, oldSubstr); // 要被替换的子串
            preparedStatement.setString(2, newSubstr); // 替换成的新子串

            int rowsAffected = preparedStatement.executeUpdate(); // 执行更新

            return "✅ 更新成功：共有 " + rowsAffected + " 条航班记录的航班号被修改。";

        } catch (SQLException throwables) {
            throwables.printStackTrace();
            return "❌ 数据库更新失败：" + throwables.getMessage();
        } finally {
            closeConnection(); // close connection
        }
    }



}
/*
@Override
    public String findMoviesByLimited10(String title) {
        getConnection();    // start connection
        String sql = "select m.title, c.country_name country, m.runtime,m.year_released\n"+ "from movies m join countries c on m.country = c.country_code\n"+ "where m.title like '%'||?||'%'limit 10;";// string combination
        try {
            PreparedStatement preparedStatement = con.prepareStatement(sql);//change here!
                    preparedStatement.setString(1, title);// change here!
            resultSet = preparedStatement.executeQuery();// and here!

            StringBuilder strb=new StringBuilder(); //combine multi-strings
            while (resultSet.next()){
                strb.append(String.format("%-20s\t",
                        resultSet.getString("country")));
                strb.append(resultSet.getInt("year_released")).append("\t");
                strb.append(resultSet.getInt("runtime")).append("\t");
                strb.append(resultSet.getString("title")).append("\n");
            }
            return strb.toString();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        finally {
            closeConnection();  // close connection
        }
        return null;
    }
 */