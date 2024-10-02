package org.ccode.asset.ctn.image;
import org.ccode.asset.ctn.image.extensions.Array2D;
import org.ccode.asset.ctn.image.extensions.Fits;
import org.ccode.asset.ctn.image.extensions.FitsDocument;
import org.ccode.asset.ctn.logging.Logger;
import org.ccode.asset.ctn.logging.LoggerBuilder;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.DefaultDrawingSupplier;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYBarPainter;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.statistics.HistogramType;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Takes flat images and separates them into corrected red, green, blue, and luminance flat images
 */
public class MakeFlatView {
    private static final Logger _logger = LoggerBuilder.defaultLogger(MakeFlatView.class.getName());

    public static final int HISTOGRAM_BINS = 4096;

    /**
     * Creates a histogram by mapping the array to the specified number of bins
     *
     * @param data
     * @param numBins
     * @param minX
     * @param maxX
     * @return
     */
    public static int[] createHistogram(float[] data, int numBins, float minX, float maxX) {
        int[] histogram = new int[numBins];
        float binWidth = (maxX - minX) / (numBins - 1);

        for (float value : data) {
            //Clip extreme values
            if (value > maxX) value = maxX;
            if (value < minX) value = minX;

            //Update the appropriate bin
            int binIndex = (int) ((value - minX) / binWidth);
            histogram[binIndex]++;
        }

        return histogram;
    }

    /**
     * Returns a dictionary with histogram stats
     * Keys:
     * "sd": Standard Deviation
     * "mean": Mean
     * "histStart": Approximate start of the histogram
     * "histEnd": Approximate end of the histogram
     *
     * @param histogram
     * @return
     */
    public static Map<String, Float> analyzeHistogram(int[] histogram) {
        //For the mean
        int maxVal = 0, maxIndex = 0;

        //Find the max value and calculate the mean
        for (int i = 1; i < HISTOGRAM_BINS; i++) {
            if (histogram[i] > maxVal) {
                maxVal = histogram[i];
                maxIndex = i;
            }
        }
        float mean = (float) maxIndex / (float) HISTOGRAM_BINS;

        //For the standard deviation
        float histStart = 0, histEnd = 0;

        //What percentage of the maximum is considered the start/end of the histogram
        final float STARTING_THRESHOLD = 1f / 100f;

        //Find the start of the histogram
        for (int i = 1; i < HISTOGRAM_BINS; i++) {
            if (histogram[i] > maxVal * STARTING_THRESHOLD) {
                histStart = (float) i / (float) HISTOGRAM_BINS;
                break;
            }
        }

        //Find the end of the histogram
        for (int i = HISTOGRAM_BINS - 1; i > 0; i--) {
            if (histogram[i] > maxVal * STARTING_THRESHOLD) {
                histEnd = (float) i / (float) HISTOGRAM_BINS;
                break;
            }
        }

        //Estimate the standard deviation
        //The interval of the normal distribution is about 6 standard deviations
        float sd = (histEnd - histStart) / 6;

        //Create a Map with the stats
        Map<String, Float> stats = new HashMap<>();
        stats.put("mean", mean);
        stats.put("sd", sd);
        stats.put("histStart", histStart);
        stats.put("histEnd", histEnd);

        return stats;
    }

    /**
     * Applies a transformation to equalize the color mean and standard deviations for the histograms
     *
     * @param redImage
     * @param greenImage
     * @param blueImage
     */
    public static void equalizeColors(float[][] redImage, float[][] greenImage, float[][] blueImage) {
        //Get stats
        Map<String, Float> redStat = analyzeHistogram(createHistogram(Array2D.flatten(redImage), HISTOGRAM_BINS, 0, 1));
        Map<String, Float> greenStat = analyzeHistogram(createHistogram(Array2D.flatten(greenImage), HISTOGRAM_BINS, 0, 1));
        Map<String, Float> blueStat = analyzeHistogram(createHistogram(Array2D.flatten(blueImage), HISTOGRAM_BINS, 0, 1));

        float redMean = redStat.get("mean"), redSD = redStat.get("sd");
        float greenMean = greenStat.get("mean"), greenSD = greenStat.get("sd");
        float blueMean = blueStat.get("mean"), blueSD = blueStat.get("sd");

		/*Print stats
		 _logger.log("\tSD\t\tMean");
		 _logger.log(String.format("Red:\t%f\t%f", redMean, redSD));
		 _logger.log(String.format("Green:\t%f\t%f", greenMean, greenSD));
		 _logger.log(String.format("Blue:\t%f\t%f", blueMean, blueSD));*/

		/* Apply mean and sd transformation
			Ref: https://math.stackexchange.com/questions/2894681/how-to-transform-shift-the-mean-and-standard-deviation-of-a-normal-distribution
		
			Calculates transformation coefficients a and b for each color
			Performs the transformation aX + b
			Uses green as the reference distribution
			*/
        float greenA, greenB, blueA, blueB, redA, redB; //Transformation coefficients
        greenA = 1f;
        greenB = greenMean - greenA * greenMean; //Should be 0

        blueA = greenSD / blueSD;
        blueB = greenMean - blueA * blueMean;

        redA = greenSD / redSD;
        redB = greenMean - redA * redMean;

        //Store values in ImageParameters
        ImageParameters.blueA = blueA;      // no clue why we're storing it at ImageParameters
        ImageParameters.blueB = blueB;      // but probably better left alone
        ImageParameters.redA = redA;        // I could be tweaking but I'm pretty sure
        ImageParameters.redB = redB;        // this is the only lines where ImageParameters
        ImageParameters.greenA = greenA;    // is used
        ImageParameters.greenB = greenB;

        //Apply transformations
        Array2D.multiply(greenImage, greenA);
        Array2D.add(greenImage, greenB);

        Array2D.multiply(blueImage, blueA);
        Array2D.add(blueImage, blueB);

        Array2D.multiply(redImage, redA);
        Array2D.add(redImage, redB);
    }

    /**
     * Creates a luminance array
     *
     * @param redImage
     * @param greenImage
     * @param blueImage
     * @return
     */
    public static float[][] createLuminance(float[][] redImage, float[][] greenImage, float[][] blueImage) {
        //Create copies of the arrays as to not modify the originals
        float[][] redCopy = Array2D.copy(redImage);
        float[][] greenCopy = Array2D.copy(greenImage);
        float[][] blueCopy = Array2D.copy(blueImage);

        //Luminance = 0.222*red + 0.707*green + 0.071*blue
        //Based on how humans see color
        Array2D.multiply(redCopy, 0.222f);
        Array2D.multiply(greenCopy, 0.707f);
        Array2D.multiply(blueCopy, 0.071f);
        float[][] luminance = Array2D.add(redCopy, greenCopy, blueCopy);

        //Get stats
        Map<String, Float> luminanceStats = analyzeHistogram(
                createHistogram(Array2D.flatten(luminance), HISTOGRAM_BINS, 0, 1));

        //Shifts luminance histogram to center mean at 0.5
        Array2D.add(luminance, 0.5f - luminanceStats.get("mean"));
        Array2D.clip(luminance, 0, 1);

        return luminance;
    }

    /**
     * Displays overlapping histograms
     *
     * @param src
     */
    public static void drawHistograms(String src) {
        try {
            drawHistograms(src, null);
        } catch (IOException e) {
            _logger.logException(e);
        }
    }

    /**
     * Displays overlapping histograms
     *
     * @param src
     * @param saveFile The PNG file where the histogram graph should be saved.
     * @throws IOException If the graph cannot be saved to the given saveFile.
     */
    public static void drawHistograms(String src, File saveFile) throws IOException {
        JFrame f = new JFrame();
        f.setTitle("Histogram");
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);


        //Get fits
        Fits fits;
        try {
            fits = new Fits(src);
        } catch (Exception e) {
            _logger.logException(e);
            return;
        }


        //Extract data
        int imgWidth = fits.getAxes()[0], imgHeight = fits.getAxes()[1];
        float[][] redImage = new float[imgHeight][imgWidth];
        float[][] greenImage = new float[imgHeight][imgWidth];
        float[][] blueImage = new float[imgHeight][imgWidth];
        FitsDocument.extractRGB(fits, redImage, blueImage, greenImage);

        //Equalize and create luminance
        equalizeColors(redImage, greenImage, blueImage);
        float[][] luminance = createLuminance(redImage, greenImage, blueImage);

        //Histogram accepts type double[], not float[][]. We convert using Array2D methods
        HistogramDataset dataset = new HistogramDataset();
        dataset.addSeries("red", Array2D.castToDouble(Array2D.flatten(redImage)), HISTOGRAM_BINS, 0, 1);
        dataset.addSeries("green", Array2D.castToDouble(Array2D.flatten(greenImage)), HISTOGRAM_BINS, 0, 1);
        dataset.addSeries("blue", Array2D.castToDouble(Array2D.flatten(blueImage)), HISTOGRAM_BINS, 0, 1);
        dataset.setType(HistogramType.FREQUENCY);

        //Add the 3 histograms to the chart
        JFreeChart chart = ChartFactory.createHistogram("Histogram", "Value",
                "Count", dataset, PlotOrientation.VERTICAL, true, true, false);

        //Render the chart
        XYPlot plot = (XYPlot) chart.getPlot();
        XYBarRenderer renderer = (XYBarRenderer) plot.getRenderer();
        renderer.setBarPainter(new StandardXYBarPainter());

        //Set translucent colors
        Paint[] paintArray = {
                new Color(0x80ff0000, true),
                new Color(0x8000ff00, true),
                new Color(0x800000ff, true),
        };
        plot.setDrawingSupplier(new DefaultDrawingSupplier(
                paintArray,
                DefaultDrawingSupplier.DEFAULT_FILL_PAINT_SEQUENCE,
                DefaultDrawingSupplier.DEFAULT_OUTLINE_PAINT_SEQUENCE,
                DefaultDrawingSupplier.DEFAULT_STROKE_SEQUENCE,
                DefaultDrawingSupplier.DEFAULT_OUTLINE_STROKE_SEQUENCE,
                DefaultDrawingSupplier.DEFAULT_SHAPE_SEQUENCE));

        if (saveFile != null) {
            ChartUtils.saveChartAsPNG(saveFile, chart, 1600, 900);
        }

        //Add and display chart
        ChartPanel chartPanel = new ChartPanel(chart);
        f.add(chartPanel);
        f.pack();
        f.setVisible(true);
    }

    /**
     * Creates flat images (returns true upon success)
     *
     * @param flatImages
     * @param outputFolder The folder where the images should be written to.
     * @return If the process was successful.
     * @throws FileNotFoundException    If the outputFolder is not a valid directory.
     * @throws IllegalArgumentException If the images are different sizes.
     */
    public static boolean execute(Fits[] flatImages, String outputFolder) throws FileNotFoundException, IllegalArgumentException {
        //Check that destination is a valid Folder
        if (!Files.isDirectory(Paths.get(outputFolder))) {
            try {
                Files.createDirectory(Path.of(outputFolder));
                for (String subDirectory : new String[]{"Red", "Green", "Blue", "Luminance"}) {
                    Files.createDirectory(Path.of(String.format("%s/%sFlat", outputFolder, subDirectory)));
                }
            } catch (IOException e) {
                throw new FileNotFoundException(String.format("Invalid folder path: %s.", outputFolder));
            }
        }
        //Check that there's at least one file
        if (flatImages.length == 0) return true;

        //Check that images are the same size
        if (FitsDocument.hasConsistentImageSize(flatImages)) {
            throw new IllegalArgumentException("Images are different sizes.");
        }

        //Get image dimensions
        int[] dimensions = flatImages[0].getAxes();
        int width = dimensions[0];
        int height = dimensions[1];

        _logger.info(String.format("Width: %d, Height: %d\n", width, height));

        //Create RGB+L arrays
        float[][] redFlat = new float[height][width];
        float[][] greenFlat = new float[height][width];
        float[][] blueFlat = new float[height][width];
        float[][] luminanceFlat;

//		int i = 0;
        //Create RGB+L flats for each flat image
        for (Fits flatImage : flatImages) {
            //Extract image RGB data
            FitsDocument.extractRGB(flatImage, redFlat, greenFlat, blueFlat);

            //Equalize the color histograms
            equalizeColors(redFlat, greenFlat, blueFlat);

            //Create luminance image
            luminanceFlat = createLuminance(redFlat, greenFlat, blueFlat);

            //Write RGB+L flats to files
            String fileName = flatImage.getFileName();
            FitsDocument.writeFits(String.format("%s/RedFlat/%s", outputFolder, fileName), redFlat);
            FitsDocument.writeFits(String.format("%s/GreenFlat/%s", outputFolder, fileName), greenFlat);
            FitsDocument.writeFits(String.format("%s/BlueFlat/%s", outputFolder, fileName), blueFlat);
            FitsDocument.writeFits(String.format("%s/LuminanceFlat/%s", outputFolder, fileName), luminanceFlat);
//			FitsDocument.createPNG(String.format("%s/test%d.png", outputFolder, i++), redFlat, greenFlat, blueFlat);    // probably broken?
        }

        //Successfully created flat images
        return true;
    }

    public static void execute(String sourceFolder, String outputFolder) throws IllegalArgumentException, IOException {
        //Get Fits files in the folder
        Fits[] fitImages = FitsDocument.getFitsFilesFromFolder(sourceFolder);
        if (fitImages == null) return;

        //Call execute function
        execute(fitImages, outputFolder);
    }

    public static void main(String[] args) {
        try {
            execute("./test/input/Flat 4 Sheets no_flip", "./test/output/MakeFlatView output");
            File saveFile = new File("./test/output/MakeFlatView output/histogram.png");
            drawHistograms("./test/input/IMG_0001.fits", saveFile);
        } catch (IllegalArgumentException | IOException e) { // FileNotFoundException is a child of IO Exception
            _logger.logException(e);
        }
    }
}
