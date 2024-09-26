package org.ccode.asset.ctn.image;

import nom.tam.fits.FitsException;
import org.ccode.asset.ctn.image.extensions.Fits;
import org.ccode.asset.ctn.image.extensions.FitsDocument;
import org.ccode.asset.ctn.image.util.Constants;

import java.util.Arrays;
import java.util.Collections;

import static org.ccode.asset.ctn.image.MakeDarkView.loadAndSum;
import static org.ccode.asset.ctn.image.extensions.Fits.collectObjects;
import static org.ccode.asset.ctn.image.extensions.Fits.collectSciObjs;

public class Combine {
    //average
    public static float[][] combineFloatArraysAverage(float[][][] images){
        int numberOfImages = images.length;
        int ilen = images[0].length;
        int jlen = images[0][0].length;
        float[][] avgArray = new float[ilen][jlen];
        for (float[] floats : avgArray) {
            Arrays.fill(floats, 0);
        }

//        for (float[] arr : avgArray){
//            for (float x : arr){
//                System.out.print(x);
//            }
//            System.out.println();
//        }
        //System.out.println(avgArray);

        for (float[][] arr : images) {
            for (int i = 0; i < arr.length; i++) {
                for (int j = 0; j < arr[i].length; j++) {
                    avgArray[i][j] = avgArray[i][j] + arr[i][j];
                }
            }
        }

        //        for (float[] arr : avgArray){
//            for (float x : arr){
//                System.out.print(x);
//            }
//            System.out.println();
//        }

            for (int i = 0; i < images[0].length; i++) {
                for (int j = 0; j < images[0][i].length; j++) {
                    avgArray[i][j] = avgArray[i][j] / numberOfImages;
                }
            }


        return avgArray;


    }

    //medians
    public static float[][] combineFloatArraysMedian(float[][][] images){
        int numberOfImages = images.length;
        int ilen = images[0].length;
        int jlen = images[0][0].length;
        float[][] medianArray = new float[ilen][jlen];
        float[] sortarray;

        for (int i = 0; i < images[0].length; i++) {
            for (int j = 0; j < images[0][0].length; j++) {
                //medianArray[i][j] = medianArray[i][j] + images[][i][j];
                sortarray = new float[images.length];
                for (int k = 0; k < images.length;k++){
                    sortarray[k] = images[k][i][j];
                }
                Arrays.sort(sortarray);
                float val;
                if (images.length % 2 == 0 ){
                    val = (sortarray[images.length / 2 ] + sortarray[(images.length-1) / 2 ] )/2;
                }
                else {
                    val = sortarray[images.length / 2 ];
                }
                medianArray[i][j] =val;

            }
        }
        return medianArray;
    }

    //sum
    public static float[][] combineFloatArraysSum(float[][][] images) {
        //int numberOfImages = images.length;
        int ilen = images[0].length;
        int jlen = images[0][0].length;
        float[][] sumArray = new float[ilen][jlen];

        for (float[] floats : sumArray) {
            Arrays.fill(floats, 0);
        }
        for (float[][] arr : images) {
            for (int i = 0; i < arr.length; i++) {
                for (int j = 0; j < arr[i].length; j++) {

                    sumArray[i][j] = arr[i][j] + sumArray[i][j];
                }
            }
        }
        return sumArray;
    }
    public static float[][] combineFloatArraysSubtract(float[][] high, float[][]low){
        int ilen = high.length;
        int jlen = high[0].length;
        float[][] subArray = new float[ilen][jlen];
        for (int i = 0; i < ilen; i++) {
            for (int j = 0; j < jlen; j++) {
                subArray[i][j] = high[i][j] - low[i][j];
            }
        }
        return subArray;

    }
    public static float[][] flat_correct(float[][] ccd , float[][] flat){
        int ilen = flat.length;
        int jlen = flat[0].length;
        float[][] flatMean = new float[flat.length][flat[0].length];
        float sum = 0;
        for (float[] floats : flatMean) {
            Arrays.fill(floats, 0);
        }
        for (int i = 0; i < ilen; i++) {
            for (int j = 0; j < jlen; j++) {
                sum = flat[i][j] + sum;
            }
        }

        float flat_mean = sum / (ilen * jlen);
        System.out.println(flat_mean);
        float[][] use_flat = new float[flat.length][flat[0].length];

        //copy flat to use_flat
        for (int i = 0; i < ilen; i++) {
            for (int j = 0; j < jlen; j++) {
                use_flat[i][j] = flat[i][j];
            }
        }

        //use_flat = use_flat.divide(flat_mean)
        for (int i = 0; i < ilen; i++) {
            for (int j = 0; j < jlen; j++) {
                use_flat[i][j] = use_flat[i][j] / flat_mean;
            }
        }
//        System.out.println("use_flat");
//        for (float[] arr : use_flat){
//            for (float x : arr){
//                System.out.print(x+" ");
//            }
//            System.out.println();
//        }


        //flat_corrected = ccd.divide(use_flat)
        for (int i = 0; i < ilen; i++) {
            for (int j = 0; j < jlen; j++) {
                use_flat[i][j] = ccd[i][j] / use_flat[i][j];
            }
        }

//        System.out.println("use_flat next");
//        for (float[] arr : use_flat){
//            for (float x : arr){
//                System.out.print(x+" ");
//            }
//            System.out.println();
//        }

        return use_flat;
    }

    public static void main(String[] args) {
//        float[][] flat = {{1,2},{3,4}};
//        float[][] ccd = {{10,10},{10,10}};
//
////        float[][] arr3 = {{0,-1,2,3} , {0,150,2,3} , {0,150,2,3}};
////        float[][] arr4 = {{0,-10,2,3} , {0,150,2,3} , {0,150,2,3}};
//
////        float[][][] test = {arr1,arr2,arr3,arr4};
//
//
//        float[][] rtn = flat_correct(ccd , flat);
//        for (float[] arr : rtn){
//            for (float x : arr){
//                System.out.print(x+" ");
//            }
//            System.out.println();
//        }
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
            // _logger.logException(e);
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

//            FitsDocument.createPNG(Constants.testOutputPath + "dark_view_out.png", darkTest);
//            FitsDocument.createPNG(Constants.testOutputPath + "flat_view_out.png", flatTest);
//            FitsDocument.createPNG(Constants.testOutputPath + "bias_view_out.png", biasTest);
//            FitsDocument.createPNG(Constants.testOutputPath + "science_view_out.png", scienceTest);

            //_logger.info(Arrays.deepToString(test));
        } catch (IllegalArgumentException e) {
            //_logger.logException(e);
        }
    }
}
