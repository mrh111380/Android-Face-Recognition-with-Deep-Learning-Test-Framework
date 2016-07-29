/* Copyright 2016 Michael Sladoje and Mike Schälchli. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
==============================================================================*/

package ch.zhaw.facerecognition.Recognition;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.preference.PreferenceManager;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;

import java.util.ArrayList;
import java.util.List;

import ch.zhaw.facerecognition.Helpers.FileHelper;

/***************************************************************************************
 *    Title: TensorFlowAndroidDemo
 *    Author: miyosuda
 *    Date: 23.04.2016
 *    Code version: -
 *    Availability: https://github.com
 *
 ***************************************************************************************/

public class TensorFlow implements Recognition {
    private static final String STRING_SPLIT_CHARACTER = " ";

    private String inputLayer;
    private String outputLayer;

    private int inputSize;
    private int outputSize;

    Recognition rec;

    public TensorFlow(Context context, int method) {
        String dataPath = FileHelper.TENSORFLOW_PATH;
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences((context.getApplicationContext()));
        int numClasses = Integer.valueOf(sharedPref.getString("key_numClasses", "1001"));
        inputSize = Integer.valueOf(sharedPref.getString("key_inputSize", "224"));
        int imageMean = Integer.valueOf(sharedPref.getString("key_imageMean", "128"));
        outputSize = Integer.valueOf(sharedPref.getString("key_outputSize", "1024"));
        inputLayer = sharedPref.getString("key_inputLayer", "input");
        outputLayer = sharedPref.getString("key_outputLayer", "avgpool0");
        String modelFile = sharedPref.getString("key_modelFileTensorFlow", "tensorflow_inception_graph.pb");
        Boolean classificationMethod = sharedPref.getBoolean("key_classificationMethodTFCaffe", true);

        final AssetManager assetManager = context.getAssets();
        initializeTensorflow(assetManager, dataPath + modelFile, numClasses, inputSize, imageMean);

        if(classificationMethod){
            rec = new SupportVectorMachine(context, method);
        }
        else {
            rec = new KNearestNeighbor(context, method);
        }
    }

    // link jni library
    static {
        System.loadLibrary("tensorflow");
    }

    // connect the native functions
    private native int initializeTensorflow(AssetManager assetManager,
                                             String model,
                                             int numClasses,
                                             int inputSize,
                                             int imageMean);
    private native String classifyImageBmp(String inputLayer, String outputLayer, int outputSize, Bitmap bitmap);
    private native String classifyImageRgb(String inputLayer, String outputLayer, int outputSize, int[] output, int width, int height);

    @Override
    public boolean train() {
        return rec.train();
    }

    @Override
    public String recognize(Mat img, String expectedLabel) {
        return rec.recognize(getTFVector(img), expectedLabel);
    }

    @Override
    public void saveToFile() {

    }

    @Override
    public void loadFromFile() {

    }

    @Override
    public void saveTestData() {
        rec.saveTestData();
    }

    @Override
    public void addImage(Mat img, String label) {
        rec.addImage(getTFVector(img), label);
    }

    private Mat getTFVector(Mat img){
        Imgproc.resize(img, img, new Size(inputSize, inputSize));

        Bitmap bmp = Bitmap.createBitmap(inputSize, inputSize, Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(img, bmp);

        String[] sVector = classifyImageBmp(inputLayer, outputLayer, outputSize, bmp).split(STRING_SPLIT_CHARACTER);

        System.out.println(sVector.length);

        List<Float> fVector = new ArrayList<>();
        for(String s : sVector){
            fVector.add(Float.parseFloat(s));
        }

        return Converters.vector_float_to_Mat(fVector);
    }
}