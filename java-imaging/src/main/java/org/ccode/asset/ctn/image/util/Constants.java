package org.ccode.asset.ctn.image.util;

import java.util.Arrays;

/**
 * Class for storing static vars/file paths
 */
public class Constants {

    /*
     * Stores values for OBJECT variable in FITS headers
     * Used to filter for specific FITS image types
     */
    public static class HeaderObjs {
        public static final String DARK_IMG = "dark";
        public static final String FLAT_IMG = "flat";
        public static final String BIAS_IMG = "bias";
        public static final String SCIENCE_IMG = "science";
    }

    /**
     * @return if OBJECT in header is NOT "dark", "flat", or "bias"
     */
    public static boolean isSciImg(String input) {
        String[] arr = new String[]{HeaderObjs.DARK_IMG, HeaderObjs.FLAT_IMG, HeaderObjs.BIAS_IMG};
        return Arrays.toString(arr).contains(input);
    }

    /*
     *  File path constants
     */
    public static String testFitsPath = "java-imaging/src/test/fits/";
    public static String testInputPath = "java-imaging/src/test/input/";
    public static String testOutputPath = "java-imaging/src/test/output/";
}
