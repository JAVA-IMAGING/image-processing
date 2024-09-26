package org.ccode.asset.ctn.image.extensions;

import java.util.Arrays;

public class Array1D {
    /**
     * Finds the standard deviation of an array.
     *
     * @param array The array whose standard deviation needs to be found.
     * @return The standard deviation.
     */
    public static double standardDeviation(int[] array) {
        int mean = Arrays.stream(array).reduce(0, Integer::sum) / array.length;
        double standardDevDouble = Arrays.stream(array)
                .map(element -> (int) Math.pow(element - mean, 2))
                .reduce(0, Integer::sum);
        return Math.sqrt(standardDevDouble / (array.length - 1));
    }

    /**
     * Finds the standard deviation of an array.
     *
     * @param array The array whose standard deviation needs to be found.
     * @return The standard deviation.
     */
    public static double standardDeviation(Float[] array) {
        float mean = Arrays.stream(array).reduce(0f, Float::sum) / array.length;
        double standardDevDouble = Arrays.stream(array)
                .map(element -> Math.pow(element - mean, 2))
                .reduce(0d, Double::sum);
        return Math.sqrt(standardDevDouble / (array.length - 1));
    }

    /**
     * Finds the standard deviation of an array.
     *
     * @param array The array whose standard deviation needs to be found.
     * @return The standard deviation.
     */
    public static double standardDeviation(double[] array) {
        double mean = Arrays.stream(array).reduce(0d, Double::sum) / array.length;
        double standardDevDouble = Arrays.stream(array)
                .map(element -> Math.pow(element - mean, 2))
                .reduce(0d, Double::sum);
        return Math.sqrt(standardDevDouble / (array.length - 1));
    }

    /**
     * Finds the standard deviation of an array.
     *
     * @param array The array whose standard deviation needs to be found.
     * @return The standard deviation.
     */
    public static double standardDeviation(Double[] array) {
        double mean = Arrays.stream(array).reduce(0d, Double::sum) / array.length;
        double standardDevDouble = Arrays.stream(array)
                .map(element -> Math.pow(element - mean, 2))
                .reduce(0d, Double::sum);
        return Math.sqrt(standardDevDouble / (array.length - 1));
    }
}
