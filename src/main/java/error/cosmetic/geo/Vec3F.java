package error.cosmetic.geo;

public class Vec3F {
    public static final Vec3F NULL_VECTOR = new Vec3F(0.0F, 0.0F, 0.0F);
    public float x;
    public float y;
    public float z;

    public Vec3F(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public float getX() {
        return this.x;
    }

    public float getY() {
        return this.y;
    }

    public float getZ() {
        return this.z;
    }

    public void set(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vec3F crossProduct(Vec3F other) {
        return new Vec3F(
                this.y * other.z - this.z * other.y,
                this.z * other.x - this.x * other.z,
                this.x * other.y - this.y * other.x
        );
    }
}
