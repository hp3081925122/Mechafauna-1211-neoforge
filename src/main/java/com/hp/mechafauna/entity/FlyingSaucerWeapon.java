package com.hp.mechafauna.entity;

public enum FlyingSaucerWeapon {
    STARBOARD_GATLING("starboard_gatling_yaw", "starboard_gatling_pitch", "starboard_gatling_muzzle_", "starboard_gatling_barrel_spin", false,
        WeaponMount.point(-54, 30.5, 0), WeaponMount.point(-54, 24, 0)),
    PORT_GATLING("port_gatling_yaw", "port_gatling_pitch", "port_gatling_muzzle_", "port_gatling_barrel_spin", false,
        WeaponMount.point(54, 30.5, 0), WeaponMount.point(54, 24, 0)),
    FORE_MISSILE("fore_missile_yaw", "fore_missile_pitch", "fore_missile_cell_", null, true,
        WeaponMount.point(0, 30.5, -54), WeaponMount.point(0, 24, -54)),
    AFT_MISSILE("aft_missile_yaw", "aft_missile_pitch", "aft_missile_cell_", null, true,
        WeaponMount.point(0, 30.5, 54), WeaponMount.point(0, 24, 54));

    private final String yawBone;
    private final String pitchBone;
    private final String muzzleBonePrefix;
    private final String barrelBone;
    private final boolean missile;
    private final WeaponMount mount;

    FlyingSaucerWeapon(String yawBone, String pitchBone, String muzzleBonePrefix, String barrelBone, boolean missile,
                       net.minecraft.world.phys.Vec3 yawPivot, net.minecraft.world.phys.Vec3 pitchPivot) {
        this.yawBone = yawBone;
        this.pitchBone = pitchBone;
        this.muzzleBonePrefix = muzzleBonePrefix;
        this.barrelBone = barrelBone;
        this.missile = missile;
        this.mount = new WeaponMount("flying_saucer_boss", yawBone, pitchBone, yawPivot, pitchPivot, yawPivot);
    }

    public String yawBone() {
        return this.yawBone;
    }

    public String pitchBone() {
        return this.pitchBone;
    }

    public String barrelBone() {
        return this.barrelBone;
    }

    public boolean isMissile() {
        return this.missile;
    }

    public WeaponMount mount() {
        return this.mount;
    }

    public String muzzleBone(int index) {
        return this.missile
            ? this.muzzleBonePrefix + (index / 3) + "_" + (index % 3) + "_launch"
            : this.muzzleBonePrefix + index;
    }
}
