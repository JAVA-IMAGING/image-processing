package org.ccode.asset.ctn.image;

import nom.tam.fits.FitsException;
import org.ccode.asset.ctn.image.extensions.Fits;
import org.ccode.asset.ctn.image.extensions.FitsDocument;
import org.ccode.asset.ctn.image.util.Constants;
import org.ccode.asset.ctn.logging.Logger;
import org.ccode.asset.ctn.logging.LoggerBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MakeDarkView {
    private static final Logger _logger = LoggerBuilder.defaultLogger(MakeDarkView.class.getName());

    /**
     * @param imageArray The array of Fits images.
     * @throws java.lang.IllegalArgumentException If the image sizes do not match.
     */
    public static float[][] loadAndSum(Fits[] imageArray) {
        int imageHeight = 0, imageWidth = 0;
        float numberOfStandardDeviationsAway = 1.5f;

        float[][] deviationArray, meanArray, darkFloatArray;
        float[] pointArray;

        //float[][][] imageArrayList = new float[imageArray.length][][]();
        ArrayList<float[][]> imageArrayList = new ArrayList<>();

        for (Fits image : imageArray) {
            float[][] floatDataFromFITSFile = FitsDocument.extractFloatData(image);


            assert floatDataFromFITSFile != null;
            if ((imageWidth == 0) && (imageHeight == 0)) {

                imageWidth = floatDataFromFITSFile[0].length;
                imageHeight = floatDataFromFITSFile.length;

            } else {
                if ((imageWidth != floatDataFromFITSFile[0].length) || (imageHeight != floatDataFromFITSFile.length)) {
                    throw new IllegalArgumentException("Image sizes do not match.");
                }
            }
            imageArrayList.add(floatDataFromFITSFile);
        }

        deviationArray = new float[imageHeight][imageWidth];
        meanArray = new float[imageHeight][imageWidth];

        for (int row = 0; row < imageHeight; row++) {
            for (int col = 0; col < imageWidth; col++) {
                pointArray = new float[imageArrayList.size()];
                int i = 0;
                for (float[][] floatArray : imageArrayList) {
                    pointArray[i] = floatArray[row][col];
                    i++;
                }

                deviationArray[row][col] = calculateDeviation(pointArray);
                meanArray[row][col] = calculateMean(pointArray);
            }
        }

        darkFloatArray = new float[imageHeight][imageWidth];
        //Arrays.fill(darkFloatArray, 0.0f);

        for (int row = 0; row < imageHeight; row++) {
            for (int col = 0; col < imageWidth; col++) {
                int sumCounter = 0;
                for (float[][] floatArray : imageArrayList) {
                    if (Math.abs(floatArray[row][col] - meanArray[row][col]) <= (numberOfStandardDeviationsAway * deviationArray[row][col])) {
                        sumCounter++;
                        darkFloatArray[row][col] =((darkFloatArray[row][col] * (float) (sumCounter - 1) + floatArray[row][col]) / (float) (sumCounter));
                    }
                }
            }
        }

        return darkFloatArray;
    }

    /**
     * @param imageArray
     * @return true if dimensions match
     */
    public static boolean isSafetoCombine(Fits[] imageArray) throws IOException {
        int nAxis1 = imageArray[0].getHDU(0).getHeader().getIntValue("NAXIS1");
        int nAxis2 = imageArray[0].getHDU(0).getHeader().getIntValue("NAXIS2");
        for(Fits image : imageArray) {
            if (image.getHDU(0).getHeader().getIntValue("NAXIS1") != nAxis1 ||
                    image.getHDU(0).getHeader().getIntValue("NAXIS2") != nAxis2) {
                return false;
            }
        }
        return true;
    }


    /**
     * Calculates the mean/average of a list of floats.
     *
     * @param li The list of floats
     * @return The mean/average
     */
    public static float calculateMean(float[] li) {
        float total = 0;

        for (float item : li) {
            total += item;
        }

        return total / li.length;
    }

    /**
     * Calculates the standard deviation of a list of floats.
     *
     * @param li The list of floats
     * @return The standard deviation of the list
     */
    public static float calculateDeviation(float[] li) {
        float mean = calculateMean(li);
        float deviation = 0;

        // calculate the standard deviation
        for (float item : li) {
            deviation += (float) Math.pow(item - mean, 2);
        }

        return (float) Math.sqrt(deviation / li.length);
    }


    public List<String> listFiles(String folderPath) throws IOException {
        List<String> filePaths;

        try (Stream<Path> paths = Files.walk(Paths.get(folderPath))) {
            filePaths = paths
                    .filter(Files::isRegularFile)
                    .map(Path::toString)
                    .collect(Collectors.toList());
        }

        return filePaths;
    }

    /**
     * Boosts the values in a DarkView image array to verify image outputs
     *
     * @param test   DarkView image array to boost
     * @param amount Arbitrary scalar value boost value
     * @return The boosted DarkView array
     */
    public static float[][] boost(float[][] test, int amount ){
        for (int row = 0; row < test.length; row++) {
            for (int col = 0; col < test[0].length; col++) {
                //if (row==100 && col==100) System.out.println(test[row][col]);
                test[row][col] = test[row][col]*amount;
                //if (row==100 && col==100) System.out.println(test[row][col]);
            }
        }
        return test;
    }
    public static void main(String[] args) {
        Fits[] imageArray = null;

        // Fits.collectObjectsTest();
        try {
            // imageArray = collectDark("./test/fits");
            imageArray = Fits.collectObjects(Constants.testFitsPath, Constants.HeaderObjs.DARK_IMG);
        } catch (FitsException e) {
            _logger.logException(e);
        }
        assert imageArray != null;
        System.out.println(imageArray.length);
        try {
            // imageArray[0].dumpHeader(); //demonstrates new dump header feature for type fits
            float[][] test = loadAndSum(imageArray);
            //imageArray[0].dumpHeader();
            float[][] testBoost = new float[test.length][test[0].length];
            FitsDocument.writeFits(Constants.testOutputPath + "dark_view_out.fits", test);
            FitsDocument.createPNG(Constants.testOutputPath + "dark_view_out_test.png", test);
            FitsDocument.createPNG(Constants.testOutputPath + "dark_view_out_boost.png", boost(test, 100000000));
            //_logger.info(Arrays.deepToString(test));
        } catch (IllegalArgumentException e) {
            _logger.logException(e);
        }
    }
}