package com.hp.mechafauna.entity;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4d;

public final class WeaponPose {
    private static final Map<String, WeaponPose> MODELS = new HashMap<>();
    private final Map<String, Bone> bones = new HashMap<>();
    private final Map<String, Map<String, Track>> animations = new HashMap<>();
    private final JsonObject animationData;

    private WeaponPose(String modelName) {
        JsonArray sourceBones = read("geo/" + modelName + ".geo.json")
            .getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject().getAsJsonArray("bones");
        for (JsonElement entry : sourceBones) {
            JsonObject bone = entry.getAsJsonObject();
            this.bones.put(bone.get("name").getAsString(), new Bone(
                bone.has("parent") ? bone.get("parent").getAsString() : null,
                vector(bone.get("pivot"), Vec3.ZERO).multiply(-1, 1, 1),
                vector(bone.get("rotation"), Vec3.ZERO)));
        }
        this.animationData = read("animations/" + modelName + ".animation.json").getAsJsonObject("animations");
    }

    public static WeaponPose forModel(String modelName) {
        return MODELS.computeIfAbsent(modelName, WeaponPose::new);
    }

    public Matrix4d parentTransform(WeaponMount mount, Matrix4d modelToWorld, String animation, double seconds) {
        this.applyBone(modelToWorld, this.bones.get(mount.yawBone()).parent(), animation, seconds);
        Vec3 position = this.sample(animation, mount.yawBone(), "position", seconds, Vec3.ZERO);
        return modelToWorld.translate(-position.x, position.y, position.z);
    }

    public Vec3 muzzle(WeaponMount mount, Vec3 muzzle, String animation, double seconds) {
        String recoil = mount == WeaponMount.MECH_MISSILES || mount == WeaponMount.FLAME_CRAWLER ? null
            : mount == WeaponMount.TANK_CANNON ? "barrel_recoil" : "gun_recoil";
        Vec3 animatedMuzzle = mount == WeaponMount.MECH_GUN
            ? WeaponMount.transform(this.boneTransform("gun_spin", animation, seconds), muzzle)
            : muzzle;
        return recoil == null ? animatedMuzzle : WeaponMount.transform(this.boneTransform(recoil, animation, seconds), animatedMuzzle);
    }

    private void applyBone(Matrix4d matrix, String name, String animation, double seconds) {
        if (name == null) {
            return;
        }
        this.applyBone(matrix, this.bones.get(name).parent(), animation, seconds);
        matrix.mul(this.boneTransform(name, animation, seconds));
    }

    private Matrix4d boneTransform(String name, String animation, double seconds) {
        Bone bone = this.bones.get(name);
        Vec3 position = this.sample(animation, name, "position", seconds, Vec3.ZERO);
        Vec3 rotation = bone.rotation().add(this.sample(animation, name, "rotation", seconds, Vec3.ZERO));
        Vec3 scale = this.sample(animation, name, "scale", seconds, new Vec3(1, 1, 1));
        Vec3 pivot = bone.pivot();
        return new Matrix4d().translate(-position.x, position.y, position.z)
            .translate(pivot.x, pivot.y, pivot.z).rotateZ(Math.toRadians(rotation.z))
            .rotateY(Math.toRadians(-rotation.y)).rotateX(Math.toRadians(-rotation.x))
            .scale(scale.x, scale.y, scale.z).translate(-pivot.x, -pivot.y, -pivot.z);
    }

    private Vec3 sample(String animation, String bone, String channel, double seconds, Vec3 fallback) {
        if (animation == null) {
            return fallback;
        }
        Map<String, Track> tracks = this.animations.computeIfAbsent(animation, key -> new HashMap<>());
        Track track = tracks.computeIfAbsent(bone + "/" + channel, key -> {
            JsonObject source = this.animationData.getAsJsonObject(animation).getAsJsonObject("bones");
            JsonElement values = source.has(bone) ? source.getAsJsonObject(bone).get(channel) : null;
            NavigableMap<Double, Vec3> frames = new TreeMap<>();
            if (values == null || values.isJsonArray()) {
                frames.put(0.0, vector(values, fallback));
            } else {
                for (Map.Entry<String, JsonElement> frame : values.getAsJsonObject().entrySet()) {
                    frames.put(Double.parseDouble(frame.getKey()), vector(frame.getValue(), fallback));
                }
            }
            return new Track(frames);
        });
        Map.Entry<Double, Vec3> before = track.frames().floorEntry(seconds);
        Map.Entry<Double, Vec3> after = track.frames().ceilingEntry(seconds);
        if (before == null) {
            return track.frames().firstEntry().getValue();
        }
        if (after == null || before.getKey().equals(after.getKey())) {
            return before.getValue();
        }
        return before.getValue().lerp(after.getValue(), (seconds - before.getKey()) / (after.getKey() - before.getKey()));
    }

    private static Vec3 vector(JsonElement value, Vec3 fallback) {
        if (value == null) {
            return fallback;
        }
        JsonArray vector = value.getAsJsonArray();
        return new Vec3(vector.get(0).getAsDouble(), vector.get(1).getAsDouble(), vector.get(2).getAsDouble());
    }

    private static JsonObject read(String resource) {
        String path = "/assets/mechafauna/" + resource;
        InputStream stream = WeaponPose.class.getResourceAsStream(path);
        if (stream == null) {
            throw new IllegalStateException("Missing weapon resource: " + path);
        }
        try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read weapon resource: " + path, exception);
        }
    }

    private record Bone(String parent, Vec3 pivot, Vec3 rotation) {
    }

    private record Track(NavigableMap<Double, Vec3> frames) {
    }
}
