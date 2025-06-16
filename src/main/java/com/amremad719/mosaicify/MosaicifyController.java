package com.amremad719.mosaicify;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.scene.image.ImageView;
import javafx.scene.image.Image;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ResourceBundle;

/**
 * Controller class for Mosaicify's JavaFX GUI.
 * Manages user interactions, GUI updates, and delegates logic to the Engine.
 */
public class MosaicifyController implements Initializable {

    // --- FXML UI Controls ---

    @FXML public CheckBox resizeOutputCheckBox;
    @FXML public ProgressBar progressBar;
    @FXML public Label
            progressStatusLabel,
            subImagesDirectoryLabel,
            selectedImageDirectoryLabel,
            subImagesLibraryFileCountLabel;
    @FXML
    public GridPane
            MosaicSubDivisionCountGrid,
            SubImageKernelDimentionsGrid;
    @FXML private Button
            setImageButton,
            generateMosaicButton,
            setLibraryDirectoryButton;
    @FXML private ImageView imageView;
    @FXML public Spinner<Integer>
            subImageKernelWidthSpinner,
            subImageKernelHeightSpinner;
    @FXML public Spinner<Integer>
            mosaicSubDivisionWidthSpinner,
            mosaicSubDivisionHeightSpinner;
    @FXML public Spinner<Integer>
            outputResolutionWidthSpinner,
            outputResolutionHeightSpinner;
    @FXML public VBox librarySettingsVBox;
    @FXML public HBox outputResolutionHBox;

    // --- Internal Fields ---

    private boolean isUpdatingSpinners = false;
    private File selectedImage;
    private static MosaicifyController instance;
    private final Engine engine = Engine.getInstance();

    /**
     * Returns the singleton instance of the controller.
     * @return the current controller instance
     */
    public static MosaicifyController getInstance() {
        return instance;
    }

    /**
     * Constructor that assigns the created controller to the static instance variable.
     * This works because the FXMLLoader calls the constructor automatically.
     */
    public MosaicifyController() {
        instance = this;
    }

    /**
     * Initializes the controller after its root element has been completely processed.
     */
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        this.initMosaicSubDivisionCountSpinners();
        this.initSubImageKernelDimensionsSpinners();
        this.initOutputResolutionSpinners();
        this.onResizeOutputCheckBox();
    }

    /**
     * Updates the progress bar from another thread using JavaFX's Platform.runLater().
     * @param progress a value between 0 and 1 representing progress
     */
    public void updateProgressBar(double progress) {
        Platform.runLater(() -> progressBar.setProgress(progress));
    }

    /**
     * Updates the status label text on the GUI thread.
     * @param status the status string to show
     */
    public void updateProgresStatusLabel(String status) {
        Platform.runLater(() -> progressStatusLabel.setText(status));
    }

    /**
     * Displays an error alert dialog with a header and message.
     * @param header short header
     * @param content detailed message
     */
    private void showError(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    // ---------- Spinner Initializations ----------

    /**
     * Initializes the spinners controlling sub-image kernel size.
     * These define how individual library images will be subdivided for matching.
     */
    private void initSubImageKernelDimensionsSpinners() {
        subImageKernelWidthSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10000, 4, 1));
        subImageKernelHeightSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10000, 4, 1));

        ChangeListener<Integer> listener = (obs, oldVal, newVal) -> {
            Size size = new Size(subImageKernelWidthSpinner.getValue(), subImageKernelHeightSpinner.getValue());
            engine.subImagesLibrary.setKernelSubDivisionDim(size);
        };

        subImageKernelWidthSpinner.valueProperty().addListener(listener);
        subImageKernelHeightSpinner.valueProperty().addListener(listener);
    }

    /**
     * Initializes the spinner that controls mosaic subdivision width.
     * Also ensures the height is adjusted to maintain the image aspect ratio.
     */
    private void initMosaicSubDivisionWidthSpinner() {
        mosaicSubDivisionWidthSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10000, 64, 1)
        );

        mosaicSubDivisionWidthSpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (isUpdatingSpinners) return;

            double aspectRatio = imageView.getImage().getWidth() / imageView.getImage().getHeight();
            int height = (int) (newVal / aspectRatio);

            isUpdatingSpinners = true;
            mosaicSubDivisionHeightSpinner.getValueFactory().setValue(height);
            isUpdatingSpinners = false;

            engine.setSubDivisionCount(new Size(newVal, height));
        });
    }

    /**
     * Initializes the spinner that controls mosaic subdivision height.
     */
    private void initMosaicSubDivisionHeightSpinner() {
        mosaicSubDivisionHeightSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10000, 36, 1)
        );

        mosaicSubDivisionHeightSpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (isUpdatingSpinners) return;

            double aspectRatio = imageView.getImage().getWidth() / imageView.getImage().getHeight();
            int width = (int) (newVal * aspectRatio);

            isUpdatingSpinners = true;
            mosaicSubDivisionWidthSpinner.getValueFactory().setValue(width);
            isUpdatingSpinners = false;

            engine.setSubDivisionCount(new Size(width, newVal));
        });
    }

    /**
     * Initializes both width and height mosaic subdivision spinners.
     */
    private void initMosaicSubDivisionCountSpinners() {
        initMosaicSubDivisionHeightSpinner();
        initMosaicSubDivisionWidthSpinner();
    }

    /**
     * Initializes the spinner for output resolution width and links it to height spinner using aspect ratio.
     */
    private void initOutputResolutionWidthSpinner() {
        outputResolutionWidthSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10000, 1920, 1)
        );

        outputResolutionWidthSpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (isUpdatingSpinners || selectedImage == null) return;

            double aspectRatio = imageView.getImage().getWidth() / imageView.getImage().getHeight();
            int height = (int) (newVal / aspectRatio);

            isUpdatingSpinners = true;
            outputResolutionHeightSpinner.getValueFactory().setValue(height);
            isUpdatingSpinners = false;
        });
    }

    /**
     * Initializes the spinner for output resolution height and links it to width spinner using aspect ratio.
     */
    private void initOutputResolutionHeightSpinner() {
        outputResolutionHeightSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10000, 1080, 1)
        );

        outputResolutionHeightSpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (isUpdatingSpinners || selectedImage == null) return;

            double aspectRatio = imageView.getImage().getWidth() / imageView.getImage().getHeight();
            int width = (int) (newVal * aspectRatio);

            isUpdatingSpinners = true;
            outputResolutionWidthSpinner.getValueFactory().setValue(width);
            isUpdatingSpinners = false;
        });
    }

    /**
     * Initializes both width and height output resolution spinners.
     */
    private void initOutputResolutionSpinners() {
        initOutputResolutionHeightSpinner();
        initOutputResolutionWidthSpinner();
    }

    // ---------- FXML Event Handlers ----------

    /**
     * Handles the "Generate Mosaic" button click event.
     * Starts the Engine's photomosaic generation.
     * @throws IOException if processing fails
     */
    @FXML
    protected void onGenerateMosaicButton() throws IOException {
        progressStatusLabel.setText("Generating photomosaic...");
        engine.start();
    }

    /**
     * Handles the "Resize Output" checkbox toggle.
     * Enables or disables resolution input controls.
     */
    @FXML
    protected void onResizeOutputCheckBox() {
        outputResolutionHBox.setDisable(!resizeOutputCheckBox.isSelected());
    }

    /**
     * Handles the "Set Library Directory" button click event.
     * Opens a directory chooser and sets the sub-image library.
     */
    @FXML
    protected void onSetLibraryDirectoryButton() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        File chosenDirectory = directoryChooser.showDialog(setLibraryDirectoryButton.getScene().getWindow());

        if (chosenDirectory == null) {
            return;
        }

        Path selectedDirectory = chosenDirectory.toPath();

        if (!selectedDirectory.toFile().exists()) {
            showError("Selected directory is empty or does not exist.", "Please select a valid directory.");
            return;
        }

        subImagesDirectoryLabel.setText(selectedDirectory.toString());
        subImagesLibraryFileCountLabel.setText(selectedDirectory.toFile().list().length + "");

        if (Files.isDirectory(selectedDirectory)) {
            boolean state = engine.subImagesLibrary.setLibraryDirectory(selectedDirectory);
            engine.subImagesLibrary.update();
        }

        SubImageKernelDimentionsGrid.setDisable(false);
    }

    /**
     * Handles the "Set Image" button click event.
     * Opens an image file dialog and loads the image into the view.
     * @throws IOException if reading the file fails
     */
    @FXML
    protected void onSetImageButton() throws IOException {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.bmp")
        );

        Window window = setImageButton.getScene().getWindow();
        File chosenImage = fileChooser.showOpenDialog(window);

        if (chosenImage == null) return;

        selectedImage = chosenImage;

        engine.setSelecetedImage(selectedImage);
        Image image = new Image(selectedImage.toURI().toString());

        // Rotate portrait images
        Mat OpenCVImage = Imgcodecs.imread(selectedImage.toString());
        if (OpenCVImage.height() > OpenCVImage.width()) {
            image = rotateImageClockwise(image, 90);
        }

        imageView.setImage(image);
        outputResolutionWidthSpinner.getValueFactory().setValue((int) image.getWidth());
        outputResolutionHeightSpinner.getValueFactory().setValue((int) image.getHeight());
        selectedImageDirectoryLabel.setText(selectedImage.toString());

        MosaicSubDivisionCountGrid.setDisable(false);

        mosaicSubDivisionWidthSpinner.getValueFactory().setValue((int) (image.getWidth() / 30));
        mosaicSubDivisionHeightSpinner.getValueFactory().setValue((int) (image.getHeight() / 30));
    }

    /**
     * Rotates a JavaFX Image clockwise by a given angle.
     * @param image the image to rotate
     * @param angle rotation angle in degrees
     * @return rotated JavaFX Image
     */
    private Image rotateImageClockwise(Image image, double angle) {
        BufferedImage bufferedImage = SwingFXUtils.fromFXImage(image, null);
        double radians = Math.toRadians(angle);
        double sin = Math.abs(Math.sin(radians));
        double cos = Math.abs(Math.cos(radians));
        int w = bufferedImage.getWidth();
        int h = bufferedImage.getHeight();
        int newW = (int) Math.floor(w * cos + h * sin);
        int newH = (int) Math.floor(h * cos + w * sin);

        BufferedImage rotatedImage = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = rotatedImage.createGraphics();

        // Center rotation
        AffineTransform at = new AffineTransform();
        at.translate(newW / 2.0, newH / 2.0);
        at.rotate(radians);
        at.translate(-w / 2.0, -h / 2.0);
        g2d.setTransform(at);
        g2d.drawImage(bufferedImage, 0, 0, null);
        g2d.dispose();

        return SwingFXUtils.toFXImage(rotatedImage, new WritableImage(newW, newH));
    }
}
