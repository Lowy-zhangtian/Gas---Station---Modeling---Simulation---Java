import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class GasStationSimulation {

    static class Vehicle {
        double arrivalTime;
        String fuelType;
        boolean needsInvoice;
        String invoiceType;
        double fuelServiceStart;
        double fuelServiceEnd;
        double invoiceQueueEnter;
        double totalStayTime;
    }

    static class Event implements Comparable<Event> {
        double time;
        String type;
        Vehicle vehicle;
        String pumpId;

        public Event(double time, String type, Vehicle vehicle, String pumpId) {
            this.time = time;
            this.type = type;
            this.vehicle = vehicle;
            this.pumpId = pumpId;
        }

        @Override
        public int compareTo(Event other) {
            return Double.compare(this.time, other.time);
        }
    }

    private final PriorityQueue<Event> eventQueue = new PriorityQueue<>();
    private double currentTime;

    // Resources
    private boolean pumpABusy = false;
    private boolean pumpBBusy = false;
    private boolean clerkBusy = false;

    // Queues
    private final Queue<Vehicle> pumpAQueue = new LinkedList<>();
    private final Queue<Vehicle> pumpBQueue = new LinkedList<>();
    private final Queue<Vehicle> invoiceQueue = new LinkedList<>();

    // Statistics
    private final List<Double> pumpAWaitTimes = new ArrayList<>();
    private final List<Double> pumpBWaitTimes = new ArrayList<>();
    private final List<Double> invoiceWaitTimes = new ArrayList<>();
    private final List<Double> stayTimesA = new ArrayList<>();
    private final List<Double> stayTimesB = new ArrayList<>();
    private double pumpABusyTime;
    private double pumpBBusyTime;
    private double clerkBusyTime;
    private int maxPumpAQueue;
    private int maxPumpBQueue;
    private int maxInvoiceQueue;

    public static void main(String[] args) {
        int[] replications = {1, 10, 100, 300};
        double[] durations = {24 * 60, 10 * 24 * 60, 30 * 24 * 60, 365 * 24 * 60};

        for (int rep : replications) {
            for (double duration : durations) {
                runExperiment(rep, duration);
            }
        }
    }

    private static void runExperiment(int replications, double duration) {
        List<Map<String, Double>> allResults = new ArrayList<>();
        String filename = String.format("results_%d_%.0f.csv", replications, duration);

        for (int i = 0; i < replications; i++) {
            GasStationSimulation simulation = new GasStationSimulation();
            simulation.run(duration);
            Map<String, Double> stats = simulation.getStats();
            allResults.add(stats);
        }

        exportAllStats(allResults, filename);
    }

    private Map<String, Double> getStats() {
        Map<String, Double> stats = new HashMap<>();

        // 队列最大值
        stats.put("Pump A Max Queue", (double) maxPumpAQueue);
        stats.put("Pump B Max Queue", (double) maxPumpBQueue);

        // 队列平均值
        stats.put("Avg Queue Length A", calculateAvgQueueLength(queueALengths));
        stats.put("Avg Queue Length B", calculateAvgQueueLength(queueBLengths));
        stats.put("Avg Invoice Queue Length", calculateAvgQueueLength(invoiceQueueLengths));

        // 平均等待时间
        stats.put("Avg Fuel Wait A", pumpAWaitTimes.stream().mapToDouble(d -> d).average().orElse(0));
        stats.put("Avg Fuel Wait B", pumpBWaitTimes.stream().mapToDouble(d -> d).average().orElse(0));
        stats.put("Avg Invoice Wait", invoiceWaitTimes.stream().mapToDouble(d -> d).average().orElse(0));

        // 平均逗留时间
        double avgStayA = stayTimesA.stream().mapToDouble(d -> d).average().orElse(0);
        double avgStayB = stayTimesB.stream().mapToDouble(d -> d).average().orElse(0);
        stats.put("Avg Stay Time A", avgStayA);
        stats.put("Avg Stay Time B", avgStayB);

        // 工作强度（利用率）
        stats.put("Pump A Utilization", (pumpABusyTime / (24 * 60)) * 100);
        stats.put("Pump B Utilization", (pumpBBusyTime / (24 * 60)) * 100);
        stats.put("Clerk Utilization", (clerkBusyTime / (24 * 60)) * 100);

        // 系统结束时各队列等待车辆数
        stats.put("Final Queue A", (double) pumpAQueue.size());
        stats.put("Final Queue B", (double) pumpBQueue.size());
        stats.put("Final Invoice Queue", (double) invoiceQueue.size());

        return stats;
    }

    private static void exportAllStats(List<Map<String, Double>> allResults, String filename) {
        try (FileWriter writer = new FileWriter(filename)) {
            Set<String> metrics = allResults.get(0).keySet();

            writer.write("Simulation #");
            for (String metric : metrics) {
                writer.write("," + metric);
            }
            writer.write("\n");

            for (int i = 0; i < allResults.size(); i++) {
                writer.write(String.format("%d", i + 1));
                Map<String, Double> result = allResults.get(i);
                for (String metric : metrics) {
                    writer.write(String.format(",%.2f", result.get(metric)));
                }
                writer.write("\n");
            }

            writer.write("Average");
            for (String metric : metrics) {
                List<Double> values = allResults.stream()
                        .map(m -> m.get(metric))
                        .collect(Collectors.toList());
                double mean = values.stream().mapToDouble(d -> d).average().orElse(0);
                writer.write(String.format(",%.2f", mean));
            }
            writer.write("\n");

            writer.write("Std Dev");
            for (String metric : metrics) {
                List<Double> values = allResults.stream()
                        .map(m -> m.get(metric))
                        .collect(Collectors.toList());
                double mean = values.stream().mapToDouble(d -> d).average().orElse(0);
                double variance = values.stream()
                        .mapToDouble(d -> Math.pow(d - mean, 2))
                        .average().orElse(0);
                double stdDev = Math.sqrt(variance);
                writer.write(String.format(",%.2f", stdDev));
            }
            writer.write("\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run(double duration) {
        scheduleArrival(0);

        while (!eventQueue.isEmpty() && currentTime <= duration) {
            Event event = eventQueue.poll();
            currentTime = event.time;

            switch (event.type) {
                case "ARRIVAL":
                    handleArrival();
                    break;
                case "FUEL_COMPLETE":
                    handleFuelComplete(event);
                    break;
                case "INVOICE_COMPLETE":
                    handleInvoiceComplete(event);
                    break;
            }

            updateQueueStats();
        }

        // Process remaining events after duration
        while (!eventQueue.isEmpty() && eventQueue.peek().time <= duration) {
            Event event = eventQueue.poll();
            currentTime = event.time;
            // Handle remaining events within duration
        }

        // 在仿真结束时处理剩余车辆
        processRemainingVehicles();
    }

    // 添加成员变量
    private Map<String, Double> stats = new HashMap<>();
    private List<Integer> queueALengths = new ArrayList<>();
    private List<Integer> queueBLengths = new ArrayList<>();
    private List<Integer> invoiceQueueLengths = new ArrayList<>();

    private void processRemainingVehicles() {
        // 统计剩余队列长度
        int remainingPumpA = pumpAQueue.size();
        int remainingPumpB = pumpBQueue.size();
        int remainingInvoice = invoiceQueue.size();

        // 更新统计数据
        stats.put("Final Queue A", (double) remainingPumpA);
        stats.put("Final Queue B", (double) remainingPumpB);
        stats.put("Final Invoice Queue", (double) remainingInvoice);

        // 计算平均队列长度
        stats.put("Avg Queue Length A", calculateAvgQueueLength(queueALengths));
        stats.put("Avg Queue Length B", calculateAvgQueueLength(queueBLengths));
        stats.put("Avg Invoice Queue Length", calculateAvgQueueLength(invoiceQueueLengths));
    }

    private double calculateAvgQueueLength(List<Integer> queueLengths) {
        return queueLengths.stream()
                .mapToDouble(Integer::doubleValue)
                .average()
                .orElse(0.0);
    }

    private void updateQueueStats() {
        // 更新队列长度统计
        queueALengths.add(pumpAQueue.size());
        queueBLengths.add(pumpBQueue.size());
        invoiceQueueLengths.add(invoiceQueue.size());

        // 更新最大队列长度
        maxPumpAQueue = Math.max(maxPumpAQueue, pumpAQueue.size());
        maxPumpBQueue = Math.max(maxPumpBQueue, pumpBQueue.size());
        maxInvoiceQueue = Math.max(maxInvoiceQueue, invoiceQueue.size());
    }

    private void scheduleArrival(double time) {
        double interval = exponential(60 / 21.5);
        if (time + interval <= 24 * 60) { // Ensure within 24 hours
            eventQueue.add(new Event(
                    time + interval,
                    "ARRIVAL",
                    null,
                    null
            ));
        }
    }

    private static final int MAX_QUEUE_SIZE = 3;
    private static final double SERVICE_TIME_REDUCTION = 0.9; // 10% reduction

    private void handleArrival() {
        Vehicle vehicle = new Vehicle();
        vehicle.arrivalTime = currentTime;

        if (Math.random() < 0.7) {
            vehicle.fuelType = "92";
            if (!pumpABusy) {
                startFuelService(vehicle, "A");
                pumpABusy = true;
            } else {
                pumpAQueue.add(vehicle);
            }
        } else {
            vehicle.fuelType = "95";
            if (!pumpBBusy) {
                startFuelService(vehicle, "B");
                pumpBBusy = true;
            } else {
                pumpBQueue.add(vehicle);
            }
        }

        vehicle.needsInvoice = Math.random() < 0.4;

        scheduleArrival(currentTime);

        // 检查队列容量限制
        if (pumpAQueue.size() >= MAX_QUEUE_SIZE || pumpBQueue.size() >= MAX_QUEUE_SIZE) {
            // 处理超出容量的情况
        }
    }

    private void startFuelService(Vehicle vehicle, String pumpId) {
        double serviceTime = exponential(3.7);
        vehicle.fuelServiceStart = currentTime;
        vehicle.fuelServiceEnd = currentTime + serviceTime;

        if (pumpId.equals("A")) {
            pumpABusyTime += serviceTime;
        } else {
            pumpBBusyTime += serviceTime;
        }

        eventQueue.add(new Event(
                vehicle.fuelServiceEnd,
                "FUEL_COMPLETE",
                vehicle,
                pumpId
        ));
    }

    private void handleFuelComplete(Event event) {
        String pumpId = event.pumpId;
        Vehicle vehicle = event.vehicle;

        if (pumpId.equals("A")) {
            pumpABusy = false;
            if (!pumpAQueue.isEmpty()) {
                Vehicle next = pumpAQueue.poll();
                pumpAWaitTimes.add(currentTime - next.arrivalTime);
                startFuelService(next, "A");
                pumpABusy = true;
            }
        } else {
            pumpBBusy = false;
            if (!pumpBQueue.isEmpty()) {
                Vehicle next = pumpBQueue.poll();
                pumpBWaitTimes.add(currentTime - next.arrivalTime);
                startFuelService(next, "B");
                pumpBBusy = true;
            }
        }

        if (vehicle.needsInvoice) {
            vehicle.invoiceQueueEnter = currentTime;
            invoiceQueue.add(vehicle);
            if (!clerkBusy) {
                startInvoiceService();
            }
        }
    }

    private void startInvoiceService() {
        if (invoiceQueue.isEmpty()) return;

        Vehicle vehicle = invoiceQueue.poll();
        clerkBusy = true;

        double serviceTime = (Math.random() < 0.5) ?
                exponential(2.3) : exponential(35.0 / 60);

        clerkBusyTime += serviceTime;
        invoiceWaitTimes.add(currentTime - vehicle.invoiceQueueEnter);

        eventQueue.add(new Event(
                currentTime + serviceTime,
                "INVOICE_COMPLETE",
                vehicle,  // Pass vehicle to event
                null
        ));
    }

    private void handleInvoiceComplete(Event event) {
        if (event != null && event.vehicle != null) {
            completeInvoiceProcessing(event.vehicle);
        }
        clerkBusy = false;
        if (!invoiceQueue.isEmpty()) {
            startInvoiceService();
        }
    }

    // 新增：处理逗留时间统计
    private void completeInvoiceProcessing(Vehicle vehicle) {
        vehicle.totalStayTime = currentTime - vehicle.arrivalTime;
        if ("92".equals(vehicle.fuelType)) {
            stayTimesA.add(vehicle.totalStayTime);
        } else {
            stayTimesB.add(vehicle.totalStayTime);
        }
    }

    private double exponential(double mean) {
        return -mean * Math.log(1 - Math.random());
    }
}