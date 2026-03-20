import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public class UseCase6RoomAllocation {

    static final class Reservation {
        private final String guestName;
        private final String roomType;

        public Reservation(String guestName, String roomType) {
            if (guestName == null || guestName.isBlank()) {
                throw new IllegalArgumentException("guestName cannot be null/blank");
            }
            if (roomType == null || roomType.isBlank()) {
                throw new IllegalArgumentException("roomType cannot be null/blank");
            }
            this.guestName = guestName;
            this.roomType = roomType;
        }

        public String getGuestName() {
            return guestName;
        }

        public String getRoomType() {
            return roomType;
        }

        @Override
        public String toString() {
            return "Reservation{guestName='" + guestName + "', roomType='" + roomType + "'}";
        }
    }

    static final class BookingRequestQueue {
        private final Queue<Reservation> bookingQueue = new ArrayDeque<>();

        public void submit(Reservation reservation) {
            if (reservation == null) {
                throw new IllegalArgumentException("reservation cannot be null");
            }
            bookingQueue.add(reservation);
        }

        public Reservation dequeue() {
            return bookingQueue.poll();
        }

        public boolean isEmpty() {
            return bookingQueue.isEmpty();
        }

        public int size() {
            return bookingQueue.size();
        }
    }

    static final class RoomInventory {
        private final Map<String, Integer> roomAvailability = new HashMap<>();

        public RoomInventory() {
            initializeInventory();
        }

        private void initializeInventory() {
            roomAvailability.put("Single", 2);
            roomAvailability.put("Double", 1);
            roomAvailability.put("Suite", 1);
        }

        public int getAvailability(String roomType) {
            return roomAvailability.getOrDefault(roomType, 0);
        }

        public void decrementAvailability(String roomType) {
            int current = getAvailability(roomType);
            if (current <= 0) {
                throw new IllegalStateException("No availability for room type: " + roomType);
            }
            roomAvailability.put(roomType, current - 1);
        }

        public Map<String, Integer> getRoomAvailability() {
            return Map.copyOf(roomAvailability);
        }
    }

    static final class RoomAllocationService {

        private final Set<String> allocatedRoomIds;
        private final Map<String, Set<String>> assignedRoomsByType;

        public RoomAllocationService() {
            this.allocatedRoomIds = new HashSet<>();
            this.assignedRoomsByType = new HashMap<>();
        }

        public void allocateRoom(Reservation reservation, RoomInventory inventory) {
            if (reservation == null || inventory == null) {
                throw new IllegalArgumentException("reservation and inventory must not be null");
            }

            String roomType = reservation.getRoomType();

            int available = inventory.getAvailability(roomType);
            if (available <= 0) {
                System.out.println("Rejected: " + reservation.getGuestName() + " requested " + roomType
                        + " (no availability)");
                return;
            }

            String roomId = generateRoomId(roomType);

            allocatedRoomIds.add(roomId);
            assignedRoomsByType.computeIfAbsent(roomType, k -> new HashSet<>()).add(roomId);

            inventory.decrementAvailability(roomType);

            System.out.println("Confirmed: " + reservation.getGuestName()
                    + " -> " + roomType
                    + " | RoomId=" + roomId);
        }

        private String generateRoomId(String roomType) {
            String prefix = normalizePrefix(roomType);

            int seq = 1;
            String candidate = prefix + "-" + seq;

            while (allocatedRoomIds.contains(candidate)) {
                seq++;
                candidate = prefix + "-" + seq;
            }
            return candidate;
        }

        private String normalizePrefix(String roomType) {
            String t = roomType.trim().toUpperCase();
            if (t.length() >= 3) {
                return t.substring(0, 3);
            }
            return t;
        }

        public Map<String, Set<String>> getAssignedRoomsByType() {
            Map<String, Set<String>> copy = new HashMap<>();
            for (Map.Entry<String, Set<String>> e : assignedRoomsByType.entrySet()) {
                copy.put(e.getKey(), Set.copyOf(e.getValue()));
            }
            return copy;
        }
    }

    public static void main(String[] args) {
        RoomInventory inventory = new RoomInventory();

        BookingRequestQueue queue = new BookingRequestQueue();
        queue.submit(new Reservation("Guest-1", "Single"));
        queue.submit(new Reservation("Guest-2", "Single"));
        queue.submit(new Reservation("Guest-3", "Single"));
        queue.submit(new Reservation("Guest-4", "Suite"));
        queue.submit(new Reservation("Guest-5", "Double"));

        RoomAllocationService allocationService = new RoomAllocationService();

        System.out.println("BookMyStayApp - Use Case 6");
        System.out.println("Processing booking requests in FIFO...");
        System.out.println();

        while (!queue.isEmpty()) {
            Reservation next = queue.dequeue();
            allocationService.allocateRoom(next, inventory);
        }

        System.out.println();
        System.out.println("Final Inventory: " + inventory.getRoomAvailability());
        System.out.println("Assigned Rooms By Type: " + allocationService.getAssignedRoomsByType());
    }
}
