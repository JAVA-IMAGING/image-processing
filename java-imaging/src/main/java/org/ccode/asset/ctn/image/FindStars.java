package org.ccode.asset.ctn.image;

import nom.tam.fits.FitsException;
import org.ccode.asset.ctn.image.extensions.Array1D;
import org.ccode.asset.ctn.image.extensions.Array2D;
import org.ccode.asset.ctn.image.extensions.Fits;
import org.ccode.asset.ctn.image.extensions.FitsDocument;
import org.ccode.asset.ctn.logging.Logger;
import org.ccode.asset.ctn.logging.LoggerBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class FindStars {
    private static final Logger _logger = LoggerBuilder.defaultLogger(FindStars.class.getName());

    /**
     * Function finds stars by finding high data values within the chosen .fits file
     *
     * @param fits:              Data from the chosen .fits file
     * @param relativePeakLimit: Finds the highest floating integer datapoint in the chunk of the image.
     * @return
     */
    public static List<XYCoord> findTheStars(Fits fits, float relativePeakLimit) throws IOException, FitsException {
        final int width, height;
        width = fits.getAxes()[0];
        height = fits.getAxes()[1];

        Double max = fits.getMax();
        double maxIntensity = max == null || max == Double.MIN_VALUE ? 0d : max;
        _logger.info(String.format("Max intensity found: %f", maxIntensity));

        float[][] floatDataFromFITSFile = FitsDocument.extractFloatData(fits);
        float[] imageData = Array2D.flatten(floatDataFromFITSFile);

        if (maxIntensity == 0d) {
            throw new ArithmeticException("Cannot divide by a maxIntensity of 0.");
        }
        Array2D.multiply(floatDataFromFITSFile, (float) (1f / maxIntensity));

        int chunksize;
        if (((width / 100) % 2) != 0) {
            chunksize = width / 100;
        } else {
            chunksize = (width / 100) + 1;
        }
        if (chunksize < 11) {
            chunksize = 11;
        }

        _logger.info(String.format("chunksize = %d", chunksize));

        // halfChunkSizeRoundedDown
        int halfChunkSize = (int) Math.floor(((double) chunksize) / 2.0);

        int centerX, centerY;
        List<Float> testArray;
        List<XYCoord> centroidCoords = new ArrayList<>();
        XYCoord returnedCoordinates;

        _logger.info("Running preliminary star finder loops.");
        for (centerY = halfChunkSize; centerY < height - halfChunkSize; centerY += chunksize) {
            for (centerX = halfChunkSize; centerX < width - halfChunkSize; centerX += chunksize) {
                testArray = new ArrayList<>();

                for (int y = centerY - halfChunkSize; y < centerY + halfChunkSize; y++) {
                    for (int x = centerX - halfChunkSize; x < centerX + halfChunkSize; x++) {
                        testArray.add(imageData[x + y * width]);
                    }
                }

                returnedCoordinates = findMaximumValueInChunk(testArray.toArray(new Float[0]), centerX, centerY, chunksize, relativePeakLimit);
                if (returnedCoordinates.y > 0.0) {
                    centroidCoords.add(returnedCoordinates);
                }
            }
        }

        _logger.info(String.format("Found %d places to check a Gaussian fit.", centroidCoords.size()));

        return centroidCoords;
    }

    /**
     * Function utilized to find the highest data point within a chunk of the image.
     *
     * @param imageData         referring to the data of the image itself, used to pull the raw data of the file, like looking for the maximum values in each of the chunks.
     * @param centerX           Rounded estimate to the nearest integer of the central x-coordinate of the image.
     * @param centerY           Rounded estimate to the nearest integer of the central y-coordinate of the image.
     * @param chunkSize         Determined by the pixel width of the image, width/100 pixels
     * @param relativePeakLimit Finds the highest floating integer datapoint in the chunk of the image.
     * @return
     */
    private static XYCoord findMaximumValueInChunk(Float[] imageData, int centerX, int centerY, int chunkSize, float relativePeakLimit) {
        final Optional<Float> optionalMax = Arrays.stream(imageData).max(Float::compare);
        if (optionalMax.isEmpty()) {
            throw new NullPointerException("No maximum value could be found in the chunk.");
        }
        final float max = optionalMax.get();

        if ((max <= relativePeakLimit) || max >= 0.95) {
            return new XYCoord(-1.0, -1.0);
        }

        float standardDev = (float) Array1D.standardDeviation(imageData);

        if (standardDev < relativePeakLimit * 0.1) {
            return new XYCoord(-1.0, -1.0);
        }

        int halfChunkSize = (int) Math.floor((double) chunkSize / 2.0d);

        int x = 0, y = 0;

        for (int deltaY = -halfChunkSize; deltaY < halfChunkSize; deltaY++) {
            for (int deltaX = -halfChunkSize; deltaX < halfChunkSize; deltaX++) {
                float valueChecking = imageData[(deltaX + halfChunkSize) + (deltaY + halfChunkSize) * chunkSize];

                if (Math.abs(valueChecking - max) < (Math.ulp(max) * 5)) {
                    x = centerX + deltaX;
                    y = centerY + deltaY;
                    break;
                }
            }
        }


        return new XYCoord(x, y);
    }

    /**
     * FUNCTION DOES NOT EXIST. FUTURE LABORERS, IMPLEMENT THIS FEATURE -DB
     *
     * @param image             raw data from the chosen .fits file.
     * @param centerX           Rounded integer of the central x-coordinate.
     * @param centerY           Rounded integer of the central y-coordinate.
     * @param width             width of the .fits file, in pixels.
     * @param height            height of the .fits file, in pixels.
     * @param chunkSize         slice of the .fits file determined by the width of the image
     * @param relativePeakLimit highest data value within each chunk.
     * @return
     */
    private static boolean shouldAttemptGaussianFitAtThisPoint(Float[] image, int centerX, int centerY, int width, int height, int chunkSize, double relativePeakLimit) {
        int halfChunkSize = (int) Math.floor(((double) chunkSize) / 2.0d);

        float chunkAverage = 0.0f;
        float valueChecking, valueCenter = image[centerX + centerY * width];

        boolean isBiggest = true, isPeak = false;

        //Checks if center pixel is the highest in the chunk. Also calculates average.
        for (int checkingY = centerY - halfChunkSize; checkingY < centerY + halfChunkSize; checkingY++) {
            for (int checkingX = centerX - halfChunkSize; checkingX < centerX + halfChunkSize; checkingX++) {
                valueChecking = image[checkingX + checkingY * width];

                if (checkingY != centerY || checkingX != centerX) { // Don't check against itself
                    if (valueChecking >= valueCenter) {
                        isBiggest = false;
                    }
                }
            }
        }

        return isBiggest;
    }

    public static class XYCoord {
        public final double x, y;

        public XYCoord(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }
}
