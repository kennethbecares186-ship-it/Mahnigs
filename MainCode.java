package finalproject;
import java.util.Scanner;
import javax.swing.JOptionPane;

public class MainCode {

    enum Season { LEAN, HIGH, PEAK, SUPER_PEAK }

    static class RoomType {
        final String name;
        final int capacity;
        final int extraBedsAllowed;
        final double[] localPrices; // index 0: LEAN, 1: HIGH, 2: PEAK, 3: SUPER_PEAK
        final double[] intlPrices;  // same indexes for international
        final String description;
        final String[] includedAmenities;

        RoomType(String name, int capacity, int extraBedsAllowed,
                 double[] localPrices, double[] intlPrices,
                 String description, String[] includedAmenities) {
            this.name = name;
            this.capacity = capacity;
            this.extraBedsAllowed = extraBedsAllowed;
            this.localPrices = localPrices;
            this.intlPrices = intlPrices;
            this.description = description;
            this.includedAmenities = includedAmenities;
        }

        double priceFor(Season season, boolean international) {
            int idx = 0;
            if (season == Season.HIGH) idx = 1;
            else if (season == Season.PEAK) idx = 2;
            else if (season == Season.SUPER_PEAK) idx = 3;
            return international ? intlPrices[idx] : localPrices[idx];
        }
    }

    // updated pricing per season as requested
    private static final RoomType[] ROOM_TYPES = {
        new RoomType("Standard", 1, 1,
                new double[]{2000, 4000, 6000, 9000},    // local: lean, high, peak, super peak
                new double[]{2500, 4500, 6500, 10000},   // intl
                "Cozy single room with queen bed. Ideal for solo travelers.",
                new String[]{"Free Wi-Fi", "Complimentary water", "Basic toiletries"}
        ),
        new RoomType("Deluxe", 2, 1,
                new double[]{3000, 5000, 8000, 12000},
                new double[]{5000, 7000, 9000, 13000},
                "Spacious room with king bed or two singles. Great for couples.",
                new String[]{"Free Wi-Fi", "Mini-fridge", "Breakfast voucher"}
        ),
        new RoomType("Quadruple", 4, 1,
                new double[]{4000, 7000, 10000, 15000},
                new double[]{7500, 9500, 11500, 16000},
                "Four-bed room. Good for groups of friends.",
                new String[]{"Free Wi-Fi", "Shared lounge access", "Extra storage"}
        ),
        new RoomType("Family", 6, 1,
                new double[]{5000, 9000, 12000, 18000},
                new double[]{10000, 12000, 14000, 19000},
                "Large family room with flexible bed setup and play area.",
                new String[]{"Free Wi-Fi", "Family dining set", "Kids amenities"}
        ),
        new RoomType("Suite", 4, 1,
                new double[]{6000, 11000, 14000, 21000},
                new double[]{12500, 14500, 16500, 22000},
                "Executive suite with separate living area and premium services.",
                new String[]{"Free Wi-Fi", "Mini-bar", "Welcome fruit basket", "Priority check-in"}
        )
    };

    private static final String[] LOCAL_DESTINATIONS = {"Baguio", "Boracay", "El Nido", "Siargao"};
    private static final String[] INTERNATIONAL_DESTINATIONS = {"Hong Kong", "Japan", "Singapore", "South Korea"};

    // add-on prices per person per night/day
    private static final double PRICE_BED = 650;
    private static final double PRICE_BLANKET = 250;
    private static final double PRICE_PILLOW = 100;
    private static final double PRICE_TOILETRIES = 200;

    // date helpers
    private static boolean isLeapYear(int y) {
        if (y % 400 == 0) return true;
        if (y % 100 == 0) return false;
        return y % 4 == 0;
    }

    private static int daysInMonth(int y, int m) {
        int[] md = {0,31,28,31,30,31,30,31,31,30,31,30,31};
        if (m == 2 && isLeapYear(y)) return 29;
        return md[m];
    }

    private static boolean isValidDate(int y, int m, int d) {
        if (y < 1) return false;
        if (m < 1 || m > 12) return false;
        int dim = daysInMonth(y,m);
        return d >= 1 && d <= dim;
    }

    // days since year 1-01-01 (simple)
    private static long dateToDays(int y, int m, int d) {
        long days = 0;
        int year = 1;
        for (int yy = 1; yy < y; yy++) {
            days += isLeapYear(yy) ? 366 : 365;
        }
        for (int mm = 1; mm < m; mm++) days += daysInMonth(y, mm);
        days += d - 1;
        return days;
    }

    private static int[] nextDate(int y, int m, int d) {
        d++;
        if (d > daysInMonth(y,m)) {
            d = 1;
            m++;
            if (m > 12) { m = 1; y++; }
        }
        return new int[]{y,m,d};
    }

    // determine season by month/day via if-else
    private static Season seasonForMonthDay(int month, int day) {
        // SUPER_PEAK: Dec 20 - Dec 31 and Jan 1 - Jan 5
        if ((month == 12 && day >= 20) || (month == 1 && day <= 5)) return Season.SUPER_PEAK;
        // PEAK: Apr 1-10, Dec 10-19
        if ((month == 4 && day >= 1 && day <= 10) || (month == 12 && day >= 10 && day <= 19)) return Season.PEAK;
        // HIGH: Jun 1 - Aug 31, Nov 1 - Nov 30
        if ((month >= 6 && month <= 8) || (month == 11)) return Season.HIGH;
        return Season.LEAN;
    }

    private static Season determineSeasonForStay(int inY, int inM, int inD, int outY, int outM, int outD) {
        // iterate day by day from check-in (inclusive) to check-out (exclusive)
        long inDays = dateToDays(inY, inM, inD);
        long outDays = dateToDays(outY, outM, outD);
        boolean hasSuper = false, hasPeak = false, hasHigh = false;
        if (outDays <= inDays) return Season.LEAN; // invalid stay length; caller should check
        int cy = inY, cm = inM, cd = inD;
        for (long s = inDays; s < outDays; s++) {
            Season se = seasonForMonthDay(cm, cd);
            if (se == Season.SUPER_PEAK) hasSuper = true;
            else if (se == Season.PEAK) hasPeak = true;
            else if (se == Season.HIGH) hasHigh = true;
            int[] nx = nextDate(cy, cm, cd);
            cy = nx[0]; cm = nx[1]; cd = nx[2];
        }
        if (hasSuper) return Season.SUPER_PEAK;
        if (hasPeak) return Season.PEAK;
        if (hasHigh) return Season.HIGH;
        return Season.LEAN;
    }

    // ------- input helpers -------
    private static int askInt(Scanner sc, String prompt, int min, int maxAllowed) {
        System.out.print(prompt);
        while (true) {
            String s = sc.nextLine().trim();
            try {
                int v = Integer.parseInt(s);
                if (v >= min && (maxAllowed < 0 || v <= maxAllowed)) return v;
            } catch (Exception ignored) {}
            if (maxAllowed < 0) System.out.print("Enter an integer >= " + min + ": ");
            else System.out.print("Enter an integer between " + min + " and " + maxAllowed + ": ");
        }
    }

    private static int[] askDateParts(Scanner sc, String label) {
        int y, m, d;
        while (true) {
            System.out.println(label + ":");
            y = askInt(sc, " Year (e.g. 2025): ", 1, -1);
            m = askInt(sc, " Month (1-12): ", 1, 12);
            d = askInt(sc, " Day (1-" + daysInMonth(y,m) + "): ", 1, daysInMonth(y,m));
            if (!isValidDate(y,m,d)) {
                System.out.println("Invalid date. Try again.");
                continue;
            }
            break;
        }
        return new int[]{y,m,d};
    }

    // ------- room allocation suggestions using arrays only -------
    private static String[] buildAllocation(int adults, int children, int infants, int bedsPerRoom, int maxAdultsPerRoom) {
        // worst case rooms = adults + children (one per room)
        String[] rooms = new String[adults + children + (infants>0?1:0)];
        int cnt = 0;
        while (adults > 0 || children > 0) {
            int adultsInRoom = adults;
            if (adultsInRoom > maxAdultsPerRoom) adultsInRoom = maxAdultsPerRoom;
            adults -= adultsInRoom;
            int space = bedsPerRoom - adultsInRoom;
            int childrenInRoom = children;
            if (childrenInRoom > space) childrenInRoom = space;
            children -= childrenInRoom;
            rooms[cnt++] = "Adults: " + adultsInRoom + ", Children (need bed): " + childrenInRoom;
        }
        if (cnt == 0 && infants > 0) {
            rooms[cnt++] = "Adults: 0, Children (need bed): 0 (infants only)";
        }
        // trim
        String[] out = new String[cnt];
        for (int i = 0; i < cnt; i++) out[i] = rooms[i];
        return out;
    }

    // ------- main flow -------
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        System.out.println("*** Lanlya Star Hotel - Reservation System ***");

        // booker details
        System.out.print("Enter your full name: ");
        String bookerName = sc.nextLine().trim();
        System.out.print("Enter your email: ");
        String bookerEmail = sc.nextLine().trim();
        System.out.print("Enter your contact number: ");
        String bookerContact = sc.nextLine().trim();
        int bookerAge = askInt(sc, "Enter your age (must be 18 or older): ", 0, 200);
        if (bookerAge < 18) {
            System.out.println("ERROR: Booker must be at least 18 years old. Reservation denied.");
            sc.close();
            return;
        }

        // ask today's date so we can validate past dates
        System.out.println("\nProvide today's date for validation.");
        int[] today = askDateParts(sc, "Today's date");

        // destination
        System.out.println("\nChoose destination type:");
        System.out.println(" [1] Local");
        System.out.println(" [2] International");
        String destType = "";
        while (true) {
            System.out.print("Enter 1 or 2: ");
            destType = sc.nextLine().trim();
            if ("1".equals(destType) || "2".equals(destType)) break;
            System.out.println("Invalid input.");
        }
        String destination = "";
        boolean international = false;
        if ("1".equals(destType)) {
            for (int i = 0; i < LOCAL_DESTINATIONS.length; i++) System.out.println(" [" + (i+1) + "] " + LOCAL_DESTINATIONS[i]);
            int pick = askInt(sc, "Pick local destination number: ", 1, LOCAL_DESTINATIONS.length);
            destination = LOCAL_DESTINATIONS[pick-1];
            international = false;
        } else {
            for (int i = 0; i < INTERNATIONAL_DESTINATIONS.length; i++) System.out.println(" [" + (i+1) + "] " + INTERNATIONAL_DESTINATIONS[i]);
            int pick = askInt(sc, "Pick international destination number: ", 1, INTERNATIONAL_DESTINATIONS.length);
            destination = INTERNATIONAL_DESTINATIONS[pick-1];
            international = true;
        }

        // check-in and check-out
        System.out.println("\nEnter check-in date:");
        int[] in = askDateParts(sc, "Check-in date");
        System.out.println("\nEnter check-out date:");
        int[] out = askDateParts(sc, "Check-out date");

        long inDays = dateToDays(in[0], in[1], in[2]);
        long outDays = dateToDays(out[0], out[1], out[2]);
        long todayDays = dateToDays(today[0], today[1], today[2]);

        if (inDays < todayDays) {
            System.out.println("ERROR: Check-in date is in the past. Reservation denied.");
            sc.close();
            return;
        }
        if (outDays <= inDays) {
            System.out.println("ERROR: Check-out must be after check-in. Reservation denied.");
            sc.close();
            return;
        }

        int nights = (int)(outDays - inDays);

        Season detectedSeason = determineSeasonForStay(in[0], in[1], in[2], out[0], out[1], out[2]);
        System.out.println("Detected season for stay: " + detectedSeason);

        // number of rooms to reserve
        int numRooms = askInt(sc, "\nEnter number of rooms to reserve: ", 1, -1);

        // guest details - count adults and children
        System.out.println("\nEnter guest details:");
        int numAdults = askInt(sc, "Number of adults (18 and above): ", 0, -1);
        int numChildren = askInt(sc, "Number of children (0-17): ", 0, -1);

        if (numAdults == 0 && numChildren == 0) {
            System.out.println("ERROR: At least one guest is required. Reservation denied.");
            sc.close();
            return;
        }

        // if there are children, ask for their ages
        int[] childAges = new int[numChildren];
        int actualAdultCount = numAdults;
        for (int i = 0; i < numChildren; i++) {
            int age = askInt(sc, " Age of child " + (i+1) + ": ", 0, 17);
            childAges[i] = age;
            // If child is 12 or older, count as adult for room allocation
            if (age >= 12) {
                actualAdultCount++;
            }
        }

        int totalGuests = actualAdultCount + numChildren;
        RoomType chosenRoom = null;
        for (RoomType rt : ROOM_TYPES) {
            if (totalGuests <= numRooms * rt.capacity) {
                chosenRoom = rt;
                break;
            }
        }
        
        if (chosenRoom == null) {
            System.out.println("ERROR: No room combination can accommodate " + totalGuests + " guests in " + numRooms + " room(s). Reservation denied.");
            sc.close();
            return;
        }

        // Generate room type suggestion based on guest count and number of rooms
        RoomType[] suggestedRooms = generateRoomSuggestion(totalGuests, numRooms);
        
        System.out.println("\n========== ROOM SUGGESTION ==========");
        System.out.println("Based on " + totalGuests + " guests and " + numRooms + " room(s) reserved:");
        for (int i = 0; i < suggestedRooms.length; i++) {
            System.out.println(" Room #" + (i + 1) + ": " + suggestedRooms[i].name);
            System.out.println("   Capacity: " + suggestedRooms[i].capacity + " guests");
            System.out.println("   " + suggestedRooms[i].description);
        }

        buildAllocation(numAdults, numChildren, nights, numRooms, numAdults);
        
        int acceptSuggestion = askInt(sc, "\nDo you accept this suggestion? (1=Yes, 0=No): ", 0, 1);
        
        RoomType[] selectedRooms = new RoomType[numRooms];
        String[] preferredProfiles = new String[numRooms];
        int totalCapacity = 0;

       
        
        // Track available room inventory per type
        java.util.Map<String, Integer> roomInventory = new java.util.HashMap<>();
        roomInventory.put("Standard", 5);
        roomInventory.put("Deluxe", 4);
        roomInventory.put("Quadruple", 5);
        roomInventory.put("Family", 3);
        roomInventory.put("Suite", 2);

        if (acceptSuggestion == 1) {
            // User accepts suggestion - use suggested rooms and proceed to room instances
            for (int r = 0; r < numRooms; r++) {
                selectedRooms[r] = suggestedRooms[r];
                totalCapacity += suggestedRooms[r].capacity;
                
                // Show available room numbers for this type and let user pick
                int available = roomInventory.get(suggestedRooms[r].name);
                if (available <= 0) {
                    System.out.println("ERROR: No rooms available for " + suggestedRooms[r].name + ". Reservation denied.");
                    sc.close();
                    return;
                }

                System.out.println("\n=== Room #" + (r + 1) + " (" + suggestedRooms[r].name + ") ===");
                System.out.println("Available room numbers:");
                String[] roomNumbers = generateRoomNumbers(suggestedRooms[r], available);
                for (int i = 0; i < roomNumbers.length; i++) {
                    System.out.println(" [" + (i + 1) + "] " + roomNumbers[i]);
                }
                
                int roomPick = askInt(sc, "Pick a specific room number (1-" + roomNumbers.length + "): ", 1, roomNumbers.length);
                String selectedRoomNumber = roomNumbers[roomPick - 1];
                
                // Update inventory
                roomInventory.put(suggestedRooms[r].name, available - 1);
                
                System.out.println("Selected: " + suggestedRooms[r].name + " - " + selectedRoomNumber);
                preferredProfiles[r] = selectedRoomNumber;
            }
        } else {
            // User rejects suggestion - show all available room types for selection
            System.out.println("\nSelect room type and instance for each reserved room:");
            for (int r = 0; r < numRooms; r++) {
                System.out.println("\n=== Room #" + (r + 1) + " ===");
                
                // Show all room types with available count
                for (int i = 0; i < ROOM_TYPES.length; i++) {
                    RoomType rt = ROOM_TYPES[i];
                    int available = roomInventory.get(rt.name);
                    System.out.println(" [" + (i + 1) + "] " + rt.name + " (Available: " + available + " rooms)");
                    System.out.println("     Capacity: " + rt.capacity + " guests | Extra beds allowed: " + rt.extraBedsAllowed);
                    System.out.println("     " + rt.description);
                    System.out.print("     Included: ");
                    for (int k = 0; k < rt.includedAmenities.length; k++) {
                        System.out.print(rt.includedAmenities[k]);
                        if (k < rt.includedAmenities.length - 1) System.out.print(", ");
                    }
                    System.out.println();
                }
                
                // User picks room type
                int pick = askInt(sc, "Pick room type number for Room #" + (r + 1) + " (1-" + ROOM_TYPES.length + "): ", 1, ROOM_TYPES.length);
                RoomType selectedType = ROOM_TYPES[pick - 1];
                selectedRooms[r] = selectedType;
                totalCapacity += selectedType.capacity;

                // Show available room numbers for this type and let user pick
                int available = roomInventory.get(selectedType.name);
                if (available <= 0) {
                    System.out.println("ERROR: No rooms available for " + selectedType.name + ". Reservation denied.");
                    sc.close();
                    return;
                }

                System.out.println("\nAvailable room numbers for " + selectedType.name + ":");
                String[] roomNumbers = generateRoomNumbers(selectedType, available);
                for (int i = 0; i < roomNumbers.length; i++) {
                    System.out.println(" [" + (i + 1) + "] " + roomNumbers[i]);
                }
                
                int roomPick = askInt(sc, "Pick a specific room number (1-" + roomNumbers.length + "): ", 1, roomNumbers.length);
                String selectedRoomNumber = roomNumbers[roomPick - 1];
                
                // Update inventory
                roomInventory.put(selectedType.name, available - 1);
                
                System.out.println("Selected: " + selectedType.name + " - " + selectedRoomNumber);
                preferredProfiles[r] = selectedRoomNumber;
            }
        }

        if (totalCapacity < totalGuests) {
            System.out.println("ERROR: Total guests (" + totalGuests + ") exceed total capacity of selected rooms (" + totalCapacity + "). Reservation denied.");
            sc.close();
            return;
        }

        // ask extra beds (as add-on) but limit by allowed per room (sum of allowed per selected room)
        int maxExtraAllowed = 0;
        for (RoomType rt : selectedRooms) maxExtraAllowed += rt.extraBedsAllowed;
        int extraBeds = askInt(sc, "Enter number of extra beds (max " + maxExtraAllowed + "): ", 0, maxExtraAllowed);

        // validate total capacity including extra beds
        int capacity = totalCapacity + extraBeds;
        if (totalGuests > capacity) {
            System.out.println("ERROR: Total guests (" + totalGuests + ") exceed room capacity (" + capacity + "). Reservation denied.");
            sc.close();
            return;
        }

        // compute per-room price & subtotal (per night)
        double[] roomPricePerNightArr = new double[numRooms];
        double roomSubtotal = 0.0;
        for (int r = 0; r < numRooms; r++) {
            roomPricePerNightArr[r] = selectedRooms[r].priceFor(detectedSeason, international);
            roomSubtotal += roomPricePerNightArr[r] * nights;
        }

        // show each reserved room and its features BEFORE asking amenities
        System.out.println("\nReserved rooms and their features:");
        for (int r = 0; r < numRooms; r++) {
            RoomType rt = selectedRooms[r];
            System.out.println("\nRoom #" + (r + 1) + ": " + rt.name + " - " + preferredProfiles[r]);
            System.out.println(" Description: " + rt.description);
            System.out.println(" Included features:");
            for (String feat : rt.includedAmenities) {
                System.out.println("  - " + feat);
            }
            System.out.println(" Price per night: PHP " + String.format("%.2f", roomPricePerNightArr[r]));
        }

        // per-room amenities input
        System.out.println("\nNow enter amenities per room:");
        String[] amenNames = {"Extra bed", "Blanket", "Pillow", "Toiletries"};
        double[] amenPrice = {PRICE_BED, PRICE_BLANKET, PRICE_PILLOW, PRICE_TOILETRIES};
        int roomsCount = numRooms;
        int amenCount = amenNames.length;

        int[][] amenPersonsPerRoom = new int[roomsCount][amenCount];
        int[][] amenPWDPerRoom = new int[roomsCount][amenCount];
        int[][] amenDaysPerRoom = new int[roomsCount][amenCount];

        for (int r = 0; r < roomsCount; r++) {
            System.out.println("\n--- Amenities for Room #" + (r + 1) + " (" + selectedRooms[r].name + " - " + preferredProfiles[r] + ") ---");
            int roomMaxPersons = selectedRooms[r].capacity;
            for (int a = 0; a < amenCount; a++) {
                System.out.println("\nAmenity: " + amenNames[a] + " (price per person per night/day: PHP " + amenPrice[a] + ")");
                amenPersonsPerRoom[r][a] = askInt(sc, " Number of persons availing in this room (0-" + roomMaxPersons + "): ", 0, roomMaxPersons);
                if (amenPersonsPerRoom[r][a] > 0) {
                    amenPWDPerRoom[r][a] = askInt(sc, "  Number of PWD/Senior availing (for 20% discount) (0-" + amenPersonsPerRoom[r][a] + "): ", 0, amenPersonsPerRoom[r][a]);
                    amenDaysPerRoom[r][a] = askInt(sc, "  Number of days to avail (1-" + nights + "): ", 1, nights);
                } else {
                    amenPWDPerRoom[r][a] = 0;
                    amenDaysPerRoom[r][a] = 0;
                }
            }
        }

        // compute amenities totals across rooms
        double amenSubtotal = 0.0;
        double amenDiscountTotal = 0.0;
        StringBuilder perRoomAmenSummary = new StringBuilder();
 
        // aggregated arrays for summary display
        int[] amenPersonsAgg = new int[amenCount];
        int[] amenPWDAgg = new int[amenCount];
        int[] amenDaysAgg = new int[amenCount];
 
        for (int r = 0; r < roomsCount; r++) {
            perRoomAmenSummary.append("\nRoom #").append(r + 1).append(" amenities:\n");
            boolean any = false;
            for (int a = 0; a < amenCount; a++) {
                int persons = amenPersonsPerRoom[r][a];
                int pwd = amenPWDPerRoom[r][a];
                int days = amenDaysPerRoom[r][a];
                if (persons == 0) continue;
                any = true;
                double total = amenPrice[a] * persons * days;
                double discount = 0.2 * amenPrice[a] * pwd * days;
                amenSubtotal += total;
                amenDiscountTotal += discount;
                perRoomAmenSummary.append("  - ").append(amenNames[a])
                    .append(": Persons=").append(persons)
                    .append(", PWD=").append(pwd)
                    .append(", Days=").append(days)
                    .append(", Unit PHP ").append(String.format("%.2f", amenPrice[a]))
                    .append(", Subtotal PHP ").append(String.format("%.2f", total))
                    .append(", Discount PHP ").append(String.format("%.2f", discount))
                    .append("\n");

                // aggregate
                amenPersonsAgg[a] += persons;
                amenPWDAgg[a] += pwd;
                amenDaysAgg[a] = Math.max(amenDaysAgg[a], days); // keep representative days (or could sum)
            }
            if (!any) perRoomAmenSummary.append("  (no amenities selected for this room)\n");
        }

        double amenFinal = amenSubtotal - amenDiscountTotal;
        double grandTotal = roomSubtotal + amenFinal;

        // payment
        System.out.println("\nPayment options:");
        System.out.println(" [1] Cash");
        System.out.println(" [2] Credit/Debit card");
        int payMethod = askInt(sc, "Choose payment method (1 or 2): ", 1, 2);
        boolean paid = false;
        double change = 0.0; // track change to show in summary if any
        if (payMethod == 1) {
            // cash
            while (true) {
                System.out.printf("Total due: %.2f%n", grandTotal);
                System.out.print("Enter cash amount paid: ");
                String s = sc.nextLine().trim();
                double paidAmt;
                try { paidAmt = Double.parseDouble(s); } catch (Exception ex) { paidAmt = -1; }
                if (paidAmt < 0) { System.out.println("Invalid amount."); continue; }
                if (paidAmt + 0.0001 < grandTotal) {
                    System.out.println("ERROR: Insufficient amount. Please pay at least the total due.");
                    continue;
                }
                change = paidAmt - grandTotal;
                System.out.printf("Cash accepted. Change: %.2f%n", change);
                paid = true;
                break;
            }
        } else {
            // card
            while (true) {
                System.out.print("Enter 16-digit card number: ");
                String card = sc.nextLine().trim();
                if (card.length() != 16) { System.out.println("ERROR: Card number must be 16 digits."); continue; }
                boolean allDigits = true;
                for (int i = 0; i < 16; i++) if (!Character.isDigit(card.charAt(i))) allDigits = false;
                if (!allDigits) { System.out.println("ERROR: Card number must contain only digits."); continue; }
                System.out.print("Enter 3-digit CVV: ");
                String cvv = sc.nextLine().trim();
                if (cvv.length() != 3) { System.out.println("ERROR: CVV must be 3 digits."); continue; }
                boolean cvvDigits = true;
                for (int i = 0; i < 3; i++) if (!Character.isDigit(cvv.charAt(i))) cvvDigits = false;
                if (!cvvDigits) { System.out.println("ERROR: CVV must contain only digits."); continue; }
                // simulate charge
                System.out.printf("Card charged successfully for %.2f%n", grandTotal);
                paid = true;
                break;
            }
        }

        if (!paid) {
            System.out.println("Payment not completed. Reservation cancelled.");
            sc.close();
            return;
        }

        // booking summary - centered top/bottom only (fixed)
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%80s%n%n", "========== BOOKING SUMMARY =========="));
        sb.append("Booker: ").append(bookerName).append('\n');
        sb.append("Email: ").append(bookerEmail).append('\n');
        sb.append("Contact: ").append(bookerContact).append('\n');
        sb.append("Age: ").append(bookerAge).append('\n');
        sb.append("Check-in: ").append(String.format("%04d-%02d-%02d", in[0], in[1], in[2])).append('\n');
        sb.append("Check-out: ").append(String.format("%04d-%02d-%02d", out[0], out[1], out[2])).append('\n');
        sb.append("Nights: ").append(nights).append('\n');
        sb.append("Destination: ").append(destination).append(" (").append(international ? "International" : "Local").append(")").append('\n');
        sb.append("Season: ").append(detectedSeason).append('\n').append('\n');

        sb.append("Room Booking Details:\n");
        for (int r = 0; r < numRooms; r++) {
            sb.append(" Room #").append(r + 1).append(": ").append(selectedRooms[r].name);
            if (preferredProfiles != null && preferredProfiles[r] != null) sb.append(" - ").append(preferredProfiles[r]);
            sb.append('\n');
            sb.append(String.format("   Price per night: PHP %.2f%n", roomPricePerNightArr[r]));
            sb.append(String.format("   Total for %d nights: PHP %.2f%n", nights, roomPricePerNightArr[r] * nights));
        }
        sb.append(String.format("%nTotal Room Subtotal: PHP %.2f%n", roomSubtotal)).append('\n');

        sb.append("Add-ons & Amenities summary (per room):\n");
        if (perRoomAmenSummary.length() > 0) sb.append(perRoomAmenSummary.toString()).append('\n');

        // aggregated amen summary
        for (int i = 0; i < amenCount; i++) {
            if (amenPersonsAgg[i] == 0) continue;
            double subtotal = amenPrice[i] * amenPersonsAgg[i] * (amenDaysAgg[i] > 0 ? amenDaysAgg[i] : 1);
            sb.append(String.format("  %s -> Persons: %d, PWD/Senior: %d, Days: %d, Unit price: PHP %.2f, Subtotal: PHP %.2f%n",
                    amenNames[i],
                    amenPersonsAgg[i],
                    amenPWDAgg[i],
                    amenDaysAgg[i],
                    amenPrice[i],
                    subtotal
            ));
        }

        sb.append(String.format("%nAmenities Subtotal: PHP %.2f%n", amenSubtotal));
        sb.append(String.format("Discount (20%% PWD/Senior): -PHP %.2f%n", amenDiscountTotal));
        sb.append(String.format("Amenities Total: PHP %.2f%n%n", amenFinal));

        sb.append(String.format("GRAND TOTAL: PHP %.2f%n", grandTotal));
        if (change > 0.0) sb.append(String.format("Change Returned: PHP %.2f%n", change));

        sb.append('\n').append(String.format("%80s%n", "========== Thank You =========="));
        sb.append(String.format("%70s%n", "Booking Completed Successfully"));

        JOptionPane.showMessageDialog(null, sb.toString(), "Booking Confirmation", JOptionPane.INFORMATION_MESSAGE);
        sc.close();
    }

    private static RoomType[] generateRoomSuggestion(int totalGuests, int numRooms) {
        RoomType[] suggested = new RoomType[numRooms];
        
        // Calculate guests per room
        int guestsPerRoom = totalGuests / numRooms;
        int remainderGuests = totalGuests % numRooms;
        
        // Find suitable room types
        for (int r = 0; r < numRooms; r++) {
            int guestCount = guestsPerRoom + (r < remainderGuests ? 1 : 0);
            
            // Find smallest room that fits this guest count
            RoomType bestRoom = null;
            for (RoomType rt : ROOM_TYPES) {
                if (rt.capacity >= guestCount) {
                    if (bestRoom == null || rt.capacity < bestRoom.capacity) {
                        bestRoom = rt;
                    }
                }
            }
            
            suggested[r] = bestRoom != null ? bestRoom : ROOM_TYPES[0]; // fallback to first room
        }
        
        return suggested;
    }

    private static String[] generateRoomNumbers(RoomType rt, int availableCount) {
        String[] roomNumbers = new String[availableCount];
        
        int baseNumber = 0;
        if (null != rt.name) switch (rt.name) {
            case "Standard":
                baseNumber = 100;
                break;
            case "Deluxe":
                baseNumber = 200;
                break;
            case "Quadruple":
                baseNumber = 300;
                break;
            case "Family":
                baseNumber = 400;
                break;
            case "Suite":
                baseNumber = 500;
                break;
            default:
                break;
        }
        
        for (int i = 0; i < availableCount; i++) {
            int roomNum = baseNumber + (i + 1);
            int floor = baseNumber / 100;
            roomNumbers[i] = "Room " + roomNum + " (Floor " + floor + ")";
        }
        
        return roomNumbers;
    }
}