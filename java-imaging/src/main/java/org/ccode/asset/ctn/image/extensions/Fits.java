package org.ccode.asset.ctn.image.extensions;

import nom.tam.fits.BasicHDU;
import nom.tam.fits.Data;
import nom.tam.fits.FitsException;
import org.ccode.asset.ctn.image.util.Constants;
import org.ccode.asset.ctn.logging.Logger;
import org.ccode.asset.ctn.logging.LoggerBuilder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.ccode.asset.ctn.image.MakeDarkView.loadAndSum;


public class Fits extends nom.tam.fits.Fits {
    private static final Logger _logger = LoggerBuilder.defaultLogger(Fits.class.getName());
    private String filePath;
    private double max;


    /**
     * @param filePath to look for FITS files - "./test/fits" - used for now
     * @throws FitsException
     */
    public Fits(String filePath) throws FitsException, FileNotFoundException {
        super(filePath);

        File file = new File(filePath);
        if (!file.exists()) {
            throw new FileNotFoundException(String.format("File: `%s` does not exist.", filePath));
        }

        this.filePath = filePath;
    }

    public BasicHDU<?>[] read() {
        return super.read();
    }

    public void dumpHeader() {
        BasicHDU<?>[] hdu = this.read();
        //Header header  = hdu[0].getHeader();
        for (BasicHDU<?> h : hdu) {
            h.getHeader().dumpHeader(System.out);
        }
    }

    public Fits() throws FitsException {
        super();
    }

    public String checkHeaderVar(String var) {
        BasicHDU<?>[] hdu = this.read();
        for (BasicHDU<?> h : hdu) {
            return h.getHeader().getStringValue(var);
        }
        return "";
    }

    public Boolean isObjectType(String target) {
        BasicHDU<?>[] hdu = this.read();
        //Header header  = hdu[0].getHeader();
        for (BasicHDU<?> h : hdu) {
            String object = h.getHeader().getStringValue("OBJECT");
            //System.out.println(object);
            if (Objects.equals(object, target)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param path File path to look for FITS files - "./test/fits" - used for now
     * @param type FitsObjectType for shared identifier for dark, flat, or bias
     * @return an array of images matching the specified FitsObjectType
     */

    public static Fits[] collectObjects(String path, String type) {

        List<String> filePaths;
        Fits[] imageArray = new Fits[0];
        try {
            //set path to fits files. I used python test images.
            Stream<Path> paths = Files.walk(Paths.get(path));
            filePaths = paths
                    .filter(Files::isRegularFile)
                    .map(Path::toString)
                    .collect(Collectors.toList());
            imageArray = new Fits[filePaths.size()];
            int i = 0;

            //iterate through all file paths in directory
            for (String fp : filePaths) {
                Fits fitsFile = new Fits(fp);

                // set header string value. Test images work with "object" but apparently there can be alternatives ways to store this information
                if (fitsFile.isObjectType(type)) {
                    imageArray[i] = fitsFile;
                    i++;
                }
            }
            System.out.printf("Number of |%s| files found: %d\n", type, i);

            Fits[] resizedImageArray = new Fits[i];
            //imageArray[0].dumpHeader();
            System.arraycopy(imageArray, 0, resizedImageArray, 0, i);
            //resizedImageArray[0].dumpHeader();
            return resizedImageArray;

        } catch (FitsException | FileNotFoundException e) {
            _logger.logException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return imageArray;
    }

    public static Fits[] collectSciObjs(String path) {

        List<String> filePaths;
        Fits[] imageArray = new Fits[0];
        try {
            //set path to fits files. I used python test images.
            Stream<Path> paths = Files.walk(Paths.get(path));
            filePaths = paths
                    .filter(Files::isRegularFile)
                    .map(Path::toString)
                    .collect(Collectors.toList());
            imageArray = new Fits[filePaths.size()];
            int i = 0;

            //iterate through all file paths in directory
            for (String fp : filePaths) {
                Fits fitsFile = new Fits(fp);

                // set header string value. Test images work with "object" but apparently there can be alternatives ways to store this information
                if (!Constants.isSciImg(fitsFile.checkHeaderVar("OBJECT"))) {
                    imageArray[i] = fitsFile;
                    i++;
                }
            }
            System.out.printf("Number of |%s| files found: %d\n", "science", i);

            Fits[] resizedImageArray = new Fits[i];
            //imageArray[0].dumpHeader();
            System.arraycopy(imageArray, 0, resizedImageArray, 0, i);
            //resizedImageArray[0].dumpHeader();
            return resizedImageArray;

        } catch (FitsException | FileNotFoundException e) {
            _logger.logException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return imageArray;
    }

    public void setMax() throws IOException, FitsException {
        BasicHDU<Data> data;
        double max = Double.MIN_VALUE;

        for (int i = 0; i < getNumberOfHDUs(); i++) {
            data = (BasicHDU<Data>) getHDU(i);
            if (max > data.getMaximumValue()) {
                max = data.getMaximumValue();
            }
        }

        this.max = max;
    }

    public Double getMax() {
        if (max == Double.MIN_VALUE) {
            try {
                setMax();
            } catch (IOException | FitsException e) {
                return null;
            }
        }
        return max;
    }

    /**
     * @return the path the file came from
     */
    public String getFilePath() {
        return filePath;
    }

    /**
     * @return the name of the file
     */
    public String getFileName() {
        File file = new File(filePath);
        return file.getName();
    }

    /**
     * @return dimensions (width, height)
     */
    public int[] getAxes() {
        try {
            //Get axes
            int[] rawDims = super.getHDU(0).getAxes();

            //Reverse the array to get dimensions in the correct order

            return new int[]{rawDims[1], rawDims[0]};


        } catch (Exception e) {
            _logger.logException(e);
        }

        //No image exists if we get here
        return null;
    }

    /**
     * Checks that files with Constants.HeaderObjs values can be collected
     */
    public static void collectObjectsTest() {
        //Get fits
        Fits[] darkArr = null,
                flatArr = null,
                biasArr = null,
                scienceArr = null;

        try {
            // imageArray = collectDark("./test/fits");
            darkArr = collectObjects(Constants.testFitsPath, Constants.HeaderObjs.DARK_IMG);
            flatArr = collectObjects(Constants.testFitsPath, Constants.HeaderObjs.FLAT_IMG);
            biasArr = collectObjects(Constants.testFitsPath, Constants.HeaderObjs.BIAS_IMG);
            scienceArr = collectSciObjs(Constants.testFitsPath);


        } catch (FitsException e) {
            _logger.logException(e);
        }
        try {
            // imageArray[0].dumpHeader(); //demonstrates new dump header feature for type fits
            assert darkArr != null;
            float[][] darkTest = loadAndSum(darkArr);

            assert flatArr != null;
            float[][] flatTest = loadAndSum(flatArr);

            assert biasArr != null;
            float[][] biasTest = loadAndSum(biasArr);

            assert scienceArr != null;
            float[][] scienceTest = loadAndSum(scienceArr);

            FitsDocument.createPNG(Constants.testOutputPath + "dark_view_out.png", darkTest);
            FitsDocument.createPNG(Constants.testOutputPath + "flat_view_out.png", flatTest);
            FitsDocument.createPNG(Constants.testOutputPath + "bias_view_out.png", biasTest);
            FitsDocument.createPNG(Constants.testOutputPath + "science_view_out.png", scienceTest);

            //_logger.info(Arrays.deepToString(test));
        } catch (IllegalArgumentException e) {
            _logger.logException(e);
        }
    }

    public static void main(String[] args) {
        collectObjectsTest();
    }
}