package com.mithrilmania.blocktopograph.util.math;

public class IntegerPos {
    /**
     * the x-component of this vector
     **/
    public int x;
    /**
     * the y-component of this vector
     **/
    public int y;
    /**
     * the z-component of this vector
     **/
    public int z;

    /**
     * Creates a vector with the given components
     *
     * @param x The x-component
     * @param y The y-component
     * @param z The z-component
     */
    public IntegerPos(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public int hashCode() {
        return hash(this.x, this.y, this.z);
    }

    /**
     * Warning: this is quite a slow hash function.
     * This hash function hashes three integer in such a way that the bits are interleaved,
     * vectors that are similar but not equal will have a much higher chance to be unique
     * compared to the classic way: (x*31 + y)*31 + z
     *
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     * @return interleaved bits of X, Y and Z, usable as hash.
     */
    public static int hash(int x, int y, int z) {
        int hash = 0;
        for (int i = 0, xi = 0, yi = 1, zi = 2; i < 32; i++, xi += 3, yi += 3, zi += 3) {
            hash ^= ((x >> i) & 1) << xi;
            hash ^= ((y >> i) & 1) << yi;
            hash ^= ((z >> i) & 1) << zi;
            xi %= 32;
            yi %= 32;
            zi %= 32;
        }
        return hash;
    }
}
