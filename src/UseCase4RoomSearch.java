import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class UseCase4RoomSearch {

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

        public abstract String getRoomTypeKey();

        public final void printDetails() {
            System.out.println("Room Type       : " + getRoomTypeKey());
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
        public String getRoomTypeKey() {
            return "Single";
        }
    }

    static final class DoubleRoom extends Room {
        public DoubleRoom() {
            super(2, 400, 2500.0);
        }

        @Override
        public String getRoomTypeKey() {
            return "Double";
        }
    }

    static final class SuiteRoom extends Room {
        public SuiteRoom() {
            super(3, 750, 5000.0);
        }

        @Override
        public String getRoomTypeKey() {
            return "Suite";
        }
    }

    static final class RoomInventory {
        private final Map<String, Integer> roomAvailability;

        public RoomInventory() {
            this.roomAvailability = new HashMap<>();
            initializeInventory();
        }

        private void initializeInventory() {
            roomAvailability.put("Single", 5);
            roomAvailability.put("Double", 3);
            roomAvailability.put("Suite", 0);
        }

        public Map<String, Integer> getRoomAvailability() {
            return Collections.unmodifiableMap(roomAvailability);
        }

        public int getAvailability(String roomTypeKey) {
            return roomAvailability.getOrDefault(roomTypeKey, 0);
        }

        public void updateAvailability(String roomTypeKey, int count) {
            if (roomTypeKey == null || roomTypeKey.isBlank()) {
                throw new IllegalArgumentException("roomTypeKey cannot be null/blank");
            }
            if (count < 0) {
                throw new IllegalArgumentException("count cannot be negative");
            }
            roomAvailability.put(roomTypeKey, count);
        }
    }

    static final class RoomSearchService {

        public void searchAvailableRooms(
                RoomInventory inventory,
                Room singleRoom,
                Room doubleRoom,
                Room suiteRoom
        ) {
            if (inventory == null || singleRoom == null || doubleRoom == null || suiteRoom == null) {
                throw new IllegalArgumentException("inventory and room objects must not be null");
            }

            displayIfAvailable(inventory, singleRoom);
            displayIfAvailable(inventory, doubleRoom);
            displayIfAvailable(inventory, suiteRoom);
        }

        private void displayIfAvailable(RoomInventory inventory, Room room) {
            int availableCount = inventory.getAvailability(room.getRoomTypeKey());
            if (availableCount <= 0) {
                return;
            }

            System.out.println("Availability    : " + availableCount);
            room.printDetails();
            System.out.println();
        }
    }

    public static void main(String[] args) {
        RoomInventory inventory = new RoomInventory();

        Room singleRoom = new SingleRoom();
        Room doubleRoom = new DoubleRoom();
        Room suiteRoom = new SuiteRoom();

        RoomSearchService searchService = new RoomSearchService();

        System.out.println("BookMyStayApp - Use Case 4");
        System.out.println();

        searchService.searchAvailableRooms(inventory, singleRoom, doubleRoom, suiteRoom);

        System.out.println("Inventory State (unchanged): " + inventory.getRoomAvailability());
    }
}