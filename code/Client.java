
public class Client {

    public static void main(String[] args) {
        try {

            DataManipulation dm = new DataFactory().createDataManipulation(args[0]);
            long startTime = System.currentTimeMillis();
            dm.initDatabase();
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            System.out.println("初始化耗时: " + duration + " 毫秒");
            // 开始计时
            startTime = System.currentTimeMillis();

//            System.out.println(dm.findMoviesByLimited10("aba"));
//            for(int i=1;i<=100;++i){
//                dm.findFlightsByDay_op("12367");
//            }

//            for(int i=1;i<=100;++i){
//                dm.findFlightsByFlightnum("12");
////                System.out.println(dm.findFlightsByFlightnum("12"));
//            }

            for(int i=1;i<=10;++i){
                dm.updateFlightnumSubstring("1","2");
                dm.updateFlightnumSubstring("2","1");
            }

            // 结束计时并计算耗时
            endTime = System.currentTimeMillis();
            duration = endTime - startTime;

            System.out.println("操作耗时: " + duration + " 毫秒");
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
        }
    }
}