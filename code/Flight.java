public class Flight {
    private String departure;
    private String arrival;
    private String day_op;
    private String dep_time;
    private String carrier;
    private String airline;
    private int flightnum;
    private int duration;
    private String aircraft;

    // 构造函数、getter、setter方法
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

    // getter methods...
    public String getDay_op() { return day_op; }
    public String getDeparture() { return departure; }
    public String getArrival() { return arrival; }
    public String getDep_time() { return dep_time; }
    public String getCarrier() { return carrier; }
    public String getAirline() { return airline; }
    public int getFlightnum() { return flightnum; }
    public int getDuration() { return duration; }
    public String getAircraft() { return aircraft; }
}