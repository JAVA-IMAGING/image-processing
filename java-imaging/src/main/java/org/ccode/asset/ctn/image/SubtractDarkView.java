package org.ccode.asset.ctn.image;

import nom.tam.fits.FitsException;
import org.ccode.asset.ctn.image.extensions.Fits;
import org.ccode.asset.ctn.image.extensions.FitsDocument;
import org.ccode.asset.ctn.logging.Logger;
import org.ccode.asset.ctn.logging.LoggerBuilder;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

public class SubtractDarkView {
    private static final Logger _logger = LoggerBuilder.defaultLogger(MakeDarkView.class.getName());

    public static float[][] loadAndSubtract(Fits image, Fits darkImage) {
        int imageHeight = 0, imageWidth = 0;
        float numberOfStandardDeviationsAway = 1.5f;
        float[] pointArray;


        ArrayList<float[][]> imageArrayList = new ArrayList<>();

        //iterate through fits images

        float[][] floatDataFromFITSFile = FitsDocument.extractFloatData(image);
        float[][] floatDataFromFITSFileDark = FitsDocument.extractFloatData(darkImage);
        //set the hight of the images
        assert floatDataFromFITSFile != null;
        imageWidth = floatDataFromFITSFile[0].length;
        imageHeight = floatDataFromFITSFile.length;

        //check all images are of same shape
        if (imageWidth != Objects.requireNonNull(floatDataFromFITSFileDark)[0].length || imageHeight != floatDataFromFITSFileDark.length) {
            throw new IllegalArgumentException("Image sizes do not match.");
        }
        //add the data to imageArrayList
        imageArrayList.add(floatDataFromFITSFile);


        float[][] rtnArray = new float[imageHeight][imageWidth];

        for (int row = 0; row < imageHeight; row++) {
            for (int col = 0; col < imageWidth; col++) {
                rtnArray[row][col] = floatDataFromFITSFile[row][col] - floatDataFromFITSFileDark[row][col];
            }
        }
        return rtnArray;
        //meanArray = new float[imageHeight][imageWidth];

    }

    public static void main(String[] args) {
        //Get fits
        Fits fits = null;
        Fits dark = null;
        try {
            dark = new Fits("./test/output/makedarkview.png");
            fits = new Fits("./test/input/IMG_0001.fits");
        } catch (FitsException | FileNotFoundException e) {
            _logger.logException(e);
        }

        //Fits[] imageArray = new Fits[]{fits};

        try {
            float[][] test = loadAndSubtract(fits, dark);
            FitsDocument.createPNG("./test/output/subtractdarkview.png", test);
            _logger.info(Arrays.deepToString(test));
        } catch (IllegalArgumentException e) {
            _logger.logException(e);
        }
    }

}


