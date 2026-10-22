package restaurantrush.support;

import java.util.Objects;

/**
 * Immutable outcome of a staff action or explicit wait: whether it was accepted and
 * a short message for the CLI. It has no printing logic and no mutators.
 *
 * <p>Owner: M3: Marcus. See docs/class-contracts.md for the implementation contract.
 */
public class ActionResult {
    private final boolean success;
    private final String message;

    private ActionResult(boolean success, String message) {
        this.success = success;
        this.message = Objects.requireNonNull(message, "ActionResult message cannot be null");
    }

    public static ActionResult ok(String message) {
        return new ActionResult(true, message);
    }

    public static ActionResult fail(String message) {
        return new ActionResult(false, message);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }
}
