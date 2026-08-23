package pvz.model.adventure;

import java.util.Objects;

public record ChapterSpec(
        String id,
        String name,
        int order
) {
    public ChapterSpec {
        id = requireText(id, "chapter id");
        name = requireText(name, "chapter name");
        if (order <= 0) {
            throw new IllegalArgumentException(
                    "chapter order must be positive"
            );
        }
    }

    private static String requireText(String value, String field) {
        Objects.requireNonNull(value, field + " cannot be null");
        String result = value.strip();
        if (result.isEmpty()) {
            throw new IllegalArgumentException(field + " cannot be blank");
        }
        return result;
    }
}
