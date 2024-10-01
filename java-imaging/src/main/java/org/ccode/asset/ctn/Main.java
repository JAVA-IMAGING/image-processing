package org.ccode.asset.ctn;

import org.ccode.asset.ctn.image.util.Constants;
import org.ccode.asset.ctn.image.extensions.*;
import org.ccode.asset.ctn.image.MakeDarkView;

import java.nio.file.Paths;
import java.io.FileNotFoundException;

/**
 * Testing purposes
 *
 */
public class Main {
    public static void main( String[] args ) throws FileNotFoundException {
        /**
         * Notes about the whole thing:
         * - When processing the FITS images, the value of BITPIX determines the datatype.
         * - If BITPIX is negative, data are floats. If it is positive, data are integers.
         */
        String img1 = "java-imaging/src/test/fits/20181002_16.fits";    // data is in integer
        String img2 = "java-imaging/src/test/fits/20181002_17.fits";    // data is in integer
        String img3 = "java-imaging/src/test/fits/20181002_28.fits";    // this should be in floats automatically
        Fits test1 = new Fits(img3);

        // testing with array of FITS images
        Fits[] test2 = Fits.collectObjects(Constants.testFitsPath, Constants.HeaderObjs.BIAS_IMG);

        // testing FitsDocument.extractFloatData()
        /*
        System.out.println(test1);
        float[][] extractFloatTest = FitsDocument.extractFloatData(test1); // problem is SOME of the extracted FITS data is in short, not float
        System.out.println(Array2D.toString(extractFloatTest));
         */
        
        // testing MakeDarkView.loadAndSum()
        float[][] result = MakeDarkView.loadAndSum(test2);
        System.out.println(Array2D.toString(result));

        // testing FitsDocument.writeFits() and FitsDocument.createPNG()
        FitsDocument.writeFits(Constants.testOutputPath + "bias.fits", result);
        FitsDocument.createPNG(Constants.testOutputPath + "bias_test.png", result);     // image is pitch black, idk if that's what is expected
        FitsDocument.createPNG
            (Constants.testOutputPath + "bias_test_boosted.png", MakeDarkView.boost(result, 100000000));    // boost image so we can actually see
    }
}
