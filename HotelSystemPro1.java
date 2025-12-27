import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

import java.util.*;

public class HotelSystemPro1 {

    static Scanner sc = new Scanner(System.in);
    static List<Room> rooms = new ArrayList<>();
    static List<Reservation> reservations = new ArrayList<>();
    static int nextResId = 1, nextGuestId = 1;

    public static void main(String[] args) {
        seedData();

        while (true) {
            System.out.println();
            menu(
                "1) Search availability (by date / optional max rate)",
                "2) Book + payment mock",
                "3) Check-in",
                "4) Check-out (generate invoice)",
                "5) Booking calendar (monthly)",
                "6) Occupancy report (date range)",
                "7) List reservations",
                "8) Exit",
                "9) Daily calendar (rooms status)"   // NEW
            );
            System.out.print("Choose: ");
            String ch = sc.nextLine().trim();

            switch (ch) {
                case "1": handleSearch(); break;
                case "2": handleBooking(); break;
                case "3": handleCheckIn(); break;
                case "4": handleCheckOut(); break;
                case "5": handleCalendar(); break;
                case "6": handleOccupancy(); break;
                case "7": listReservations(); break;
                case "8": System.out.println("Bye!"); return;
                case "9": handleDaily(); break; // NEW
                default: System.out.println("Invalid choice.");
            }
        }
    }

    // --- Menu Handlers ---
    static void handleSearch() {
        System.out.println("-- Availability Search --");
        LocalDate in = askDate("Check-in (yyyy-mm-dd): ");
        LocalDate out = askDate("Check-out (yyyy-mm-dd): ");
        Double max = askDoubleOpt("Max rate (optional): ");
        for (Room r : rooms) {
            if (CalendarService.roomAvailable(r, in, out)) {
                double rate = PricingService.rate(r, in, out);
                if (max == null || rate <= max) {
                    System.out.printf("Room %s (%s): %.2f%n", r.number, r.type, rate);
                }
            }
        }
    }

    static void handleBooking() {
        System.out.println("-- Booking --");
        LocalDate in = askDate("Check-in (yyyy-mm-dd): ");
        LocalDate out = askDate("Check-out (yyyy-mm-dd): ");
        List<Room> avail = new ArrayList<>();
        for (Room r : rooms) if (CalendarService.roomAvailable(r, in, out)) avail.add(r);
        if (avail.isEmpty()) { System.out.println("No rooms free."); return; }
        for (int i = 0; i < avail.size(); i++) {
            Room r = avail.get(i);
            double rate = PricingService.rate(r, in, out);
            System.out.printf("%d) Room %s (%s): %.2f%n", i+1, r.number, r.type, rate);
        }
        int choice = askInt("Choose room #: ", 1, avail.size());
        Room sel = avail.get(choice-1);
        System.out.print("Guest name: "); String gname = sc.nextLine();
        Guest g = new Guest(nextGuestId++, gname);
        double total = PricingService.rate(sel, in, out);
        Reservation res = new Reservation(nextResId++, sel, g, in, out, "BOOKED", total);
        reservations.add(res);
        System.out.println("Booked! Reservation #" + res.id + " Total=" + res.total);
    }

    static void handleCheckIn() {
        int id = askInt("Reservation id: ",1,9999);
        Reservation r = findRes(id);
        if (r!=null) { r.status="CHECKED_IN"; System.out.println("Checked in."); }
        else System.out.println("Not found.");
    }

    static void handleCheckOut() {
        int id = askInt("Reservation id: ",1,9999);
        Reservation r = findRes(id);
        if (r!=null) { 
            r.status="CHECKED_OUT"; 
            System.out.println("Invoice: " + r.total + " paid. Bye!"); 
        }
        else System.out.println("Not found.");
    }

    static void handleCalendar() {
        System.out.println("-- Booking Calendar (Monthly) --");
        int y = askInt("Year: ",2000,2100);
        int m = askInt("Month(1-12): ",1,12);
        printMonthlyCalendar(y,m);
    }

    static void handleOccupancy() {
        System.out.println("-- Occupancy Report --");
        LocalDate in = askDate("From (yyyy-mm-dd): ");
        LocalDate out = askDate("To (yyyy-mm-dd): ");
        long totalNights = ChronoUnit.DAYS.between(in, out);
        long bookedNights = 0;
        for (Reservation r : reservations) {
            if (!r.status.equals("CANCELLED")) {
                LocalDate start = r.checkin.isBefore(in)? in: r.checkin;
                LocalDate end = r.checkout.isAfter(out)? out: r.checkout;
                if (start.isBefore(end)) {
                    bookedNights += ChronoUnit.DAYS.between(start,end);
                }
            }
        }
        System.out.printf("Total room-nights: %d, Booked: %d, Occupancy: %.1f%%%n",
            totalNights*rooms.size(), bookedNights, 100.0*bookedNights/(totalNights*rooms.size()));
    }

    static void listReservations() {
        for (Reservation r : reservations) {
            System.out.printf("#%d Room%s %s %s-%s %s Total=%.2f%n",
                r.id,r.room.number,r.guest.name,fmt(r.checkin),fmt(r.checkout),r.status,r.total);
        }
    }

    // --- NEW: Daily Calendar ---
    static void handleDaily() {
        System.out.println("\n-- Daily Calendar (Room Status) --");
        int year  = askInt("Year (e.g., 2025): ", 2000, 2100);
        int month = askInt("Month (1-12): ", 1, 12);
        int day   = askInt("Day (1-31): ", 1, 31);

        try {
            LocalDate date = LocalDate.of(year, month, day);
            printDailyCalendar(date);
        } catch (Exception e) {
            System.out.println("❌ Invalid date.");
        }
    }

    static void printDailyCalendar(LocalDate date) {
        System.out.printf("%n📅 Daily Calendar: %s%n", fmt(date));
        System.out.println("──────────────────────────────────");
        for (Room r : rooms) {
            boolean occupied = !CalendarService.roomAvailable(r, date, date.plusDays(1));
            String status = occupied ? "❌ BOOKED" : "✅ AVAILABLE";
            System.out.printf("Room %s (%s) → %s%n", r.number, r.type, status);
        }
        System.out.println("──────────────────────────────────\n");
    }

    // --- Utility ---
    static void seedData() {
        rooms.add(new Room(1,"101","Standard",100));
        rooms.add(new Room(2,"102","Deluxe",150));
        rooms.add(new Room(3,"201","Suite",200));
    }
    static void menu(String...opts){ for (String o:opts) System.out.println(o); }
    static LocalDate askDate(String msg){
        System.out.print(msg);
        return LocalDate.parse(sc.nextLine().trim());
    }
    static int askInt(String msg,int min,int max){
        while(true){
            System.out.print(msg);
            try{int v=Integer.parseInt(sc.nextLine()); if(v>=min&&v<=max)return v;}
            catch(Exception e){}
            System.out.println("Invalid.");
        }
    }
    static Double askDoubleOpt(String msg){
        System.out.print(msg); String s=sc.nextLine().trim();
        if(s.isEmpty())return null;
        try{return Double.parseDouble(s);}catch(Exception e){return null;}
    }
    static String fmt(LocalDate d){return d.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));}
    static Reservation findRes(int id){for(Reservation r:reservations)if(r.id==id)return r;return null;}
    static void printMonthlyCalendar(int year,int month){
        LocalDate first=LocalDate.of(year,month,1);
        System.out.printf("%n     %s %d%n",first.getMonth(),year);
        System.out.println("Su Mo Tu We Th Fr Sa");
        int val=first.getDayOfWeek().getValue()%7;
        for(int i=0;i<val;i++)System.out.print("   ");
        int len=first.lengthOfMonth();
        for(int d=1;d<=len;d++){
            LocalDate curr=LocalDate.of(year,month,d);
            int count=0;
            for(Reservation r:reservations){
                if(!(curr.isBefore(r.checkin)||!curr.isBefore(r.checkout))) count++;
            }
            String mark = count==0? String.format("%2d",d): d+"*";
            System.out.printf("%-3s",mark);
            if((val+d)%7==0)System.out.println();
        }
        System.out.println("\n\nLegend: number=rooms booked ( means 1 or more)");
    }

    // --- Data Models ---
    static class Room{int id;String number,type;double baseRate;Room(int id,String num,String t,double br){this.id=id;this.number=num;this.type=t;this.baseRate=br;}}
    static class Guest{int id;String name;Guest(int id,String n){this.id=id;this.name=n;}}
    static class Reservation{int id;Room room;Guest guest;LocalDate checkin,checkout;String status;double total;Reservation(int i,Room r,Guest g,LocalDate ci,LocalDate co,String st,double tot){id=i;room=r;guest=g;checkin=ci;checkout=co;status=st;total=tot;}}
    static class PricingService{static double rate(Room r,LocalDate in,LocalDate out){long days=ChronoUnit.DAYS.between(in,out);double base=r.baseRate;return base*days;}}
    static class CalendarService{static boolean roomAvailable(Room r,LocalDate in,LocalDate out){for(Reservation rs:reservations){if(rs.room==r&&!rs.status.equals("CANCELLED")){if(!(out.isBefore(rs.checkin)||in.isAfter(rs.checkout.minusDays(1))))return false;}}return true;}}
}
 
