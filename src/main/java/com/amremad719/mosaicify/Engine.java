package com.amremad719.mosaicify;

import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.IOException;

/**
 * The {@code Engine} class handles the core logic for generating a photomosaic.
 * It follows the singleton design pattern and extends {@link Thread} to run mosaic
 * generation in a background thread. It uses OpenCV to process and generate the output image.
 */
public class Engine extends Thread {
    private static Engine instance; // Singleton instance

    /** Library containing the collection of sub-images used to construct the mosaic. */
    public SubImagesLibrary subImagesLibrary = new SubImagesLibrary();

    private File selectedImage;
    private Mat result;
    private Size subDivisionCount = new Size(64, 36);

    /**
     * Returns the singleton instance of the {@code Engine} class.
     *
     * @return the single instance of the engine.
     */
    public static Engine getInstance() {
        if (instance == null) {
            synchronized (Engine.class) {
                if (instance == null) {
                    instance = new Engine();
                }
            }
        }
        return instance;
    }

    /**
     * Sets the number of subdivisions (tiles) in the mosaic grid.
     *
     * @param subDivisionCount the number of horizontal and vertical subdivisions.
     */
    public void setSubDivisionCount(Size subDivisionCount) {
        this.subDivisionCount = subDivisionCount;
    }

    /**
     * Replaces a region of the main image with a resized sub-image.
     *
     * @param mainImage         the destination image.
     * @param replacementImage  the sub-image to insert.
     * @param replacementRegion the region in the main image to replace.
     */
    private void replaceImageRegion(Mat mainImage, Mat replacementImage, Rect replacementRegion) {
        // Resize the replacement image to exactly fit the region to be replaced
        Mat resizedReplacement = new Mat();
        Size targetSize = new Size(replacementRegion.width, replacementRegion.height);
        Imgproc.resize(replacementImage, resizedReplacement, targetSize);

        // Extract the region of interest (ROI) from the main image
        Mat roi = mainImage.submat(replacementRegion);

        // Overwrite the ROI with the resized replacement
        resizedReplacement.copyTo(roi);
    }

    /**
     * Generates the photomosaic by dividing the input image and replacing each segment
     * with the best-matching sub-image from the library.
     *
     * @throws IOException if the selected image cannot be read.
     */
    public void generateMosaic() throws IOException {
        // Load the selected image from disk
        Mat image = Imgcodecs.imread(selectedImage.toString());

        // Resize input image to match the desired output resolution
        Size targetSize = new Size(
                MosaicifyController.getInstance().outputResolutionWidthSpinner.getValue(),
                MosaicifyController.getInstance().outputResolutionHeightSpinner.getValue());
        Imgproc.resize(image, image, targetSize);

        // Calculate the size of each tile/subdivision in the mosaic grid
        Size subDivisionSize = new Size(
                (int) (image.width() / subDivisionCount.width),
                (int) (image.height() / subDivisionCount.height));

        // Prepare an empty matrix for the final mosaic image
        result = new Mat(new Size(
                subDivisionSize.width * subDivisionCount.width,
                subDivisionSize.height * subDivisionCount.height),
                image.type());

        // Iterate over the image grid to replace each block with a matching sub-image
        for (int i = 0; i < subDivisionCount.height; i++) {
            for (int j = 0; j < subDivisionCount.width; j++) {
                // Define the region of interest (ROI) for the current tile
                Rect roi = new Rect(
                        (int) (j * subDivisionSize.width),
                        (int) (i * subDivisionSize.height),
                        (int) subDivisionSize.width,
                        (int) subDivisionSize.height);

                // Extract the tile from the base image
                Mat kernel = image.submat(roi);

                // Find the best matching sub-image from the library
                Mat match = subImagesLibrary.findBestMatch(kernel);

                // Replace the corresponding tile in the result image
                replaceImageRegion(result, match, roi);

                // Update the progress bar in the UI
                double progress = ((i * subDivisionCount.width) + j + 1)
                        / (subDivisionCount.width * subDivisionCount.height);
                MosaicifyController.getInstance().updateProgressBar(progress);
            }
        }
    }

    /**
     * Sets the image file to be used for generating the photomosaic.
     *
     * @param selectedImage the image file chosen by the user.
     */
    public void setSelecetedImage(File selectedImage) {
        this.selectedImage = selectedImage;
    }

    /**
     * Runs the photomosaic generation in a separate thread.
     * On completion, saves the output and updates the UI.
     */
    @Override
    public void run() {
        try {
            generateMosaic();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        onFinish();
    }

    /**
     * Saves the generated mosaic to disk, updates the UI status label.
     */
    private void onFinish() {
        // Parse file name and extension to build output file name
        String fileName = selectedImage.toString();
        int dotIndex = fileName.lastIndexOf('.');
        String baseName = (dotIndex != -1) ? fileName.substring(0, dotIndex) : fileName;
        String extension = (dotIndex != -1) ? fileName.substring(dotIndex) : "";

        // Generate new file name for output image
        String newFileName = baseName + "_Photomosaic" + extension;

        // Save the resulting mosaic image to disk
        Imgcodecs.imwrite(newFileName, result);

        // Inform the user via the UI
        MosaicifyController.getInstance().updateProgresStatusLabel(
                "Photomosaic generated successfully. Result saved to " + newFileName);
    }
}
