package com.hp.mechafauna.entity.ai;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.control.MoveControl;

public class CombatMoveControl extends MoveControl {
    private boolean strafing;

    public CombatMoveControl(Mob mob) {
        super(mob);
    }

    @Override
    public void tick() {
        Operation previous = this.operation;
        float yaw = this.mob.getYRot();
        super.tick();
        this.strafing = moveWhileFacingTarget(this.mob, previous, yaw, this.wantedX, this.wantedZ, this.strafing);
    }

    private static boolean moveWhileFacingTarget(Mob mob, Operation operation, float yaw, double wantedX, double wantedZ, boolean wasStrafing) {
        boolean moving = operation == Operation.MOVE_TO || operation == Operation.JUMPING;
        double dx = wantedX - mob.getX();
        double dz = wantedZ - mob.getZ();
        if (!moving || mob.getTarget() == null || mob.isVehicle() || dx * dx + dz * dz < 1.0E-4) {
            if (wasStrafing) {
                mob.setXxa(0.0F);
            }
            return false;
        }
        float desiredYaw = (float)(Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F;
        float delta = Mth.wrapDegrees(desiredYaw - yaw) * Mth.DEG_TO_RAD;
        float speed = mob.getSpeed();
        mob.setYRot(yaw);
        mob.setZza(speed * Mth.cos(delta));
        mob.setXxa(-speed * Mth.sin(delta));
        return true;
    }

    public static class Flying extends FlyingMoveControl {
        private boolean strafing;

        public Flying(Mob mob, int maxTurn, boolean hoversInPlace) {
            super(mob, maxTurn, hoversInPlace);
        }

        @Override
        public void tick() {
            Operation previous = this.operation;
            float yaw = this.mob.getYRot();
            super.tick();
            this.strafing = moveWhileFacingTarget(this.mob, previous, yaw, this.wantedX, this.wantedZ, this.strafing);
        }
    }
}
