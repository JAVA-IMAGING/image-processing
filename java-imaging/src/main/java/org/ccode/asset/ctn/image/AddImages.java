package org.ccode.asset.ctn.image;

import org.ccode.asset.ctn.image.extensions.Array2D;
import org.ccode.asset.ctn.image.extensions.Fits;
import org.ccode.asset.ctn.image.extensions.FitsDocument;
import org.ccode.asset.ctn.logging.Logger;
import org.ccode.asset.ctn.logging.LoggerBuilder;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class AddImages {
    private static final Logger _logger = LoggerBuilder.defaultLogger(AddImages.class.getName());

    /**
     * Adds and averages an array of images
     *
     * @param images
     * @return
     */
    public static float[][] addImages(List<float[][]> images) {
        int width = images.get(0)[0].length;
        int height = images.get(0).length;
        int numberOfImages = images.size();

        float[][] resultImage = new float[height][width];
        Array2D.setValue(resultImage, 0);

        //Add all images together
        for (float[][] image : images)
            Array2D.add(resultImage, image);

        //Divide by number of images to normalize values
        Array2D.multiply(resultImage, 1f / numberOfImages);

        return resultImage;
    }

    public static boolean execute(Fits[] images, String outputFolder) throws FileNotFoundException, IllegalArgumentException {
        //Check that destination is a valid Folder
        if (!Files.isDirectory(Paths.get(outputFolder))) {
            throw new FileNotFoundException(String.format("Invalid folder path: %s.", outputFolder));
        }
        //Check that there's at least one file
        if (images.length == 0) return true;

        //Check that images are the same size
        if (FitsDocument.hasConsistentImageSize(images)) {
            throw new IllegalArgumentException("Images are different sizes.");
        }

        //Initialize necessary variables
        final int SUBSET_SIZE = 50; //Batch size to work with

        List<float[][]> floatImages = new ArrayList<>();
        List<float[][]> addedSubsetImages = new ArrayList<>();
        List<Thread> threads = new ArrayList<>();

        //Populate arrayList
        for (Fits image : images) {
            floatImages.add(FitsDocument.extractFloatData(image));
        }

        //Create thread to add each subset
        for (int startIndex = 0; startIndex < floatImages.size(); startIndex += SUBSET_SIZE) {
            //Find the end index of the subset
            int endIndex = startIndex + SUBSET_SIZE;
            if (endIndex >= floatImages.size()) {
                endIndex = floatImages.size();
            }

            //Create final variables for use inside the thread
            //final int threadNum = startIndex / SUBSET_SIZE;
            final int startIndexCopy = startIndex;
            final int endIndexCopy = endIndex;
            // _logger.info(String.format("(start, end): (%d,%d)\n", startIndexCopy, endIndexCopy));

            //Create thread to add subset of images
            Thread thread = new Thread(() -> {
                float[][] addedSubset = addImages(floatImages.subList(startIndexCopy, endIndexCopy)); //Add images

                //Critical section
                synchronized (addedSubsetImages) {
                    addedSubsetImages.add(addedSubset);
                } //Store result

            });
            threads.add(thread);
            thread.start();
        }


        // Wait for all threads to finish
        for (Thread thread : threads) {
            try {
                thread.join();
            } //Wait for the thread
            catch (InterruptedException e) {
                _logger.logException(e);
            }
        }

        //Add the results from the subtasks
        float[][] addedImage = addImages(addedSubsetImages);

        //Save image
        String fileName = images[0].getFileName();
        FitsDocument.writeFits(String.format("%s/Combined/%s", outputFolder, fileName), addedImage);

        return true;
    }

    public static boolean execute(String sourceFolder, String outputFolder) throws IllegalArgumentException, IOException {
        //Get Fits files in the folder
        Fits[] fitImages = FitsDocument.getFitsFilesFromFolder(sourceFolder);
        if (fitImages == null) return false;

        //Call execute function
        return execute(fitImages, outputFolder);
    }

    public static void main(String[] args) {
        try {
            execute("./test/input/Blue", "./test/output");
        } catch (IllegalArgumentException | IOException e) { // FileNotFoundException is a child of IO Exception
            _logger.logException(e);
        }
    }
}
