package pvz.model.session.condition;

@FunctionalInterface
public interface WinCondition {
    boolean isSatisfied(WinConditionContext context);
}
