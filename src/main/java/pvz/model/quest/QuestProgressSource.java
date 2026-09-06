package pvz.model.quest;

import pvz.model.account.User;

/**
 * Adapter used by QuestService to read measurable progress from an existing
 * model snapshot without depending on a screen or a gameplay implementation.
 */
public interface QuestProgressSource {
    boolean supports(QuestMetric metric);

    int currentValue(User user, QuestObjective objective);
}
