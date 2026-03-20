import java.util.ArrayDeque;
import java.util.Queue;

public class UseCase5BookingRequestQueue {

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

    static final class BookingRequestQueue {
        private final Queue<Reservation> bookingQueue;

        public BookingRequestQueue() {
            this.bookingQueue = new ArrayDeque<>();
        }

        public void submitRequest(Reservation reservation) {
            if (reservation == null) {
                throw new IllegalArgumentException("reservation cannot be null");
            }
            bookingQueue.add(reservation);
        }

        public Reservation peekNextRequest() {
            return bookingQueue.peek();
        }

        public int getPendingRequestCount() {
            return bookingQueue.size();
        }

        public boolean isEmpty() {
            return bookingQueue.isEmpty();
        }

        public void printQueue() {
            System.out.println("Booking Request Queue (FIFO)");
            int position = 1;
            for (Reservation r : bookingQueue) {
                System.out.println(position + ". " + r.getGuestName() + " -> " + r.getRoomType());
                position++;
            }
        }
    }

    public static void main(String[] args) {
        BookingRequestQueue requestQueue = new BookingRequestQueue();

        requestQueue.submitRequest(new Reservation("Guest-1", "Single"));
        requestQueue.submitRequest(new Reservation("Guest-2", "Double"));
        requestQueue.submitRequest(new Reservation("Guest-3", "Suite"));
        requestQueue.submitRequest(new Reservation("Guest-4", "Double"));

        System.out.println("BookMyStayApp - Use Case 5");
        System.out.println();

        requestQueue.printQueue();
        System.out.println();

        System.out.println("Next to process (peek): " + requestQueue.peekNextRequest());
        System.out.println("Pending requests: " + requestQueue.getPendingRequestCount());
        System.out.println();
        System.out.println("Note: No inventory mutation or room allocation occurs in this stage.");
    }
}
