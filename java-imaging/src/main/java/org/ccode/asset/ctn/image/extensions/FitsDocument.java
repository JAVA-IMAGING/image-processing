package org.ccode.asset.ctn.image.extensions;

import nom.tam.fits.*;
import nom.tam.util.Cursor;
import org.ccode.asset.ctn.image.util.Constants;
import org.ccode.asset.ctn.logging.Logger;
import org.ccode.asset.ctn.logging.LoggerBuilder;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

public class FitsDocument {
    private static final Logger _logger = LoggerBuilder.defaultLogger(FitsDocument.class.getName());

    /**
     * Return raw image float data
     *
     * @param image
     * @return
     */
    public static float[][] extractFloatData(Fits image) {
        BasicHDU imageHDU;
        Object data;
        float [][] rawData = null;

        //Load in data
        try {
            imageHDU = image.getHDU(0);
            data = imageHDU.getKernel();
        } catch (Exception e) {
            _logger.logException(e);
            return null;
        } //If there is no image data

        // Check if the data is a 2D array (image)
        if (data instanceof float[][]) {
            rawData = (float[][]) data;
        }


        //Create a float array with the same dimensions
        assert rawData != null;
        int imgWidth = rawData[0].length;
        int imgHeight = rawData.length;
        float[][] adjustedData = new float[imgHeight][imgWidth];

        float min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;

        //Populate the float array and adjust for negatives
        for (int row = 0; row < imgHeight; row++) {
            for (int col = 0; col < imgWidth; col++) {
                //Add by minimum value to make all numbers positive
                float num = rawData[row][col];
                adjustedData[row][col] = num;

                //Keep track of minimum and maximum
                if (num > max) max = num;
                if (num < min) min = num;
            }
        }

        //Map each pixel to a 0-1 range
        float range = max - min;
        Array2D.add(adjustedData, -min);
        Array2D.multiply(adjustedData, 1f / range);

        //Return float data
        return adjustedData;
    }

    /**
     * Populates red, green, and blue arrays from raw fits image.
     *
     * @param floatData
     * @param bayerPat   The bayer pattern that should be used. Current valid options are "RGGB" and "BGGR".
     * @param redImage
     * @param greenImage
     * @param blueImage
     * @throws IllegalArgumentException If an invalid bayer pattern is given.
     */
    public static void extractRGB(float[][] floatData, String bayerPat, float[][] redImage, float[][] greenImage, float[][] blueImage) throws IllegalArgumentException {
        //Image dimensions
        int imageWidth = floatData[0].length;
        int imageHeight = floatData.length;

        _logger.info(String.format("rgb w: %d, h: %d", imageWidth, imageHeight));

        //Extract color depending on the Bayer pattern
        switch (bayerPat.toUpperCase()) {
            case "RGGB":
                //Loop through the image
                for (int row = 1; row < imageHeight - 1; row++) {
                    for (int col = 1; col < imageWidth - 1; col++) {
                        //To determine which pixel we're on
                        int rowRemainder = row % 2;
                        int colRemainder = col % 2;

                        //On a red pixel
                        if (rowRemainder == 0 && colRemainder == 0) {
                            //Set red pixel
                            redImage[row][col] = floatData[row][col];

                            //Average neighboring green pixels
                            greenImage[row][col] = 0.25f * (
                                    floatData[row - 1][col] +
                                            floatData[row + 1][col] +
                                            floatData[row][col - 1] +
                                            floatData[row][col + 1]);

                            //Average neighboring blue pixels
                            blueImage[row][col] = 0.25f * (
                                    floatData[row - 1][col - 1] +
                                            floatData[row - 1][col + 1] +
                                            floatData[row + 1][col - 1] +
                                            floatData[row + 1][col - 1]);
                        }
                        //On a blue pixel
                        else if (rowRemainder == 1 && colRemainder == 1) {
                            //Average neighboring red pixels
                            redImage[row][col] = 0.25f * (
                                    floatData[row - 1][col - 1] +
                                            floatData[row - 1][col + 1] +
                                            floatData[row + 1][col - 1] +
                                            floatData[row + 1][col - 1]);

                            //Average neighboring green pixels
                            greenImage[row][col] = 0.25f * (
                                    floatData[row - 1][col] +
                                            floatData[row + 1][col] +
                                            floatData[row][col - 1] +
                                            floatData[row][col + 1]);

                            //Set blue pixel
                            blueImage[row][col] = floatData[row][col];
                        }
                        //On a green pixel (case 1)
                        else if (rowRemainder == 0 && colRemainder == 1) {
                            //Average neighboring red pixels
                            redImage[row][col] = 0.5f * (
                                    floatData[row][col - 1] +
                                            floatData[row][col + 1]);

                            //Set green pixel
                            greenImage[row][col] = floatData[row][col];

                            //Average neighboring blue pixels
                            blueImage[row][col] = 0.5f * (
                                    floatData[row - 1][col] +
                                            floatData[row + 1][col]);
                        }
                        //On a green pixel (case 2)
                        else if (rowRemainder == 1 && colRemainder == 0) {
                            //Average neighboring red pixels
                            redImage[row][col] = 0.5f * (
                                    floatData[row - 1][col] +
                                            floatData[row + 1][col]);

                            //Set green pixel
                            greenImage[row][col] = floatData[row][col];

                            //Average neighboring blue pixels
                            blueImage[row][col] = 0.5f * (
                                    floatData[row][col - 1] +
                                            floatData[row][col + 1]);
                        }
                    }
                }
                break;
            case "BGGR":
                for (int row = 1; row < imageHeight - 1; row++) {
                    for (int col = 1; col < imageWidth - 1; col++) {
                        //To determine which pixel we're on
                        int rowRemainder = row % 2;
                        int colRemainder = col % 2;

                        //On a blue pixel
                        if (rowRemainder == 0 && colRemainder == 0) {
                            //Average neighboring red pixels
                            redImage[row][col] = 0.25f * (
                                    floatData[row - 1][col - 1] +
                                            floatData[row - 1][col + 1] +
                                            floatData[row + 1][col - 1] +
                                            floatData[row + 1][col - 1]);

                            //Average neighboring green pixels
                            greenImage[row][col] = 0.25f * (
                                    floatData[row - 1][col] +
                                            floatData[row + 1][col] +
                                            floatData[row][col - 1] +
                                            floatData[row][col + 1]);

                            //Set blue pixel
                            blueImage[row][col] = floatData[row][col];
                        }
                        //On a red pixel
                        else if (rowRemainder == 1 && colRemainder == 1) {
                            //Set red pixel
                            redImage[row][col] = floatData[row][col];

                            //Average neighboring green pixels
                            greenImage[row][col] = 0.25f * (
                                    floatData[row - 1][col] +
                                            floatData[row + 1][col] +
                                            floatData[row][col - 1] +
                                            floatData[row][col + 1]);

                            //Average neighboring blue pixels
                            blueImage[row][col] = 0.25f * (
                                    floatData[row - 1][col - 1] +
                                            floatData[row - 1][col + 1] +
                                            floatData[row + 1][col - 1] +
                                            floatData[row + 1][col - 1]);
                        }
                        //On a green pixel (case 1)
                        else if (rowRemainder == 0 && colRemainder == 1) {
                            //Average neighboring red pixels
                            redImage[row][col] = 0.5f * (
                                    floatData[row - 1][col] +
                                            floatData[row + 1][col]);

                            //Set green pixel
                            greenImage[row][col] = floatData[row][col];

                            //Average neighboring blue pixels
                            blueImage[row][col] = 0.5f * (
                                    floatData[row][col - 1] +
                                            floatData[row][col + 1]);
                        }
                        //On a green pixel (case 2)
                        else if (rowRemainder == 1 && colRemainder == 0) {
                            //Average neighboring red pixels
                            redImage[row][col] = 0.5f * (
                                    floatData[row][col - 1] +
                                            floatData[row][col + 1]);

                            //Set green pixel
                            greenImage[row][col] = floatData[row][col];

                            //Average neighboring blue pixels
                            blueImage[row][col] = 0.5f * (
                                    floatData[row + 1][col] +
                                            floatData[row - 1][col]);
                        }
                    }
                }
                break;
            default:
                throw new IllegalArgumentException("Invalid Bayer Pattern");
        }
    }

    public static void extractRGB(Fits fitsImage, float[][] redImage, float[][] greenImage, float[][] blueImage) {
        Header header = null;

        //Get header data
        try {
            header = fitsImage.getHDU(0).getHeader();
        } catch (Exception e) {
            _logger.logException(e);
        }

        //Get Bayer pattern
        String bayerpat = header.getStringValue("BAYERPAT");

        //Call extractRGB
        extractRGB(Objects.requireNonNull(extractFloatData(fitsImage)), bayerpat, redImage, greenImage, blueImage);
    }

    /**
     * Check all images are the same size
     *
     * @param images
     * @return
     */
    public static boolean hasConsistentImageSize(Fits[] images) {
        Fits firstImage = images[0];
        int[] axes = firstImage.getAxes();
        int width = axes[0], height = axes[1];

        if (!Arrays.stream(images).allMatch(fitsImage -> fitsImage.getAxes()[0] == width)) {
            return true;
        }
        if (!Arrays.stream(images).allMatch(fitsImage -> fitsImage.getAxes()[1] == height)) {
            return true;
        }
        return false;
//		//To store image size
//		int width = -1, height = -1;
//
//		//Check each image
//		for(Fits image : images)
//		{
//			//Get dimensions
//			int[] axes = image.getAxes();
//
//			//Populate initial image size
//			if (width < 0 || height < 0)
//			{
//				width = axes[0];
//				height = axes[1];
//			}
//			//Check consistency of image sizes
//			else if (axes[0] != width || axes[1] != height)
//				return false;
//		}
//
//		//If we reach here then all images are equal length
//		return true;
    }

    /**
     * Get Fits images from folder path
     *
     * @param sourceFolder
     * @return
     * @throws IOException
     */
    public static Fits[] getFitsFilesFromFolder(String sourceFolder) throws IOException {
        //Extract files in the folder, filtering for .fit or .fits only
        File folder = new File(sourceFolder);
        //
        FileFilter fileFilter = dir -> Pattern.compile("^.*.fits?$").matcher(dir.getName()).find();
        File[] files = folder.listFiles(fileFilter);
        if (files == null) {
            throw new IOException(String.format("Unable to list the files in the directory: %s", sourceFolder));
        }

        //Create a Fits object for each file
        Fits[] fitsImages = new Fits[files.length];

        try {
            for (int i = 0; i < fitsImages.length; i++)
                fitsImages[i] = new Fits(files[i].getPath());
        } catch (Exception e) {
            _logger.logException("Can't create Fits from files", e);
            return null;
        }

        //Return the array of Fits objects
        return fitsImages;
    }

    //Write fits image to a file
    public static void writeFits(String destination, float[][] data) {
        try {
            // Create a new FITS file
            File outputFile = new File(destination);
            Fits fitsFile = new Fits();

            // Get image dimensions
            int width = data[0].length;
            int height = data.length;

            // Convert float[][] to short[][]
            final int MAX_USHORT_VAL = Short.MAX_VALUE + Math.abs(Short.MIN_VALUE);
            short[][] fitsData = new short[height][width];

            //Scale float data to short range
            for (int row = 0; row < height; row++)
                for (int col = 0; col < width; col++)
                    fitsData[row][col] = (short) (data[row][col] * MAX_USHORT_VAL + Short.MIN_VALUE);

            // Add data to fits file
            BasicHDU<?> hdu = Fits.makeHDU(fitsData);
            fitsFile.addHDU(hdu);

            // Write the FITS file to disk
            fitsFile.write(outputFile);
            fitsFile.close();

        } catch (Exception e) {
            _logger.logException(e);
        }
    }

    public static void writeFits(String destination, float[][] data, Header newHeader) {
        try {
            // Create a new FITS file
            File outputFile = new File(destination);
            Fits fitsFile = new Fits();

            // Get image dimensions
            int width = data[0].length;
            int height = data.length;

            // Convert float[][] to short[][]
            int MAX_USHORT_VAL = Short.MAX_VALUE + Math.abs(Short.MIN_VALUE);
            short[][] fitsData = new short[height][width];

            //Scale float data to short range
            for (int row = 0; row < height; row++)
                for (int col = 0; col < width; col++)
                    fitsData[row][col] = (short) (data[row][col] * MAX_USHORT_VAL + Short.MIN_VALUE);

            // Add data to fits file
            ImageHDU hdu = new ImageHDU(newHeader, new ImageData(fitsData));
            fitsFile.addHDU(hdu);

            // Write the FITS file to disk
            fitsFile.write(outputFile);
            fitsFile.close();
        } catch (Exception e) {
            _logger.logException(e);
        }
    }

    //TODO: Might need to adjust image format

    /**
     * @param filePath
     * @return image portion of FITS file as a BufferedImage
     */
    public static BufferedImage getImagePNG(String filePath) {
        Fits fits = null;
        BufferedImage image = null;

        //Read Fits file
        try {
            fits = new Fits(filePath);
        } catch (Exception e) {
            _logger.logException(e);
        }

        //Read in raw float data
        float[][] imageData = extractFloatData(fits);

        if (imageData != null) //If there is image data
        {
            //Get image width and height
            int height = imageData.length;
            int width = imageData[0].length;

            //Construct and populate Buffered image
            image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    float pixelValue = imageData[y][x];
                    int normalizedPixel = Math.min((int) pixelValue, 255); //Cap values at 255
                    int pixel = (normalizedPixel << 16) | (normalizedPixel << 8) | normalizedPixel; //Format for png
                    image.setRGB(x, y, pixel); //Update BufferedImage
                }
            }
        }

        //Return BufferedImage
        return image;

    }


    //TODO: Might need to adjust image format
    // FIXME: Every image generated has come out completely black.

    /**
     * Creates a PNG file from RGB or greyscale data
     *
     * @param destination
     * @param red
     * @param green
     * @param blue
     */
    public static void createPNG(String destination, float[][] red, float[][] green, float[][] blue) {

        //Find width and height
        int width = red[0].length;
        int height = red.length;
        BufferedImage image;

        try {
            //Create appropriately sized image
            image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

            int r, g, b, rgb;
            // Set pixel values from raw data
            for (int row = 0; row < width; row++) {
                for (int col = 0; col < height; col++) {
                    //Map values to 0-255 range
                    r = (int) (red[col][row] / Short.MAX_VALUE * 255) & 0xFF;
                    g = (int) (green[col][row] / Short.MAX_VALUE * 255) & 0xFF;
                    b = (int) (blue[col][row] / Short.MAX_VALUE * 255) & 0xFF;
                    rgb = (r << 16) | (g << 8) | b; //Encode in png format
                    image.setRGB(row, col, rgb);
                }
            }

            // Save the image as PNG
            ImageIO.write(image, "PNG", new File(destination));
        } catch (Exception e) {
            _logger.logException(e);
        }
    }

    public static void createPNG(String destination, float[][] grayscale) {
        createPNG(destination, grayscale, grayscale, grayscale);
    }


    /**
     * Prints out header information
     *
     * @param filePath
     */
    public static String[] getHeaderData(String filePath) {
        Fits fits;
        BasicHDU imageHDU = null;

        //Read Fits file
        try {
            fits = new Fits(filePath);
            imageHDU = fits.getHDU(0);
        } catch (Exception e) {
            _logger.logException(e);
        }

        //Read header
        List<String> list = new ArrayList<>();
        Cursor<String, HeaderCard> header = imageHDU.getHeader().iterator();
        String headerValue;
        while (header.hasNext()) {
            headerValue = header.next().toString().trim();
            list.add(headerValue);
            _logger.info(headerValue);
        }
        return list.toArray(new String[]{});
    }

    //A test program
    public static void main(String[] args) {
        try {
            Fits fits = new Fits(Constants.testFitsPath + "/img-0079r.fits");
            BasicHDU imageHDU = fits.getHDU(0);
            Object data = imageHDU.getKernel();
            Data d = imageHDU.getData();

            _logger.info(fits.getFileName());
            _logger.info(fits.getFilePath());

            float[][] da = extractFloatData(fits);
            writeFits("test/output", da);


            int imgWidth = da[0].length, imgHeight = da.length;
            float[][] redImage = new float[imgHeight][imgWidth];
            float[][] greenImage = new float[imgHeight][imgWidth];
            float[][] blueImage = new float[imgHeight][imgWidth];

            //extractRGB(da, "BGGR", redImage, blueImage, greenImage);
            getHeaderData("test/input/IMG_0001.fits");

            // writeFits("test/output", da);
            // createPNG("test/output", redImage, greenImage, blueImage);
            // createPNG("test/output", da);

            // Print out pixel value at specific point
            // int x = 0, y = 0;
            // Scanner scan = new Scanner(System.in);
            // do
            // {
            //	 _logger.info("X: ");
            //	 x = scan.nextInt();
            //	 _logger.info("Y: ");
            //	 y = scan.nextInt();

            //	 _logger.info(da[y][x]);
            //	 _logger.info(String.format("(%d, %d) -> (%f, %f, %f)", x, y, redImage[y][x], greenImage[y][x], blueImage[y][x]));

            // } while (x >0 && y>0);


        } catch (Exception e) {
            _logger.logException(e);
        }
    }
}
