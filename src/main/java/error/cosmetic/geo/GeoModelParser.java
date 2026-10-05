package error.cosmetic.geo;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class GeoModelParser {

    public static GeoModel parse(InputStream stream) {
        if (stream == null) return null;
        try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            return parseModel(json);
        } catch (Exception e) {
            return null;
        }
    }

    public static GeoModel parse(String jsonString) {
        if (jsonString == null || jsonString.isEmpty()) return null;
        try {
            JsonObject json = JsonParser.parseString(jsonString).getAsJsonObject();
            return parseModel(json);
        } catch (Exception e) {
            return null;
        }
    }

    private static GeoModel parseModel(JsonObject root) {
        JsonArray geoms = root.getAsJsonArray("minecraft:geometry");
        if (geoms == null || geoms.isEmpty()) {
            return null;
        }

        JsonObject geom = geoms.get(0).getAsJsonObject();
        JsonObject desc = geom.getAsJsonObject("description");
        int texW = (desc != null && desc.has("texture_width")) ? desc.get("texture_width").getAsInt() : 64;
        int texH = (desc != null && desc.has("texture_height")) ? desc.get("texture_height").getAsInt() : 64;

        GeoModel model = new GeoModel();
        model.textureWidth = texW;
        model.textureHeight = texH;

        JsonArray bones = geom.getAsJsonArray("bones");
        if (bones != null) {
            Map<String, GeoBone> boneMap = new HashMap<>();

            for (JsonElement elem : bones) {
                JsonObject boneJson = elem.getAsJsonObject();
                GeoBone bone = parseBone(boneJson, texW, texH);
                boneMap.put(bone.name, bone);
            }

            for (JsonElement elem : bones) {
                JsonObject boneJson = elem.getAsJsonObject();
                String name = boneJson.get("name").getAsString();
                GeoBone bone = boneMap.get(name);
                if (boneJson.has("parent")) {
                    String parentName = boneJson.get("parent").getAsString();
                    GeoBone parent = boneMap.get(parentName);
                    if (parent != null) {
                        parent.childBones.add(bone);
                        bone.parent = parent;
                    } else {
                        model.topLevelBones.add(bone);
                    }
                } else {
                    model.topLevelBones.add(bone);
                }
            }
        }

        return model;
    }

    private static GeoBone parseBone(JsonObject json, int texW, int texH) {
        String name = json.get("name").getAsString();
        GeoBone bone = new GeoBone(name);

        if (json.has("pivot")) {
            JsonArray p = json.getAsJsonArray("pivot");
            bone.rotationPointX = -p.get(0).getAsFloat();
            bone.rotationPointY = p.get(1).getAsFloat();
            bone.rotationPointZ = p.get(2).getAsFloat();
        }

        if (json.has("rotation")) {
            JsonArray r = json.getAsJsonArray("rotation");
            bone.setRotationX((float) Math.toRadians(-r.get(0).getAsFloat()));
            bone.setRotationY((float) Math.toRadians(-r.get(1).getAsFloat()));
            bone.setRotationZ((float) Math.toRadians(r.get(2).getAsFloat()));
        }

        if (json.has("cubes")) {
            for (JsonElement elem : json.getAsJsonArray("cubes")) {
                JsonObject cubeJson = elem.getAsJsonObject();
                GeoCube cube = parseCube(cubeJson, texW, texH);
                bone.childCubes.add(cube);
            }
        }

        return bone;
    }

    private static GeoCube parseCube(JsonObject json, int texW, int texH) {
        float[] origin = parseFloatArray(json, "origin", new float[]{0.0F, 0.0F, 0.0F});
        float[] size = parseFloatArray(json, "size", new float[]{1.0F, 1.0F, 1.0F});
        float[] pivot = parseFloatArray(json, "pivot", origin.clone());
        float[] rot = parseFloatArray(json, "rotation", new float[]{0.0F, 0.0F, 0.0F});
        float inflate = json.has("inflate") ? json.get("inflate").getAsFloat() : 0.0F;
        boolean mirror = json.has("mirror") && json.get("mirror").getAsBoolean();

        GeoCube cube = new GeoCube(size[0], size[1], size[2]);
        cube.pivot = new Vec3F(-pivot[0], pivot[1], pivot[2]);
        cube.rotation = new Vec3F(
                (float) Math.toRadians(-rot[0]),
                (float) Math.toRadians(-rot[1]),
                (float) Math.toRadians(rot[2])
        );
        cube.inflate = inflate;
        cube.mirror = mirror;

        buildCubeQuads(cube, origin, size, inflate, mirror, json, texW, texH);
        return cube;
    }

    private static void buildCubeQuads(GeoCube cube, float[] origin, float[] size, float inflate,
                                       boolean mirror, JsonObject json, int texW, int texH) {
        float ox = origin[0] - inflate;
        float oy = origin[1] - inflate;
        float oz = origin[2] - inflate;
        float sx = size[0] + inflate * 2.0F;
        float sy = size[1] + inflate * 2.0F;
        float sz = size[2] + inflate * 2.0F;

        float u = 0.0F;
        float v = 0.0F;
        boolean perFace = false;
        JsonObject facesObj = null;

        if (json.has("uv")) {
            JsonElement uvElem = json.get("uv");
            if (uvElem.isJsonArray()) {
                JsonArray arr = uvElem.getAsJsonArray();
                u = arr.get(0).getAsFloat();
                v = arr.get(1).getAsFloat();
            } else if (uvElem.isJsonObject()) {
                perFace = true;
                facesObj = uvElem.getAsJsonObject();
            }
        }

        float minX = -(ox + sx) / 16.0F;
        float minY = oy / 16.0F;
        float minZ = oz / 16.0F;
        float maxX = -ox / 16.0F;
        float maxY = (oy + sy) / 16.0F;
        float maxZ = (oz + sz) / 16.0F;

        if (perFace && facesObj != null) {
            cube.quads[0] = buildQuadPerFace(facesObj, "west", minX, minY, minZ, minX, maxY, maxZ, -1.0F, 0.0F, 0.0F, texW, texH);
            cube.quads[1] = buildQuadPerFace(facesObj, "east", maxX, minY, minZ, maxX, maxY, maxZ, 1.0F, 0.0F, 0.0F, texW, texH);
            cube.quads[2] = buildQuadPerFace(facesObj, "down", minX, minY, minZ, maxX, minY, maxZ, 0.0F, -1.0F, 0.0F, texW, texH);
            cube.quads[3] = buildQuadPerFace(facesObj, "up", minX, maxY, minZ, maxX, maxY, maxZ, 0.0F, 1.0F, 0.0F, texW, texH);
            cube.quads[4] = buildQuadPerFace(facesObj, "north", minX, minY, minZ, maxX, maxY, minZ, 0.0F, 0.0F, -1.0F, texW, texH);
            cube.quads[5] = buildQuadPerFace(facesObj, "south", minX, minY, maxZ, maxX, maxY, maxZ, 0.0F, 0.0F, 1.0F, texW, texH);
        } else {
            cube.quads[0] = buildQuadBox(minX, minY, minZ, minX, maxY, maxZ, -1.0F, 0.0F, 0.0F, u, v, sz, sy, sx, texW, texH, "west");
            cube.quads[1] = buildQuadBox(maxX, minY, minZ, maxX, maxY, maxZ, 1.0F, 0.0F, 0.0F, u, v, sz, sy, sx, texW, texH, "east");
            cube.quads[2] = buildQuadBox(minX, minY, minZ, maxX, minY, maxZ, 0.0F, -1.0F, 0.0F, u, v, sz, sy, sx, texW, texH, "down");
            cube.quads[3] = buildQuadBox(minX, maxY, minZ, maxX, maxY, maxZ, 0.0F, 1.0F, 0.0F, u, v, sz, sy, sx, texW, texH, "up");
            cube.quads[4] = buildQuadBox(minX, minY, minZ, maxX, maxY, minZ, 0.0F, 0.0F, -1.0F, u, v, sz, sy, sx, texW, texH, "north");
            cube.quads[5] = buildQuadBox(minX, minY, maxZ, maxX, maxY, maxZ, 0.0F, 0.0F, 1.0F, u, v, sz, sy, sx, texW, texH, "south");
        }
    }

    private static GeoQuad buildQuadPerFace(JsonObject obj, String faceName,
                                            float x1, float y1, float z1, float x2, float y2, float z2,
                                            float nx, float ny, float nz, int texW, int texH) {
        if (!obj.has(faceName)) return null;
        JsonObject face = obj.getAsJsonObject(faceName);
        if (!face.has("uv") || !face.has("uv_size")) return null;

        JsonArray uvArr = face.getAsJsonArray("uv");
        JsonArray sizeArr = face.getAsJsonArray("uv_size");
        float u = uvArr.get(0).getAsFloat() / texW;
        float v = uvArr.get(1).getAsFloat() / texH;
        float uw = sizeArr.get(0).getAsFloat() / texW;
        float vh = sizeArr.get(1).getAsFloat() / texH;

        GeoVertex[] verts = buildFaceVertices(faceName, x1, y1, z1, x2, y2, z2, u, v, uw, vh);
        return new GeoQuad(verts, nx, ny, nz);
    }

    private static GeoQuad buildQuadBox(float x1, float y1, float z1, float x2, float y2, float z2,
                                        float nx, float ny, float nz,
                                        float u, float v, float sz, float sy, float sx,
                                        float texW, float texH, String faceName) {
        float fu, fv, fuw, fvh;
        switch (faceName) {
            case "north" -> {
                fu = (u + sz + sx) / texW;
                fv = (v + sz) / texH;
                fuw = sx / texW;
                fvh = sy / texH;
            }
            case "south" -> {
                fu = (u + sz + sx + sz) / texW;
                fv = (v + sz) / texH;
                fuw = sx / texW;
                fvh = sy / texH;
            }
            case "east" -> {
                fu = u / texW;
                fv = (v + sz) / texH;
                fuw = sz / texW;
                fvh = sy / texH;
            }
            case "west" -> {
                fu = (u + sz + sx) / texW;
                fv = (v + sz) / texH;
                fuw = sz / texW;
                fvh = sy / texH;
            }
            case "up" -> {
                fu = (u + sz) / texW;
                fv = v / texH;
                fuw = sx / texW;
                fvh = sz / texH;
            }
            case "down" -> {
                fu = (u + sz + sx) / texW;
                fv = v / texH;
                fuw = sx / texW;
                fvh = sz / texH;
            }
            default -> {
                fu = 0.0F; fv = 0.0F; fuw = 0.0F; fvh = 0.0F;
            }
        }
        GeoVertex[] verts = buildFaceVertices(faceName, x1, y1, z1, x2, y2, z2, fu, fv, fuw, fvh);
        return new GeoQuad(verts, nx, ny, nz);
    }

    private static GeoVertex[] buildFaceVertices(String faceName,
                                                 float x1, float y1, float z1, float x2, float y2, float z2,
                                                 float u, float v, float uw, float vh) {
        GeoVertex[] verts = new GeoVertex[4];
        float u2 = u + uw;
        float v2 = v + vh;
        switch (faceName) {
            case "north" -> {
                verts[0] = new GeoVertex(x2, y2, z1, u, v);
                verts[1] = new GeoVertex(x1, y2, z1, u2, v);
                verts[2] = new GeoVertex(x1, y1, z1, u2, v2);
                verts[3] = new GeoVertex(x2, y1, z1, u, v2);
            }
            case "south" -> {
                verts[0] = new GeoVertex(x1, y2, z2, u, v);
                verts[1] = new GeoVertex(x2, y2, z2, u2, v);
                verts[2] = new GeoVertex(x2, y1, z2, u2, v2);
                verts[3] = new GeoVertex(x1, y1, z2, u, v2);
            }
            case "east" -> {
                verts[0] = new GeoVertex(x2, y2, z2, u, v);
                verts[1] = new GeoVertex(x2, y2, z1, u2, v);
                verts[2] = new GeoVertex(x2, y1, z1, u2, v2);
                verts[3] = new GeoVertex(x2, y1, z2, u, v2);
            }
            case "west" -> {
                verts[0] = new GeoVertex(x1, y2, z1, u, v);
                verts[1] = new GeoVertex(x1, y2, z2, u2, v);
                verts[2] = new GeoVertex(x1, y1, z2, u2, v2);
                verts[3] = new GeoVertex(x1, y1, z1, u, v2);
            }
            case "up" -> {
                verts[0] = new GeoVertex(x1, y2, z1, u, v);
                verts[1] = new GeoVertex(x1, y2, z2, u, v2);
                verts[2] = new GeoVertex(x2, y2, z2, u2, v2);
                verts[3] = new GeoVertex(x2, y2, z1, u2, v);
            }
            case "down" -> {
                verts[0] = new GeoVertex(x2, y1, z1, u, v);
                verts[1] = new GeoVertex(x2, y1, z2, u, v2);
                verts[2] = new GeoVertex(x1, y1, z2, u2, v2);
                verts[3] = new GeoVertex(x1, y1, z1, u2, v);
            }
        }
        return verts;
    }

    private static float[] parseFloatArray(JsonObject obj, String key, float[] def) {
        if (!obj.has(key)) return def;
        JsonArray arr = obj.getAsJsonArray(key);
        float[] res = new float[arr.size()];
        for (int i = 0; i < arr.size(); i++) {
            res[i] = arr.get(i).getAsFloat();
        }
        return res;
    }
}
