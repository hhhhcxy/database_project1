
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.*;

public class OpengaussManipulation implements DataManipulation {
    private Connection con = null;
    private ResultSet resultSet;

    private String host = "localhost";
    private String dbname = "project1";
    private String user = "new_user";
    private String pwd = "0xccCheng!";
    private String port = "8888";


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

    private String containerName = "opengauss";

    /**
     * 初始化 openGauss 数据库：删除旧库，创建新库，导入 SQL
     */
    public void initDatabase() {
        String url = "jdbc:postgresql://" + host + ":" + port + "/postgres"; // openGauss JDBC 兼容

        try (Connection conn = DriverManager.getConnection(url, user, pwd);
             Statement stmt = conn.createStatement()) {

            System.out.println("连接到 openGauss 数据库服务器...");

            // 删除旧数据库
            System.out.println("正在删除旧数据库（如果存在）...");
            stmt.executeUpdate("DROP DATABASE IF EXISTS \"" + dbname + "\";");

            // 创建新数据库
            System.out.println("正在创建新数据库...");
            stmt.executeUpdate("CREATE DATABASE \"" + dbname + "\" WITH ENCODING 'UTF8';");

            // 导入 SQL 文件（通过 docker exec 调用 gsql）
            importSQLInDocker();

            restartDatabaseContainer();
            try {
                Thread.sleep(5000); // 等待容器重启后数据库完全启动
            } catch (InterruptedException e) {
                // 重新设置中断状态（推荐做法）
                Thread.currentThread().interrupt();
                System.err.println("等待过程中线程被中断：" + e.getMessage());
            }

            System.out.println("✅ openGauss 数据库初始化完成！");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void restartDatabaseContainer() {
        try {
            System.out.println("正在重启 openGauss 容器: " + containerName);
            executeCommand("docker restart " + containerName);
            System.out.println("✅ 容器已重启。");
        } catch (Exception e) {
            System.err.println("⚠️ 重启容器失败，请确认容器名称是否正确或 Docker 是否正在运行。");
            e.printStackTrace();
        }
    }

    private void importSQLInDocker() {
        String containerSQLPath ="/mnt/sql/flights.sql";
        try {
            // 2️⃣ 执行 gsql 导入命令
            System.out.println("在容器中执行 gsql 导入...");
            String command = String.format(
                    "docker exec -i -u omm opengauss bash -c \"export LD_LIBRARY_PATH=/usr/local/opengauss/lib && /usr/local/opengauss/bin/gsql -d %s -U %s -W \\\"%s\\\" -f \\\"%s\\\"\"",
                    dbname, user, pwd, containerSQLPath
            );

            System.out.println("执行命令：" + command);
            executeCommand(command);

            System.out.println("✅ SQL 导入成功！");

        } catch (Exception e) {
            System.err.println("⚠️ 导入 SQL 文件失败：");
            e.printStackTrace();
        }
    }

    /**
     * 执行外部命令并打印输出
     */
    private void executeCommand(String command) throws IOException, InterruptedException {
        System.out.println("执行命令：" + command);
        Process process = new ProcessBuilder("cmd", "/c", command).start();

        BufferedReader stdOut = new BufferedReader(new InputStreamReader(process.getInputStream()));
        BufferedReader stdErr = new BufferedReader(new InputStreamReader(process.getErrorStream()));

        String line;
        while ((line = stdOut.readLine()) != null) System.out.println(line);
        while ((line = stdErr.readLine()) != null) System.err.println(line);

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("命令执行失败，退出码：" + exitCode);
        }
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
        String sql = "SELECT * FROM flights WHERE flightnum::text LIKE ?;";// SQL语句

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
        String sql = "UPDATE flights SET flightnum = REPLACE(flightnum, ?, ?);";

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