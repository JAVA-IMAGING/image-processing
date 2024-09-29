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
        String bias = "java-imaging/src/test/fits/20181002_16.fits";
        String flat = "java-imaging/src/test/fits/20181002_16.fits";
        Fits test1 = new Fits(flat);

       //  Fits[] test2 = Fits.collectObjects(Constants.testFitsPath, Constants.HeaderObjs.BIAS_IMG);

        System.out.println(test1);

        float[][] extractFloatTest = FitsDocument.extractFloatData(test1);

        System.out.println(extractFloatTest);
    }
}
