public class UseCase2RoomInitialization {

    static abstract class Room {
        private final int numberOfBeds;
        private final int sizeSqFt;
        private final double pricePerNight;

        protected Room(int numberOfBeds, int sizeSqFt, double pricePerNight) {
            this.numberOfBeds = numberOfBeds;
            this.sizeSqFt = sizeSqFt;
            this.pricePerNight = pricePerNight;
        }

        public int getNumberOfBeds() {
            return numberOfBeds;
        }

        public int getSizeSqFt() {
            return sizeSqFt;
        }

        public double getPricePerNight() {
            return pricePerNight;
        }

        public abstract String getRoomType();

        public final void printDetails() {
            System.out.println("Room Type       : " + getRoomType());
            System.out.println("Beds            : " + numberOfBeds);
            System.out.println("Size (sq ft)    : " + sizeSqFt);
            System.out.printf("Price per Night : $%.2f%n", pricePerNight);
        }
    }

    static final class SingleRoom extends Room {
        public SingleRoom() {
            super(1, 250, 1500.0);
        }

        @Override
        public String getRoomType() {
            return "Single Room";
        }
    }

    static final class DoubleRoom extends Room {
        public DoubleRoom() {
            super(2, 400, 2500.0);
        }

        @Override
        public String getRoomType() {
            return "Double Room";
        }
    }

    static final class SuiteRoom extends Room {
        public SuiteRoom() {
            super(3, 750, 5000.0);
        }

        @Override
        public String getRoomType() {
            return "Suite Room";
        }
    }

    public static void main(String[] args) {
        Room single = new SingleRoom();
        Room doub = new DoubleRoom();
        Room suite = new SuiteRoom();

        int singleAvailability = 5;
        int doubleAvailability = 3;
        int suiteAvailability = 1;

        System.out.println("BookMyStayApp - Use Case 2");
        System.out.println();

        single.printDetails();
        System.out.println("Availability    : " + singleAvailability);
        System.out.println();

        doub.printDetails();
        System.out.println("Availability    : " + doubleAvailability);
        System.out.println();

        suite.printDetails();
        System.out.println("Availability    : " + suiteAvailability);
        System.out.println();
    }
}