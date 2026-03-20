import java.util.*;

class AddOnService {
    private String serviceName;
    private double cost;

    public AddOnService(String serviceName, double cost) {
        this.serviceName = serviceName;
        this.cost = cost;
    }

    public String getServiceName() {
        return serviceName;
    }

    public double getCost() {
        return cost;
    }

    @Override
    public String toString() {
        return serviceName + " ($" + String.format("%.2f", cost) + ")";
    }
}

class AddOnServiceManager {
    private Map<String, List<AddOnService>> servicesByReservation;

    public AddOnServiceManager() {
        servicesByReservation = new HashMap<>();
    }

    public void addService(String reservationId, AddOnService service) {
        servicesByReservation.putIfAbsent(reservationId, new ArrayList<>());
        servicesByReservation.get(reservationId).add(service);
        System.out.println("✓ Service added: " + service.getServiceName() + " to Reservation " + reservationId);
    }

    /**
     * Calculates total add-on cost
     * for a reservation.
     *
     * @param reservationId reservation ID
     * @return total service cost
     */
    public double calculateTotalServiceCost(String reservationId) {
        return servicesByReservation.getOrDefault(reservationId, new ArrayList<>())
                .stream()
                .mapToDouble(AddOnService::getCost)
                .sum();
    }

    public List<AddOnService> getServices(String reservationId) {
        return servicesByReservation.getOrDefault(reservationId, new ArrayList<>());
    }

    public void displayServices(String reservationId) {
        List<AddOnService> services = getServices(reservationId);
        if (services.isEmpty()) {
            System.out.println("No add-on services selected for Reservation " + reservationId);
        } else {
            System.out.println("\n--- Add-On Services for Reservation " + reservationId + " ---");
            for (int i = 0; i < services.size(); i++) {
                System.out.println((i + 1) + ". " + services.get(i));
            }
            System.out.println("Total Add-On Cost: $" + String.format("%.2f", calculateTotalServiceCost(reservationId)));
        }
    }
}

public class UseCase7AddOnServiceSelection {
    public static void main(String[] args) {
        System.out.println("===============================================");
        System.out.println("USE CASE 7: ADD-ON SERVICE SELECTION");
        System.out.println("===============================================\n");
        AddOnServiceManager serviceManager = new AddOnServiceManager();
        AddOnService breakfastPackage = new AddOnService("Breakfast Package", 15.99);
        AddOnService spaAccess = new AddOnService("Spa Access", 45.00);
        AddOnService airportTransfer = new AddOnService("Airport Transfer", 35.00);
        AddOnService earlyCheckIn = new AddOnService("Early Check-In", 20.00);
        AddOnService lateCheckOut = new AddOnService("Late Check-Out", 25.00);
        AddOnService miniBar = new AddOnService("Mini Bar Access", 12.50);
        System.out.println("--- Available Add-On Services ---");
        System.out.println("1. " + breakfastPackage);
        System.out.println("2. " + spaAccess);
        System.out.println("3. " + airportTransfer);
        System.out.println("4. " + earlyCheckIn);
        System.out.println("5. " + lateCheckOut);
        System.out.println("6. " + miniBar);
        System.out.println("\n--- GUEST 1: Selecting Services ---");
        String reservationId1 = "RES-001";
        System.out.println("Reservation ID: " + reservationId1);
        serviceManager.addService(reservationId1, breakfastPackage);
        serviceManager.addService(reservationId1, spaAccess);
        serviceManager.addService(reservationId1, airportTransfer);
        serviceManager.displayServices(reservationId1);
        System.out.println("\n--- GUEST 2: Selecting Services ---");
        String reservationId2 = "RES-002";
        System.out.println("Reservation ID: " + reservationId2);
        serviceManager.addService(reservationId2, earlyCheckIn);
        serviceManager.addService(reservationId2, lateCheckOut);
        serviceManager.addService(reservationId2, miniBar);
        serviceManager.displayServices(reservationId2);
        System.out.println("\n--- GUEST 3: No Additional Services ---");
        String reservationId3 = "RES-003";
        System.out.println("Reservation ID: " + reservationId3);
        serviceManager.displayServices(reservationId3);
        System.out.println("\n===============================================");
        System.out.println("BOOKING SUMMARY WITH ADD-ON COSTS");
        System.out.println("===============================================");
        double baseRoomCost1 = 150.00;
        double baseRoomCost2 = 120.00;
        double baseRoomCost3 = 100.00;
        System.out.println("\nReservation 1 (RES-001):");
        System.out.println("  Base Room Cost: $" + String.format("%.2f", baseRoomCost1));
        System.out.println("  Add-On Services Cost: $" + String.format("%.2f", serviceManager.calculateTotalServiceCost(reservationId1))); 
        System.out.println("  Total Cost: $" + String.format("%.2f", baseRoomCost1 + serviceManager.calculateTotalServiceCost(reservationId1)));  
        System.out.println("\nReservation 2 (RES-002):");
        System.out.println("  Base Room Cost: $" + String.format("%.2f", baseRoomCost2));
        System.out.println("  Add-On Services Cost: $" + String.format("%.2f", serviceManager.calculateTotalServiceCost(reservationId2))); 
        System.out.println("  Total Cost: $" + String.format("%.2f", baseRoomCost2 + serviceManager.calculateTotalServiceCost(reservationId2)));  
        System.out.println("\nReservation 3 (RES-003):");
        System.out.println("  Base Room Cost: $" + String.format("%.2f", baseRoomCost3));
        System.out.println("  Add-On Services Cost: $" + String.format("%.2f", serviceManager.calculateTotalServiceCost(reservationId3))); 
        System.out.println("  Total Cost: $" + String.format("%.2f", baseRoomCost3 + serviceManager.calculateTotalServiceCost(reservationId3)));  
        System.out.println("\n===============================================");
        System.out.println("KEY CONCEPTS DEMONSTRATED:");
        System.out.println("===============================================");
        System.out.println("✓ Business Extensibility: Optional services added without modifying core logic");
        System.out.println("✓ One-to-Many Relationship: Multiple services per reservation");
        System.out.println("✓ Map and List Combination: Efficient service lookup by reservation ID");
        System.out.println("✓ Composition over Inheritance: Services composed with reservations");
        System.out.println("✓ Separation of Concerns: Add-on logic independent of booking/inventory");
        System.out.println("✓ Cost Aggregation: Service costs calculated separately and combined");
        System.out.println("===============================================\n");
    }
}