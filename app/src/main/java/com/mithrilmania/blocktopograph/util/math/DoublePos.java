package com.mithrilmania.blocktopograph.util.math;

public class DoublePos {
    /**
     * the x-component of this vector
     **/
    public double x;
    /**
     * the y-component of this vector
     **/
    public double y;
    /**
     * the z-component of this vector
     **/
    public double z;

    /**
     * Creates a vector with the given components
     *
     * @param x The x-component
     * @param y The y-component
     * @param z The z-component
     */
    public DoublePos(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
}
