
public class Client {

    public static void main(String[] args) {
        try {
            DataManipulation dm = new DataFactory().createDataManipulation(args[0]);
            // 开始计时
            long startTime = System.currentTimeMillis();

//            System.out.println(dm.findMoviesByLimited10("aba"));
            for(int i=1;i<=100;++i)
            System.out.println(dm.findFlightsByDay_op("12"));


            // 结束计时并计算耗时
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            System.out.println("查询耗时: " + duration + " 毫秒");
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
        }
    }
}