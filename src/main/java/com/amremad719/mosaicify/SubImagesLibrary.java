package com.amremad719.mosaicify;

import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;

/**
 * The {@code SubImagesLibrary} class manages a collection of preprocessed sub-images
 * used to build a photomosaic. It allows scanning a directory of images, preprocessing them,
 * and finding the best visual match for a given image patch.
 *
 * This class extends {@link Thread} to allow background processing of image libraries.
 */
public class SubImagesLibrary extends Thread {

    /** The path to the directory containing sub-images. */
    private Path libraryPath = null;

    /** The dimensions to which each sub-image and kernel is resized for matching. */
    private Size kernelSubDivisionDim = new Size(4, 4);

    /** A map from file paths to their processed image matrices. */
    private final Map<String, Mat> processedImages = new HashMap<>();

    /**
     * Sets the size to which sub-images and kernels are resized before comparison.
     *
     * @param kernelSubDivisionDim the size to resize each image to.
     */
    public void setKernelSubDivisionDim(Size kernelSubDivisionDim) {
        this.kernelSubDivisionDim = kernelSubDivisionDim;
    }

    /**
     * Reads and preprocesses a sub-image and stores it in memory.
     *
     * @param filePath the path to the image file.
     * @throws IOException if reading or processing the image fails.
     */
    public void processSubImage(Path filePath) throws IOException {
        Mat image = Imgcodecs.imread(filePath.toString());
        Imgproc.resize(image, image, kernelSubDivisionDim);
        processedImages.put(filePath.toString(), image);
    }

    /**
     * Computes the Mean Squared Error (MSE) between two images.
     *
     * @param a the first image (reference).
     * @param b the second image (to compare).
     * @return the MSE value.
     */
    private double computeMSE(Mat a, Mat b) {
        double mse = 0.0;

        // Ensure images are the same size before comparing
        if (a.size().width != b.size().width || a.size().height != b.size().height) {
            Imgproc.resize(b, b, a.size(), 0, 0, Imgproc.INTER_AREA);
        }

        // Loop through all pixels
        for (int i = 0; i < a.rows(); i++) {
            for (int j = 0; j < a.cols(); j++) {
                double[] pixelA = a.get(i, j);
                double[] pixelB = b.get(i, j);

                // Sum squared differences across all three channels (BGR)
                for (int k = 0; k < 3; k++) {
                    double diff = pixelA[k] - pixelB[k];
                    mse += diff * diff;
                }
            }
        }

        // Normalize MSE by total number of color values
        mse /= (a.rows() * a.cols() * 3);
        return mse;
    }

    /**
     * Finds the best-matching sub-image in the library for the given kernel.
     *
     * @param kernel the image patch to match against the library.
     * @return the best-matching sub-image from disk (unprocessed).
     * @throws IOException if no match is found.
     */
    public Mat findBestMatch(Mat kernel) throws IOException {
        Imgproc.resize(kernel, kernel, kernelSubDivisionDim);

        double minMSE = -1;
        Map.Entry<String, Mat> match = null;

        // Compare the processed kernel to all processed sub-images
        for (Map.Entry<String, Mat> entry : processedImages.entrySet()) {
            double error = computeMSE(kernel, entry.getValue());

            // Track the image with the lowest MSE
            if (error < minMSE || minMSE < 0) {
                minMSE = error;
                match = entry;
            }
        }

        if (match == null) {
            throw new IOException("Failed to find a match for a kernel");
        }

        // Load and return the original (unprocessed) matching image
        return Imgcodecs.imread(match.getKey());
    }

    /**
     * Loads and processes all images from the library directory into memory.
     * Updates the UI to reflect progress.
     *
     * @throws ArithmeticException if the library path has not been set.
     */
    public void searchLibrary() throws ArithmeticException {
        if (libraryPath == null) {
            throw new ArithmeticException("Library path not set");
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(libraryPath)) {
            double i = 0;
            int total = libraryPath.toFile().list().length;

            // Process each image file in the directory
            for (Path entry : stream) {
                processSubImage(entry);
                i++;

                // Update UI with progress
                MosaicifyController.getInstance().updateProgressBar(i / total);
                MosaicifyController.getInstance().updateProgresStatusLabel(
                        "Found and processed " + (int)i + "/" + total + " images");
            }

            MosaicifyController.getInstance().updateProgressBar(0);
            MosaicifyController.getInstance().updateProgresStatusLabel(
                    "Found and processed " + processedImages.size() + " images");

        } catch (IOException | DirectoryIteratorException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sets the directory from which to load sub-images.
     *
     * @param libraryDirectory a valid directory path.
     * @return {@code true} if the path exists and is a directory; {@code false} otherwise.
     */
    public boolean setLibraryDirectory(Path libraryDirectory) {
        if (Files.exists(libraryDirectory) && Files.isDirectory(libraryDirectory)) {
            this.libraryPath = libraryDirectory;
            return true;
        }
        return false;
    }

    /**
     * Prepares the UI and starts the background thread to scan the library.
     */
    public void update() {
        MosaicifyController.getInstance().librarySettingsVBox.setDisable(true);
        this.start();
    }

    /**
     * Called when the thread starts; launches the library search logic.
     */
    @Override
    public void run() {
        searchLibrary();
        onFinish();
    }

    /**
     * Called when the library has finished loading; re-enables UI controls.
     */
    public void onFinish() {
        MosaicifyController.getInstance().librarySettingsVBox.setDisable(false);
    }
}
