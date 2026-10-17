package restaurantrush.support;

/**
 * Stage 1 skeleton. Owner: M3: Marcus.
 * See docs/class-contracts.md for the implementation contract.
 */
public class ActionResult {
    private boolean success;
    private String message;

    public static ActionResult ok(String message) {
        throw new UnsupportedOperationException("ActionResult.ok is not implemented yet");
    }

    public static ActionResult fail(String message) {
        throw new UnsupportedOperationException("ActionResult.fail is not implemented yet");
    }

    public boolean isSuccess() {
        throw new UnsupportedOperationException("ActionResult.isSuccess is not implemented yet");
    }

    public String getMessage() {
        throw new UnsupportedOperationException("ActionResult.getMessage is not implemented yet");
    }
}
