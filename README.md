以下是完整的Markdown格式的README文件内容：

# Gas Station Simulation System Documentation

## Project Overview
This program is a discrete event simulation system designed to simulate the operation scenario of a gas station. The system mimics the complete process of vehicle arrival, refueling service, and invoice processing, and collects key performance indicators for operational analysis. It supports multiple sets of parameter configurations and repeated experiments, with the results output in CSV format.

## Core Functions
- **Multi-scenario Simulation**: Supports four simulation durations: 24 hours, 10 days, 30 days, and 1 year.
- **Resource Management**:
  - Two fuel pumps (A - 92 - octane fuel, B - 95 - octane fuel).
  - One cashier (for invoice processing).
- **Queue Management**:
  - Each fuel pump can have a maximum of 3 vehicles in the queue.
  - Dynamic queue statistics.
- **Statistical Indicators**:
  - Queue length (maximum/average).
  - Waiting time (for refueling/invoice).
  - Resource utilization (fuel pumps/cashier).
  - Vehicle stay time.
  - Final queue remaining capacity.

## Class Structure Description

### 1. Vehicle (Vehicle Entity)
```java
static class Vehicle {
    double arrivalTime;        // Arrival time
    String fuelType;           // Fuel type (92/95)
    boolean needsInvoice;      // Whether an invoice is needed
    double fuelServiceStart;   // Start time of refueling service
    double fuelServiceEnd;     // End time of refueling service
    double invoiceQueueEnter;  // Time of entering the invoice queue
    double totalStayTime;      // Total stay time
}
```

### 2. Event (Event Model)
```java
static class Event implements Comparable<Event> {
    double time;     // Event occurrence time
    String type;     // Event type (ARRIVAL/FUEL_COMPLETE/INVOICE_COMPLETE)
    Vehicle vehicle; // Associated vehicle
    String pumpId;   // Fuel pump identifier (A/B)
}
```

### 3. GasStationSimulation (Main Class)
Manages core logics such as event queues, resource status, and statistical collection.

## Key Configuration Parameters
| Parameter Name             | Value              | Description                          |
|----------------------------|--------------------|--------------------------------------|
| Mean interval of vehicle arrival | 21.5 vehicles/hour | Generate interval time using exponential distribution |
| Mean refueling service time | 3.7 minutes        | Exponential distribution             |
| Invoice processing time    | 2.3 minutes (50%) / 35 seconds (50%) | Mixed distribution |
| Maximum queue length       | 3 vehicles         | Maximum number of queuing vehicles allowed for each fuel pump |
| Probability of needing an invoice | 40%              | Probability that a vehicle needs an invoice after refueling |

## Runtime Configuration
```java
public static void main(String[] args) {
    int[] replications = {1, 10, 100, 300};      // Number of repeated experiments
    double[] durations = {                        // Single - experiment duration (minutes)
        24*60,        // 1 day
        10*24*60,    // 10 days
        30*24*60,     // 30 days 
        365*24*60    // 1 year
    };
}
```

## Output Files
- **File Naming**: `results_[Number of replications]_[Duration].csv`
- **Included Indicators**:
  - Maximum length of each queue.
  - Average queue length.
  - Average waiting time (for refueling/invoice).
  - Average stay time.
  - Resource utilization (%).
  - Final queue remaining capacity.
  - Mean and standard deviation of each indicator.

## Statistical Indicator Calculation
```java
// Example indicator calculation logic
stats.put("Pump A Utilization", (pumpABusyTime / (24*60)) * 100);
stats.put("Avg Fuel Wait A", pumpAWaitTimes.stream().average().orElse(0));
stats.put("Avg Stay Time A", stayTimesA.stream().average().orElse(0));
```

## Runtime Example
Output file snippet:
```
Simulation #,Pump A Max Queue,Pump B Max Queue,...,Clerk Utilization
1,2,1,...,68.23
2,3,2,...,71.15
...
Average,2.4,1.8,...,69.82
Std Dev,0.6,0.4,...,1.23
```

## Expansion Instructions
- **Event Processing Flow**:
  1. Vehicle arrives → Joins the corresponding fuel pump queue.
  2. Refueling is completed → Enters the invoice queue (if needed).
  3. Invoice processing is completed → Records the stay time.

- **Special Processing**:
  - Automatically restricts new vehicles from entering when the queue is full.
  - Service time optimization: Automatically accelerates the service speed by 10% when the queue is long.
  - Counts the statistics of unprocessed vehicles at the end into the results.

- **Random Number Generation**:
  ```java
  private double exponential(double mean) {
      return -mean * Math.log(1 - Math.random());
  }
  ```

## Usage Suggestions
1. Adjust the experimental parameter combinations in the `main()` method.
2. Analyze the key indicators in the generated CSV files.
3. Optimize resource allocation based on the fuel pump utilization.
4. Analyze bottleneck links through the waiting time.

# Gas Station Simulation System Documentation (Continued)

## System Architecture and Process

### Architecture Diagram
```
+-------------------+       +-----------------+       +-------------------+
|   Event Queue      | ----> | Resource Management | ----> | Statistical Collection Module |
| (PriorityQueue)    |       | (Fuel Pump A/B, Cashier) |       | (Queue Length, Waiting Time) |
+-------------------+       +-----------------+       +-------------------+
       ^                          |                            |
       |                          v                            v
+-------------------+       +-----------------+       +-------------------+
| Vehicle Generator  |       | Service Processing Logic |       | Result Output Module |
| (Exponential Distribution for Arrival Intervals) |       | (Refueling/Invoice Issuing) |       | (CSV File Generation) |
+-------------------+       +-----------------+       +-------------------+
```

### Core Process
1. **Vehicle Arrival**
   - Generate arrival intervals according to an exponential distribution (average of 21.5 vehicles per hour).
   - Select the fuel pump type (80% choose Pump A for 92 - octane fuel, 20% choose Pump B for 95 - octane fuel).

2. **Refueling Service**
   - If the fuel pump is idle, start service immediately; otherwise, join the queue (maximum of 3 vehicles).
   - Generate service time according to an exponential distribution (mean of 3.7 minutes).
   - **Queue Optimization**: When the queue length ≥ 2, reduce the service time by 10%.

3. **Invoice Processing**
   - 40% of vehicles need an invoice.
   - If the cashier is idle, process the invoice immediately; otherwise, join the queue.
   - Service time follows a mixed distribution:
     - 50% probability: 2.3 minutes (exponential distribution).
     - 50% probability: 35 seconds (exponential distribution).

4. **Statistical Recording**
   - Update indicators such as queue length, resource utilization, and stay time in real - time.

## Key Algorithm Details

### 1. Exponential Distribution Random Number Generation
```java
private double exponential(double mean) {
    return -mean * Math.log(1 - Math.random());
}
```
- **Principle**: Transform a uniform distribution into an exponential distribution using the inverse transformation method.
- **Unit**: Minutes (consistent with the simulation time unit).

### 2. Dynamic Service Time Adjustment
```java
// Trigger optimization when the queue length ≥ MAX_QUEUE_SIZE
if (pumpAQueue.size() >= MAX_QUEUE_SIZE) {
    serviceTime *= SERVICE_TIME_REDUCTION; // Reduce service time by 10%
}
```
- **Effect**: Accelerate the processing of the backlogged queue to prevent system overload.

### 3. Mixed Distribution Processing
```java
double serviceTime = (Math.random() < 0.5) ? 
    exponential(2.3) : exponential(35.0 / 60);
```
- **Unit Conversion**: Convert 35 seconds to minutes (35/60 ≈ 0.583 minutes).

## Exception Handling Mechanism

### Queue Overflow Handling
| Scenario                  | Handling Strategy                          | Code Location               |
|--------------------------|------------------------------------------|-----------------------------|
| Fuel pump queue is full (>3 vehicles) | Reject new vehicles from entering (need to expand the code implementation) | `handleArrival()` method |
| Invoice queue has no limit | Theoretically, it may grow infinitely (actually, an upper limit needs to be set) | `invoiceQueue` definition |

> Note: The current code does not fully implement the queue limit logic. Rejection logic needs to be added in the `handleArrival()` method.

## Optimization and Expansion Suggestions

### 1. Dynamic Resource Allocation
```java
// Example: Dynamically activate a backup fuel pump based on the queue length
if (pumpAQueue.size() > 5) {
    activateBackupPump("A");
}
```

### 2. Time - Sensitive Traffic Flow
```java
// Example: Increase the arrival rate during peak hours
double peakMultiplier = isPeakHour() ? 1.5 : 1.0;
double interval = exponential(60 / (21.5 * peakMultiplier));
```

### 3. Advanced Statistical Functions
- Add statistics on utilization by time period.
- Record the longest single - waiting time.
- Output a heat map analysis of queuing vehicles.

## Complete Runtime Example

### Command - Line Execution
```bash
javac GasStationSimulation.java
java GasStationSimulation
```

### Output File Parsing (Taking results_1_1440.csv as an example)
```csv
Simulation #,Pump A Max Queue,Pump B Max Queue,Avg Queue Length A,...,Clerk Utilization
1,2,1,0.8,...,68.23
Average,2.4,1.8,0.7,...,69.82
Std Dev,0.6,0.4,0.1,...,1.23
```

### Key Indicator Interpretation
| Indicator Name           | Healthy Range       | Optimization Suggestions                          |
|-------------------------|--------------------|-------------------------------------------------|
| Fuel pump utilization    | 70% - 85%          | If > 85%, add more fuel pumps; if < 70%, reduce resources |
| Average invoice waiting time | < 2 minutes       | If > 3 minutes, add more cashiers |
| Maximum queue length     | ≤ 3 vehicles       | If continuously > 3, expand the queue capacity or optimize service efficiency |

## Debugging and Verification

### 1. Verification Method
```java
// Unit test example: Verify exponential distribution generation
void testExponentialDistribution() {
    double sum = 0;
    for (int i = 0; i < 10000; i++) {
        sum += exponential(3.7);
    }
```
