package com.mittiandmore.service;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

@Component
public class PincodeZoneClassifier {

    public enum Zone {
        A, B, C, D, E
    }

    private record PrefixRule(String prefix, String state) {}

    /*
     * Compact India-wide postal-region rules. These are intentionally kept as
     * prefix rules rather than a 20k-row snapshot because India Post updates
     * its directory periodically. Explicit shipping_pincodes rows still have
     * priority over these automatic rules.
     */
    private static final List<PrefixRule> STATE_RULES = List.of(
            new PrefixRule("11", "DELHI"),
            new PrefixRule("12", "HARYANA"), new PrefixRule("13", "HARYANA"),
            new PrefixRule("14", "PUNJAB"), new PrefixRule("15", "PUNJAB"), new PrefixRule("16", "PUNJAB"),
            new PrefixRule("17", "HIMACHAL PRADESH"),
            new PrefixRule("18", "JAMMU AND KASHMIR"), new PrefixRule("19", "JAMMU AND KASHMIR"),
            new PrefixRule("20", "UTTAR PRADESH"), new PrefixRule("21", "UTTAR PRADESH"),
            new PrefixRule("22", "UTTAR PRADESH"), new PrefixRule("23", "UTTAR PRADESH"),
            new PrefixRule("244", "UTTARAKHAND"), new PrefixRule("246", "UTTARAKHAND"),
            new PrefixRule("247", "UTTAR PRADESH"), new PrefixRule("248", "UTTARAKHAND"),
            new PrefixRule("249", "UTTARAKHAND"), new PrefixRule("262", "UTTARAKHAND"),
            new PrefixRule("263", "UTTARAKHAND"),
            new PrefixRule("24", "UTTAR PRADESH"), new PrefixRule("25", "UTTAR PRADESH"),
            new PrefixRule("26", "UTTAR PRADESH"), new PrefixRule("27", "UTTAR PRADESH"),
            new PrefixRule("28", "UTTAR PRADESH"),
            new PrefixRule("30", "RAJASTHAN"), new PrefixRule("31", "RAJASTHAN"),
            new PrefixRule("32", "RAJASTHAN"), new PrefixRule("33", "RAJASTHAN"), new PrefixRule("34", "RAJASTHAN"),
            new PrefixRule("36", "GUJARAT"), new PrefixRule("37", "GUJARAT"), new PrefixRule("38", "GUJARAT"), new PrefixRule("39", "GUJARAT"),
            new PrefixRule("40", "MAHARASHTRA"), new PrefixRule("41", "MAHARASHTRA"),
            new PrefixRule("42", "MAHARASHTRA"), new PrefixRule("43", "MAHARASHTRA"), new PrefixRule("44", "MAHARASHTRA"),
            new PrefixRule("45", "MADHYA PRADESH"), new PrefixRule("46", "MADHYA PRADESH"),
            new PrefixRule("47", "MADHYA PRADESH"), new PrefixRule("48", "MADHYA PRADESH"),
            new PrefixRule("49", "CHHATTISGARH"),
            new PrefixRule("50", "TELANGANA"), new PrefixRule("51", "ANDHRA PRADESH"),
            new PrefixRule("52", "ANDHRA PRADESH"), new PrefixRule("53", "ANDHRA PRADESH"),
            new PrefixRule("54", "ANDHRA PRADESH"), new PrefixRule("55", "ANDHRA PRADESH"),
            new PrefixRule("56", "KARNATAKA"), new PrefixRule("57", "KARNATAKA"),
            new PrefixRule("58", "KARNATAKA"), new PrefixRule("59", "KARNATAKA"),
            new PrefixRule("60", "TAMIL NADU"), new PrefixRule("61", "TAMIL NADU"),
            new PrefixRule("62", "TAMIL NADU"), new PrefixRule("63", "TAMIL NADU"),
            new PrefixRule("64", "TAMIL NADU"), new PrefixRule("65", "TAMIL NADU"), new PrefixRule("66", "TAMIL NADU"),
            new PrefixRule("67", "KERALA"), new PrefixRule("68", "KERALA"), new PrefixRule("69", "KERALA"),
            new PrefixRule("70", "WEST BENGAL"), new PrefixRule("71", "WEST BENGAL"),
            new PrefixRule("72", "WEST BENGAL"), new PrefixRule("73", "WEST BENGAL"), new PrefixRule("74", "WEST BENGAL"),
            new PrefixRule("75", "ODISHA"), new PrefixRule("76", "ODISHA"), new PrefixRule("77", "ODISHA"),
            new PrefixRule("78", "ASSAM"), new PrefixRule("79", "NORTH EAST"),
            new PrefixRule("80", "BIHAR"), new PrefixRule("81", "BIHAR"), new PrefixRule("82", "BIHAR"),
            new PrefixRule("83", "JHARKHAND"), new PrefixRule("84", "BIHAR"), new PrefixRule("85", "BIHAR"),
            new PrefixRule("90", "ARMY POSTAL"), new PrefixRule("91", "ARMY POSTAL"),
            new PrefixRule("92", "ARMY POSTAL"), new PrefixRule("93", "ARMY POSTAL"), new PrefixRule("94", "ARMY POSTAL"),
            new PrefixRule("95", "ARMY POSTAL"), new PrefixRule("96", "ARMY POSTAL"), new PrefixRule("99", "ARMY POSTAL")
    );

    private static final List<String> SPECIAL_PREFIXES = List.of(
            "18", "19", "79", "744", "6825", "793", "794", "795", "796", "797", "798", "799"
    );

    /* Common major-city PIN prefixes used only for the Metro-to-Metro rule. */
    private static final List<String> METRO_PREFIXES = List.of(
            "110", "400", "401", "380", "390", "395", "411", "440", "452", "462",
            "500", "560", "600", "620", "641", "682", "700", "800", "226", "208", "302"
    );

    public Zone classify(String destinationPincode, String pickupPincode, String pickupState) {
        String destination = normalize(destinationPincode);
        String pickup = normalize(pickupPincode);
        String pickupStateNormalized = normalizeState(pickupState);

        if (isSpecial(destination)) {
            return Zone.E;
        }

        String destinationState = resolveState(destination);
        if (destinationState == null) {
            return Zone.D;
        }

        if (sameCityPrefix(destination, pickup)) {
            return Zone.A;
        }

        if (isMetro(pickup) && isMetro(destination)) {
            return Zone.C;
        }

        if (pickupStateNormalized != null && sameState(destinationState, pickupStateNormalized)) {
            return Zone.B;
        }

        return Zone.D;
    }

    private boolean sameCityPrefix(String destination, String pickup) {
        return destination.substring(0, 3).equals(pickup.substring(0, 3));
    }

    private boolean isMetro(String pincode) {
        return METRO_PREFIXES.stream().anyMatch(pincode::startsWith);
    }

    private boolean isSpecial(String pincode) {
        return SPECIAL_PREFIXES.stream().anyMatch(prefix -> pincode.startsWith(prefix));
    }

    private String resolveState(String pincode) {
        return STATE_RULES.stream()
                .filter(rule -> pincode.startsWith(rule.prefix()))
                .map(PrefixRule::state)
                .findFirst()
                .orElse(null);
    }

    private boolean sameState(String destinationState, String pickupState) {
        String normalizedPickup = normalizeState(pickupState);
        if (normalizedPickup == null) return false;
        return destinationState.equals(normalizedPickup)
                || (normalizedPickup.equals("UTTAR PRADESH") && destinationState.equals("UTTAR PRADESH"));
    }

    private String normalizeState(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim()
                .toUpperCase(Locale.ROOT)
                .replace('&', ' ')
                .replaceAll("\\s+", " ")
                .replace("JAMMU AND KASHMIR", "JAMMU AND KASHMIR")
                .replace("JAMMU & KASHMIR", "JAMMU AND KASHMIR");
    }

    private String normalize(String pincode) {
        if (pincode == null || !pincode.trim().matches("\\d{6}")) {
            throw new IllegalArgumentException("Pincode must contain exactly 6 digits");
        }
        return pincode.trim();
    }
}
