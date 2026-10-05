package error.cosmetic.geo;

import java.util.ArrayList;
import java.util.List;

public class GeoBone {
    public GeoBone parent;
    public List<GeoBone> childBones = new ArrayList<>();
    public List<GeoCube> childCubes = new ArrayList<>();
    public String name;
    public boolean isHidden = false;

    public float rotationPointX;
    public float rotationPointY;
    public float rotationPointZ;

    private float rotateX;
    private float rotateY;
    private float rotateZ;

    private float positionX;
    private float positionY;
    private float positionZ;

    private float scaleX = 1.0F;
    private float scaleY = 1.0F;
    private float scaleZ = 1.0F;

    public GeoBone(String name) {
        this.name = name;
    }

    public float getRotationX() {
        return this.rotateX;
    }

    public float getRotationY() {
        return this.rotateY;
    }

    public float getRotationZ() {
        return this.rotateZ;
    }

    public void setRotationX(float rotX) {
        this.rotateX = rotX;
    }

    public void setRotationY(float rotY) {
        this.rotateY = rotY;
    }

    public void setRotationZ(float rotZ) {
        this.rotateZ = rotZ;
    }

    public float getPositionX() {
        return this.positionX;
    }

    public float getPositionY() {
        return this.positionY;
    }

    public float getPositionZ() {
        return this.positionZ;
    }

    public void setPositionX(float px) {
        this.positionX = px;
    }

    public void setPositionY(float py) {
        this.positionY = py;
    }

    public void setPositionZ(float pz) {
        this.positionZ = pz;
    }

    public float getScaleX() {
        return this.scaleX;
    }

    public float getScaleY() {
        return this.scaleY;
    }

    public float getScaleZ() {
        return this.scaleZ;
    }

    public void setScaleX(float sx) {
        this.scaleX = sx;
    }

    public void setScaleY(float sy) {
        this.scaleY = sy;
    }

    public void setScaleZ(float sz) {
        this.scaleZ = sz;
    }

    public float getPivotX() {
        return this.rotationPointX;
    }

    public float getPivotY() {
        return this.rotationPointY;
    }

    public float getPivotZ() {
        return this.rotationPointZ;
    }

    public void setPivotX(float px) {
        this.rotationPointX = px;
    }

    public void setPivotY(float py) {
        this.rotationPointY = py;
    }

    public void setPivotZ(float pz) {
        this.rotationPointZ = pz;
    }

    public String getName() {
        return this.name;
    }
}
