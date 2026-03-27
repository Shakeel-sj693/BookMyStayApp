import java.time.LocalDateTime;
import java.util.*;

/**
 * Use Case 10: Safe Cancellation + Rollback
 *
 * - Confirmed bookings allocate a roomId and decrement inventory.
 * - Cancellations reverse state in a controlled order:
 *    1) validate reservation exists + is cancellable
 *    2) record allocated roomId in rollback stack (LIFO)
 *    3) increment inventory for that room type (immediate restoration)
 *    4) update booking history to reflect cancellation (audit trail)
 */
public class UseCase10CancellationRollbackWithStack {

    // -------------------- Custom Exceptions --------------------

    static class BookingDomainException extends RuntimeException {
        public BookingDomainException(String message) { super(message); }
    }

    static final class InvalidRoomTypeException extends BookingDomainException {
        public InvalidRoomTypeException(String roomType, Set<String> supported) {
            super("Invalid room type: '" + roomType + "'. Supported types: " + supported);
        }
    }

    static final class InvalidGuestNameException extends BookingDomainException {
        public InvalidGuestNameException() {
            super("Invalid guest name: guestName cannot be null/blank.");
        }
    }

    static final class NoAvailabilityException extends BookingDomainException {
        public NoAvailabilityException(String roomType) {
            super("No availability for room type '" + roomType + "'. Booking cannot be confirmed.");
        }
    }

    static final class ReservationNotFoundException extends BookingDomainException {
        public ReservationNotFoundException(String reservationId) {
            super("Reservation not found: '" + reservationId + "'.");
        }
    }

    static final class AlreadyCancelledException extends BookingDomainException {
        public AlreadyCancelledException(String reservationId) {
            super("Reservation already cancelled: '" + reservationId + "'.");
        }
    }

    static final class NotCancellableException extends BookingDomainException {
        public NotCancellableException(String reservationId) {
            super("Reservation is not cancellable: '" + reservationId + "'.");
        }
    }

    // -------------------- Domain Model --------------------

    enum ReservationStatus {
        CONFIRMED,
        CANCELLED
    }

    static final class Reservation {
        private final String reservationId;
        private final String guestName;
        private final String roomType;
        private final String allocatedRoomId;     // required for rollback
        private final LocalDateTime confirmedAt;

        private ReservationStatus status;
        private LocalDateTime cancelledAt;

        public Reservation(String reservationId,
                           String guestName,
                           String roomType,
                           String allocatedRoomId,
                           LocalDateTime confirmedAt) {
            if (reservationId == null || reservationId.isBlank()) throw new IllegalArgumentException("reservationId cannot be null/blank");
            if (guestName == null || guestName.isBlank()) throw new IllegalArgumentException("guestName cannot be null/blank");
            if (roomType == null || roomType.isBlank()) throw new IllegalArgumentException("roomType cannot be null/blank");
            if (allocatedRoomId == null || allocatedRoomId.isBlank()) throw new IllegalArgumentException("allocatedRoomId cannot be null/blank");
            if (confirmedAt == null) throw new IllegalArgumentException("confirmedAt cannot be null");

            this.reservationId = reservationId;
            this.guestName = guestName;
            this.roomType = roomType;
            this.allocatedRoomId = allocatedRoomId;
            this.confirmedAt = confirmedAt;

            this.status = ReservationStatus.CONFIRMED;
        }

        public String getReservationId() { return reservationId; }
        public String getGuestName() { return guestName; }
        public String getRoomType() { return roomType; }
        public String getAllocatedRoomId() { return allocatedRoomId; }
        public ReservationStatus getStatus() { return status; }
        public LocalDateTime getConfirmedAt() { return confirmedAt; }
        public LocalDateTime getCancelledAt() { return cancelledAt; }

        public boolean isCancellable() {
            // Simple rule: only CONFIRMED can be cancelled
            return status == ReservationStatus.CONFIRMED;
        }

        public void markCancelled(LocalDateTime when) {
            this.status = ReservationStatus.CANCELLED;
            this.cancelledAt = when;
        }

        @Override
        public String toString() {
            return "Reservation{" +
                    "reservationId='" + reservationId + '\'' +
                    ", guestName='" + guestName + '\'' +
                    ", roomType='" + roomType + '\'' +
                    ", allocatedRoomId='" + allocatedRoomId + '\'' +
                    ", status=" + status +
                    ", confirmedAt=" + confirmedAt +
                    (cancelledAt != null ? ", cancelledAt=" + cancelledAt : "") +
                    '}';
        }
    }

    // -------------------- Inventory + Room ID Pool --------------------

    /**
     * Inventory counts by room type.
     * Incremented immediately on cancellation (state restoration).
     */
    static final class RoomInventory {
        private final Map<String, Integer> availableByType = new HashMap<>();

        public RoomInventory(int single, int dbl, int suite) {
            if (single < 0 || dbl < 0 || suite < 0) {
                throw new BookingDomainException("Inventory cannot be initialized with negative counts.");
            }
            availableByType.put("Single", single);
            availableByType.put("Double", dbl);
            availableByType.put("Suite", suite);
        }

        public Set<String> supportedRoomTypes() {
            return availableByType.keySet();
        }

        public int getAvailable(String roomType) {
            Integer v = availableByType.get(roomType);
            if (v == null) throw new BookingDomainException("Unknown room type in inventory: '" + roomType + "'");
            return v;
        }

        public void decrement(String roomType) {
            int current = getAvailable(roomType);
            if (current <= 0) throw new NoAvailabilityException(roomType);
            availableByType.put(roomType, current - 1);
        }

        public void increment(String roomType) {
            int current = getAvailable(roomType);
            availableByType.put(roomType, current + 1);
        }

        public void printInventory() {
            System.out.println("Inventory:");
            for (Map.Entry<String, Integer> e : availableByType.entrySet()) {
                System.out.println("- " + e.getKey() + ": " + e.getValue());
            }
        }
    }

    /**
     * Room ID availability pools.
     * On confirmation: allocate from pool (LIFO via Deque).
     * On cancellation: released IDs go to rollbackStack, then returned to pool.
     */
    static final class RoomIdPool {
        private final Map<String, Deque<String>> availableIdsByType = new HashMap<>();

        public RoomIdPool() {
            availableIdsByType.put("Single", new ArrayDeque<>());
            availableIdsByType.put("Double", new ArrayDeque<>());
            availableIdsByType.put("Suite", new ArrayDeque<>());
        }

        public void addRoomId(String roomType, String roomId) {
            Deque<String> dq = availableIdsByType.get(roomType);
            if (dq == null) throw new BookingDomainException("Unknown room type for roomId pool: '" + roomType + "'");
            if (roomId == null || roomId.isBlank()) throw new BookingDomainException("roomId cannot be null/blank");
            dq.push(roomId); // push => LIFO allocation (simplifies rollback story)
        }

        public String allocate(String roomType) {
            Deque<String> dq = availableIdsByType.get(roomType);
            if (dq == null) throw new BookingDomainException("Unknown room type for roomId pool: '" + roomType + "'");
            String id = dq.pollFirst();
            if (id == null) {
                // If count says available but IDs missing -> inconsistent state
                throw new BookingDomainException("No room IDs available for type '" + roomType + "'. State inconsistent.");
            }
            return id;
        }

        public void release(String roomType, String roomId) {
            Deque<String> dq = availableIdsByType.get(roomType);
            if (dq == null) throw new BookingDomainException("Unknown room type for roomId pool: '" + roomType + "'");
            dq.push(roomId);
        }

        public void printPools() {
            System.out.println("RoomId Pools (top is next allocation):");
            for (Map.Entry<String, Deque<String>> e : availableIdsByType.entrySet()) {
                System.out.println("- " + e.getKey() + ": " + e.getValue());
            }
        }
    }

    // -------------------- Booking History (audit trail) --------------------

    static final class BookingHistory {
        // insertion order preserved
        private final List<Reservation> reservations = new ArrayList<>();
        // quick lookup by reservationId
        private final Map<String, Reservation> byId = new HashMap<>();

        public void addConfirmed(Reservation r) {
            if (r == null) throw new IllegalArgumentException("reservation cannot be null");
            reservations.add(r);
            byId.put(r.getReservationId(), r);
        }

        public Reservation getById(String reservationId) {
            return byId.get(reservationId);
        }

        public List<Reservation> getAllInOrderReadOnly() {
            return Collections.unmodifiableList(reservations);
        }

        public void printHistory() {
            System.out.println("Booking History (insertion order):");
            if (reservations.isEmpty()) {
                System.out.println("(empty)");
                return;
            }
            int i = 1;
            for (Reservation r : reservations) {
                System.out.println(i + ". " + r);
                i++;
            }
        }
    }

    // -------------------- Validation --------------------

    static final class BookingValidator {
        public void validateForConfirmation(String guestName, String roomType, RoomInventory inventory) {
            if (guestName == null || guestName.isBlank()) throw new InvalidGuestNameException();
            if (roomType == null || roomType.isBlank() || !inventory.supportedRoomTypes().contains(roomType)) {
                throw new InvalidRoomTypeException(roomType, inventory.supportedRoomTypes());
            }
            // inventory.decrement does final availability check
        }

        public void validateForCancellation(String reservationId, BookingHistory history) {
            if (reservationId == null || reservationId.isBlank()) {
                throw new BookingDomainException("Invalid reservationId: cannot be null/blank.");
            }
            Reservation r = history.getById(reservationId);
            if (r == null) throw new ReservationNotFoundException(reservationId);
            if (r.getStatus() == ReservationStatus.CANCELLED) throw new AlreadyCancelledException(reservationId);
            if (!r.isCancellable()) throw new NotCancellableException(reservationId);
        }
    }

    // -------------------- Services --------------------

    static final class BookingConfirmationService {
        private final BookingValidator validator;
        private final RoomInventory inventory;
        private final RoomIdPool roomIdPool;
        private final BookingHistory history;
        private int sequence = 1000;

        public BookingConfirmationService(BookingValidator validator,
                                         RoomInventory inventory,
                                         RoomIdPool roomIdPool,
                                         BookingHistory history) {
            this.validator = Objects.requireNonNull(validator, "validator");
            this.inventory = Objects.requireNonNull(inventory, "inventory");
            this.roomIdPool = Objects.requireNonNull(roomIdPool, "roomIdPool");
            this.history = Objects.requireNonNull(history, "history");
        }

        public Reservation confirm(String guestName, String roomType) {
            validator.validateForConfirmation(guestName, roomType, inventory);

            // Controlled mutation order for confirmation:
            // 1) decrement inventory
            // 2) allocate roomId
            // 3) store in history
            inventory.decrement(roomType);
            String allocatedRoomId = roomIdPool.allocate(roomType);

            String reservationId = "R-" + (++sequence);
            Reservation r = new Reservation(reservationId, guestName, roomType, allocatedRoomId, LocalDateTime.now());
            history.addConfirmed(r);

            System.out.println("Confirmed: " + r.getReservationId() + " allocatedRoomId=" + allocatedRoomId);
            return r;
        }
    }

    /**
     * Cancellation Service:
     * - validates existence + cancellable
     * - uses Stack<String> to track rollback (LIFO)
     * - increments inventory immediately
     * - updates booking history to CANCELLED (audit)
     */
    static final class CancellationService {
        private final BookingValidator validator;
        private final RoomInventory inventory;
        private final RoomIdPool roomIdPool;
        private final BookingHistory history;

        // rollback structure: most recently released roomId is first to be returned
        private final Stack<String> releasedRoomIdRollbackStack = new Stack<>();

        public CancellationService(BookingValidator validator,
                                   RoomInventory inventory,
                                   RoomIdPool roomIdPool,
                                   BookingHistory history) {
            this.validator = Objects.requireNonNull(validator, "validator");
            this.inventory = Objects.requireNonNull(inventory, "inventory");
            this.roomIdPool = Objects.requireNonNull(roomIdPool, "roomIdPool");
            this.history = Objects.requireNonNull(history, "history");
        }

        public void cancel(String reservationId) {
            // 1) validate reservation exists and cancellable (fail-fast)
            validator.validateForCancellation(reservationId, history);

            Reservation r = history.getById(reservationId);
            String roomType = r.getRoomType();
            String roomId = r.getAllocatedRoomId();

            // Controlled rollback order:
            // 2) record allocated room ID in rollback stack (LIFO)
            releasedRoomIdRollbackStack.push(roomId);

            // 3) restore inventory immediately
            inventory.increment(roomType);

            // 4) update booking history / reservation status to CANCELLED (audit trail)
            r.markCancelled(LocalDateTime.now());

            // 5) release the roomId back to pool from the rollback stack (predictable LIFO behavior)
            String toRelease = releasedRoomIdRollbackStack.pop();
            roomIdPool.release(roomType, toRelease);

            System.out.println("Cancelled: " + reservationId + " (released roomId=" + roomId + ", restored inventory for " + roomType + ")");
        }
    }

    // -------------------- Demo (graceful failures, stable system) --------------------

    public static void main(String[] args) {
        System.out.println("BookMyStayApp - Use Case 10 (Cancellation + Rollback using Stack)");
        System.out.println();

        RoomInventory inventory = new RoomInventory(1, 1, 0);

        RoomIdPool pool = new RoomIdPool();
        pool.addRoomId("Single", "S-101");
        pool.addRoomId("Double", "D-201");
        // Note: Suite inventory is 0, so we don't add suite IDs

        BookingHistory history = new BookingHistory();
        BookingValidator validator = new BookingValidator();

        BookingConfirmationService confirmationService =
                new BookingConfirmationService(validator, inventory, pool, history);
        CancellationService cancellationService =
                new CancellationService(validator, inventory, pool, history);

        inventory.printInventory();
        pool.printPools();
        System.out.println();

        Reservation r1 = null;
        Reservation r2 = null;

        // Confirm a couple bookings (with validation)
        try {
            r1 = confirmationService.confirm("Guest-1", "Single");
            r2 = confirmationService.confirm("Guest-2", "Double");
        } catch (BookingDomainException ex) {
            System.out.println("Confirmation failed: " + ex.getMessage());
        }

        System.out.println();
        inventory.printInventory();
        pool.printPools();
        System.out.println();
        history.printHistory();
        System.out.println("--------------------------------------------------");

        // Cancel: valid
        try {
            cancellationService.cancel(r1.getReservationId());
        } catch (BookingDomainException ex) {
            System.out.println("Cancellation failed: " + ex.getMessage());
        }

        // Cancel again: rejected safely (already cancelled)
        try {
            cancellationService.cancel(r1.getReservationId());
        } catch (BookingDomainException ex) {
            System.out.println("Cancellation failed: " + ex.getMessage());
        }

        // Cancel non-existent: rejected safely
        try {
            cancellationService.cancel("R-9999");
        } catch (BookingDomainException ex) {
            System.out.println("Cancellation failed: " + ex.getMessage());
        }

        System.out.println();
        inventory.printInventory();
        pool.printPools();
        System.out.println();
        history.printHistory();

        System.out.println();
        System.out.println("System remained stable; inventory + roomId pools are consistent after cancellations.");
    }
