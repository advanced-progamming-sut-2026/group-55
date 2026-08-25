package pvz.model.entity.zombie.behavior;

public final class FreezeResistanceBehavior implements ZombieBehavior {
    @Override
    public boolean convertsFreezeToChill() {
        return true;
    }
}
