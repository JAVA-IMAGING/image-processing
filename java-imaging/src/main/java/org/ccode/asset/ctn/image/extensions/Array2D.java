package org.ccode.asset.ctn.image.extensions;

import java.util.Arrays;

public class Array2D {

    /**
     * Converts a 2D Array int a 1D array.
     *
     * @param arr The array to be flattened.
     * @return The flattened array
     */
    public static float[] flatten(float[][] arr) {
        //Get width and height of 2D array
        int width = arr[0].length;
        int height = arr.length;

        //Create 1D array
        float[] flattened = new float[width * height];

        //Populate new array
        for (int row = 0; row < height; row++) {
            System.arraycopy(arr[row], 0, flattened, row * width, width);
        }

        //Return result
        return flattened;
    }

    /**
     * Cast a 2D float array to a 2D double array
     *
     * @param arr The float array to be converted
     * @return The new 2D double array
     */
    public static double[][] castToDouble(float[][] arr) {
        //Get width and height of 2D array
        int width = arr[0].length;
        int height = arr.length;

        //Create and cast the array
        double[][] castedArr = new double[height][width];

        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                castedArr[row][col] = arr[row][col]; // Explicit casting is not required
            }
        }

        return castedArr;        //Return result
    }

    /**
     * Cast a 1D float array to a 1D double array
     *
     * @param arr The float array to be converted
     * @return The new 1D double array
     */
    public static double[] castToDouble(float[] arr) {
        //Create and cast new array
        double[] castedArr = new double[arr.length];

        for (int i = 0; i < arr.length; i++) {
            castedArr[i] = arr[i];        // Explicit case is not required.
        }

        //Return result
        return castedArr;
    }

    /**
     * Adds the given number to each element in the 2D array.
     *
     * @param arr The array whose elements should be increased.
     * @param num The value that should be added to each element
     */
    public static void add(float[][] arr, float num) {
        //Get width and height of 2D array
        int width = arr[0].length;
        int height = arr.length;

        //Apply addition
        for (int row = 0; row < height; row++)
            for (int col = 0; col < width; col++)
                arr[row][col] += num;
    }

    /**
     * Adds the corresponding index for each given array together and sets them to the corresponding index in a 2D
     *
     * @param arrays
     * @return
     * @throws IllegalArgumentException If the width or height does not match the array width or array height.
     */
    public static float[][] add(float[][]... arrays) {
        //Get array dimensions
        int width = arrays[0][0].length;
        int height = arrays[0].length;

        float[][] result = new float[height][width];

        for (float[][] arr : arrays) {
            //Array dimensions
            int arrWidth = arr[0].length;
            int arrHeight = arr.length;

            //Check that all arrays are the same dimensions
            if (arrWidth != width || arrHeight != height) {
                throw new IllegalArgumentException("The width or height of an array does not match the others.");
            }

            //Add all the elements to the result array
            for (int row = 0; row < height; row++)
                for (int col = 0; col < width; col++)
                    result[row][col] += arr[row][col];
        }

        return result;
    }

    /**
     * Adds the values of one 2d float array to the original array.
     *
     * @param original The array whose values should be affected by the addition.
     * @param arr      The array whose values are being added to the affected array.
     */
    public static void add(float[][] original, float[][] arr) {
        //Get array dimensions
        int width = arr[0].length;
        int height = arr.length;

        //Add all the elements to the original array
        for (int row = 0; row < height; row++)
            for (int col = 0; col < width; col++)
                original[row][col] += arr[row][col];
    }

    /**
     * Multiplies the given number to each element in the 2D array.
     *
     * @param arr The array whose elements should be multiplied.
     * @param num The value that should be multiplied to each element.
     */
    public static void multiply(float[][] arr, float num) {
        //Get width and height of 2D array
        int width = arr[0].length;
        int height = arr.length;

        //Apply multiplication
        for (int row = 0; row < height; row++)
            for (int col = 0; col < width; col++)
                arr[row][col] *= num;
    }

    /**
     * Clips values between min and max
     *
     * @param arr
     * @param min
     * @param max
     */
    public static void clip(float[][] arr, float min, float max) {
        //Get width and height of 2D array
        int width = arr[0].length;
        int height = arr.length;

        //Loop through the array and clip values
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                if (arr[row][col] > max) {
                    arr[row][col] = max;
                } else if (arr[row][col] < min) {
                    arr[row][col] = min;
                }
            }
        }
    }

    /**
     * Creates a deep copy of the 2D array.
     *
     * @param arr The array that needs to be copied.
     * @return A new array that holds the same values in a new memory location.
     */
    public static float[][] copy(float[][] arr) {
        //Get width and height of 2D array
        int width = arr[0].length;
        int height = arr.length;

        float[][] copy = new float[height][width];

        //Loop through the array and clip values
        for (int row = 0; row < height; row++)
            System.arraycopy(arr[row], 0, copy[row], 0, width);
        return copy;
    }

    /**
     * Populates a 2D float array with a specific value.
     *
     * @param arr   The 2D array that needs to be populated.
     * @param value The value that every element will be set to.
     */
    public static void setValue(float[][] arr, float value) {
        //Loop through the array and set values
        for (float[] _float : arr) {
            Arrays.fill(_float, value);
        }
    }

}
