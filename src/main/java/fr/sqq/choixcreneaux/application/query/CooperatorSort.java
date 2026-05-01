package fr.sqq.choixcreneaux.application.query;

public record CooperatorSort(Field field, Direction direction) {

    public static final CooperatorSort DEFAULT = new CooperatorSort(Field.NAME, Direction.ASC);

    public enum Field { NAME, EMAIL, LAST_REMINDER }

    public enum Direction { ASC, DESC }

    public static CooperatorSort of(String field, String direction) {
        Field f = parseField(field);
        Direction d = parseDirection(direction);
        return new CooperatorSort(f, d);
    }

    private static Field parseField(String s) {
        if (s == null || s.isBlank()) return Field.NAME;
        return switch (s.trim()) {
            case "name", "lastName" -> Field.NAME;
            case "email" -> Field.EMAIL;
            case "lastReminder", "lastReminderAt" -> Field.LAST_REMINDER;
            default -> Field.NAME;
        };
    }

    private static Direction parseDirection(String s) {
        if (s == null) return Direction.ASC;
        return "desc".equalsIgnoreCase(s.trim()) ? Direction.DESC : Direction.ASC;
    }
}
