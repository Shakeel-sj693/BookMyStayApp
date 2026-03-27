import java.util.*;
import java.util.concurrent.*;

// Booking Request Model
class BookingRequest {
  private final String guestName;

  public BookingRequest(String guestName) {
    this.guestName = guestName;
  }

  public String getGuestName() {
    return guestName;
  }
}

// Shared Inventory (NOT thread-safe version)
class UnsafeInventory {
  private int availableRooms;

  public UnsafeInventory(int rooms) {
    this.availableRooms = rooms;
  }

  public boolean bookRoom(String guest) {
    if (availableRooms > 0) {
      // Simulate delay (to amplify race condition)
      try { Thread.sleep(50); } catch (InterruptedException e) {}
      availableRooms--;
      System.out.println("Room booked for " + guest + " | Remaining: " + availableRooms);
      return true;
    }
    return false;
  }

  public int getAvailableRooms() {
    return availableRooms;
  }
}

// Thread-safe Inventory using synchronization
class SafeInventory {
  private int availableRooms;

  public SafeInventory(int rooms) {
    this.availableRooms = rooms;
  }

  // Critical Section
  public synchronized boolean bookRoom(String guest) {
    if (availableRooms > 0) {
      try { Thread.sleep(50); } catch (InterruptedException e) {}
      availableRooms--;
      System.out.println("Room booked for " + guest + " | Remaining: " + availableRooms);
      return true;
    }
    return false;
  }

  public int getAvailableRooms() {
    return availableRooms;
  }
}

// Booking Processor (Worker Thread)
class BookingProcessor implements Runnable {
  private final Queue<BookingRequest> queue;
  private final Object queueLock;
  private final Object inventory;
  private final boolean safeMode;

  public BookingProcessor(Queue<BookingRequest> queue, Object queueLock, Object inventory, boolean safeMode) {
    this.queue = queue;
    this.queueLock = queueLock;
    this.inventory = inventory;
    this.safeMode = safeMode;
  }

  @Override
  public void run() {
    while (true) {
      BookingRequest request;

      // Synchronize queue access
      synchronized (queueLock) {
        if (queue.isEmpty()) return;
        request = queue.poll();
      }

      // Process booking
      if (safeMode) {
        ((SafeInventory) inventory).bookRoom(request.getGuestName());
      } else {
        ((UnsafeInventory) inventory).bookRoom(request.getGuestName());
      }
    }
  }
}

// Main Class
public class ConcurrentBookingDemo {

  public static void main(String[] args) throws InterruptedException {
    System.out.println("=== UNSAFE BOOKING (Race Condition Demo) ===");
    runSimulation(false);

    System.out.println("\n=== SAFE BOOKING (Synchronized) ===");
    runSimulation(true);
  }

  private static void runSimulation(boolean safeMode) throws InterruptedException {
    int totalRooms = 5;
    int totalRequests = 10;

    Queue<BookingRequest> bookingQueue = new LinkedList<>();
    Object queueLock = new Object();

    // Create booking requests
    for (int i = 1; i <= totalRequests; i++) {
      bookingQueue.add(new BookingRequest("Guest-" + i));
    }

    Object inventory = safeMode
            ? new SafeInventory(totalRooms)
            : new UnsafeInventory(totalRooms);

    // Create thread pool
    int threadCount = 3;
    List<Thread> threads = new ArrayList<>();

    for (int i = 0; i < threadCount; i++) {
      Thread t = new Thread(new BookingProcessor(bookingQueue, queueLock, inventory, safeMode));
      threads.add(t);
      t.start();
    }

    // Wait for all threads to finish
    for (Thread t : threads) {
      t.join();
    }

    int remaining = safeMode
            ? ((SafeInventory) inventory).getAvailableRooms()
            : ((UnsafeInventory) inventory).getAvailableRooms();

    System.out.println("Final Remaining Rooms: " + remaining);
  }
}