import java.io.*;
import java.util.*;

class Booking implements Serializable {
    int id;
    String name;

    Booking(int id, String name) {
        this.id = id;
        this.name = name;
    }
}

class Inventory implements Serializable {
    Map<String, Integer> items = new HashMap<>();

    void addItem(String item, int count) {
        items.put(item, items.getOrDefault(item, 0) + count);
    }
}

class SystemState implements Serializable {
    List<Booking> bookings = new ArrayList<>();
    Inventory inventory = new Inventory();
}

class PersistenceService {
    static void save(SystemState state, String fileName) {
        try {
            ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(fileName));
            out.writeObject(state);
            out.close();
        } catch (Exception e) {
            System.out.println("Save failed");
        }
    }

    static SystemState load(String fileName) {
        try {
            ObjectInputStream in = new ObjectInputStream(new FileInputStream(fileName));
            SystemState state = (SystemState) in.readObject();
            in.close();
            return state;
        } catch (Exception e) {
            System.out.println("No valid data found, starting fresh");
            return new SystemState();
        }
    }
}

public class Main {
    public static void main(String[] args) {
        String file = "data.ser";

        SystemState state = PersistenceService.load(file);

        state.inventory.addItem("Laptop", 5);
        state.bookings.add(new Booking(1, "Alice"));

        System.out.println("Inventory: " + state.inventory.items);
        System.out.println("Bookings: " + state.bookings.size());

        PersistenceService.save(state, file);
    }
}